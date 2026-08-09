package com.holidate.app.data.repository

import android.util.Log
import com.holidate.app.crypto.KeyManager
import com.holidate.app.data.db.ChatMessageEntity
import com.holidate.app.data.db.HoliDateDatabase
import com.holidate.app.data.db.IncomingLikeEntity
import com.holidate.app.data.db.MatchEntity
import com.holidate.app.data.db.ProfileEntity
import com.holidate.app.data.db.SwipeEntity
import com.holidate.app.mesh.MeshMessageType
import com.holidate.app.mesh.MeshPayloads
import com.holidate.app.mesh.MessageRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single source of truth for the app. Bridges the encrypted mesh ([MessageRouter.Events]) and
 * local storage (Room), and hosts the like → mutual-like → match → chat state machine.
 */
class HoliDateRepository(
    private val db: HoliDateDatabase,
    private val keys: KeyManager,
    private val now: () -> Long = { System.currentTimeMillis() },
) : MessageRouter.Events {

    /** Wired up by the app container after the router is constructed (breaks the cycle). */
    lateinit var router: MessageRouter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ----- Flows for the UI --------------------------------------------------------------

    val selfProfile: Flow<ProfileEntity?> = db.profileDao().observeSelf()
    val discoverable: Flow<List<ProfileEntity>> = db.profileDao().observeDiscoverable()
    val matches: Flow<List<ProfileEntity>> = db.matchDao().observeMatchedProfiles()

    fun conversation(peerId: String): Flow<List<ChatMessageEntity>> =
        db.chatDao().observeConversation(peerId)

    // ----- Own profile -------------------------------------------------------------------

    suspend fun getSelf(): ProfileEntity? = db.profileDao().getSelf()

    /** Persist our profile, stamping it with our own public keys and node id. */
    suspend fun saveOwnProfile(displayName: String, age: Int, bio: String, interests: List<String>, photo: String?) {
        db.profileDao().upsert(
            ProfileEntity(
                nodeId = keys.nodeId,
                displayName = displayName,
                age = age,
                bio = bio,
                interests = interests,
                photo = photo,
                signingKey = keys.signingPublicKey,
                encryptionKey = keys.encryptionPublicKey,
                lastSeen = now(),
                isSelf = true,
            ),
        )
    }

    /** Advertise our profile into the mesh. Called periodically by the mesh service. */
    suspend fun broadcastOwnBeacon() {
        val self = db.profileDao().getSelf() ?: return
        router.broadcastBeacon(MeshPayloads.encodeBeacon(self), now())
    }

    // ----- Swiping / matching ------------------------------------------------------------

    suspend fun swipe(nodeId: String, liked: Boolean) {
        db.swipeDao().upsert(SwipeEntity(nodeId, liked, now()))
        if (!liked) return

        val profile = db.profileDao().get(nodeId)
        if (db.incomingLikeDao().exists(nodeId) != null) {
            // They already liked us → it's a match. Tell them, and record it locally.
            db.incomingLikeDao().delete(nodeId)
            createMatch(nodeId)
            profile?.let { sendDirect(MeshMessageType.MATCH, it, MeshPayloads.EMPTY) }
        } else {
            profile?.let { sendDirect(MeshMessageType.LIKE, it, MeshPayloads.EMPTY) }
        }
    }

    // ----- Chat --------------------------------------------------------------------------

    suspend fun sendChat(peerId: String, text: String) {
        val peer = db.profileDao().get(peerId) ?: return
        val messageId = UUID.randomUUID().toString()
        db.chatDao().upsert(
            ChatMessageEntity(
                id = messageId,
                peerId = peerId,
                text = text,
                outgoing = true,
                timestamp = now(),
                delivered = false,
            ),
        )
        sendDirect(MeshMessageType.CHAT, peer, MeshPayloads.encodeChat(messageId, text))
    }

    // ----- MessageRouter.Events (inbound from the mesh) ----------------------------------

    override fun onProfileBeacon(senderId: String, senderSigningKey: String, payloadJson: String) {
        if (!identityMatches(senderId, senderSigningKey)) return
        scope.launch {
            val existing = db.profileDao().get(senderId)
            val incoming = MeshPayloads.decodeBeacon(senderId, senderSigningKey, payloadJson, now())
            // Preserve a previously matched/self flag if we already knew this person.
            db.profileDao().upsert(incoming.copy(isSelf = existing?.isSelf ?: false))
        }
    }

    override fun onDirectMessage(
        type: MeshMessageType,
        senderId: String,
        senderSigningKey: String,
        plaintext: ByteArray,
    ) {
        if (!identityMatches(senderId, senderSigningKey)) return
        scope.launch {
            when (type) {
                MeshMessageType.LIKE -> handleIncomingLike(senderId)
                MeshMessageType.MATCH -> createMatch(senderId)
                MeshMessageType.CHAT -> handleIncomingChat(senderId, plaintext)
                MeshMessageType.ACK -> db.chatDao().markDelivered(MeshPayloads.decodeAckRef(plaintext))
                MeshMessageType.PROFILE_BEACON -> Unit // beacons never arrive as directed messages
            }
        }
    }

    private suspend fun handleIncomingLike(senderId: String) {
        if (db.swipeDao().likedState(senderId) == true) {
            // We already liked them → mutual match. Confirm back to them.
            createMatch(senderId)
            db.profileDao().get(senderId)?.let { sendDirect(MeshMessageType.MATCH, it, MeshPayloads.EMPTY) }
        } else {
            db.incomingLikeDao().upsert(IncomingLikeEntity(senderId, now()))
        }
    }

    private suspend fun handleIncomingChat(senderId: String, plaintext: ByteArray) {
        // Only accept chat from people we have matched with.
        if (db.matchDao().get(senderId) == null) return
        val chat = MeshPayloads.decodeChat(plaintext)
        db.chatDao().upsert(
            ChatMessageEntity(
                id = chat.id,
                peerId = senderId,
                text = chat.text,
                outgoing = false,
                timestamp = now(),
                delivered = true,
            ),
        )
        // Send a delivery receipt back through the mesh.
        db.profileDao().get(senderId)?.let {
            sendDirect(MeshMessageType.ACK, it, MeshPayloads.encodeAck(chat.id))
        }
    }

    // ----- Helpers -----------------------------------------------------------------------

    private suspend fun createMatch(nodeId: String) {
        db.matchDao().upsert(MatchEntity(nodeId, now()))
        // Ensure a like is recorded on our side so the match is internally consistent.
        if (db.swipeDao().likedState(nodeId) != true) {
            db.swipeDao().upsert(SwipeEntity(nodeId, liked = true, decidedAt = now()))
        }
    }

    private fun sendDirect(type: MeshMessageType, peer: ProfileEntity, payload: ByteArray) {
        if (peer.encryptionKey.isBlank()) {
            Log.d(TAG, "Cannot send ${type.name} to ${peer.nodeId}: no encryption key yet")
            return
        }
        router.sendDirect(type, peer.nodeId, peer.encryptionKey, payload, now())
    }

    /** Guard against a peer claiming a node id that does not derive from their signing key. */
    private fun identityMatches(nodeId: String, signingKey: String): Boolean =
        KeyManager.deriveNodeId(signingKey) == nodeId

    companion object {
        private const val TAG = "HoliDateRepository"
        const val BEACON_INTERVAL_MS = 15_000L
    }
}
