package com.holidate.app.mesh

import android.util.Log
import com.holidate.app.crypto.KeyManager
import java.util.Collections

/**
 * The routing brain of the mesh. Turns a one-hop [MeshTransport] into a multi-hop,
 * store-and-forward gossip network and enforces authenticity + confidentiality.
 *
 * Inbound handling of every envelope:
 *  1. **Dedup** — drop anything whose id we have already processed (kills flood loops).
 *  2. **Authenticate** — verify the Ed25519 signature; drop forgeries.
 *  3. **Deliver** — if it is a broadcast beacon, or a directed message addressed to us,
 *     hand it to the app (decrypting directed payloads with our private key).
 *  4. **Relay** — if there is TTL left and we are not the final recipient, decrement TTL,
 *     append ourselves to the path, and re-broadcast to our other neighbours.
 *
 * Store-and-forward: recently seen envelopes are cached briefly and replayed to any phone
 * that connects later, so a message can reach someone who was out of range when it was sent —
 * as long as some phone that carries it eventually comes near them.
 */
class MessageRouter(
    private val keys: KeyManager,
    private val transport: MeshTransport,
    private val events: Events,
) : MeshTransport.Listener {

    interface Events {
        /** A public profile advertisement arrived (already-verified plaintext JSON). */
        fun onProfileBeacon(senderId: String, senderSigningKey: String, payloadJson: String)

        /** A directed, decrypted message addressed to us arrived. */
        fun onDirectMessage(
            type: MeshMessageType,
            senderId: String,
            senderSigningKey: String,
            plaintext: ByteArray,
        )
    }

    init {
        transport.setListener(this)
    }

    /** Ids we have already handled, bounded to avoid unbounded growth on a busy mesh. */
    private val seen: MutableSet<String> = Collections.synchronizedSet(
        object : LinkedHashSet<String>() {
            override fun add(element: String): Boolean {
                val added = super.add(element)
                if (size > SEEN_CAPACITY) iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
                return added
            }
        },
    )

    /** Recently relayed envelopes, replayed to newly connected phones (store-and-forward). */
    private val recent = Collections.synchronizedMap(
        object : LinkedHashMap<String, RecentEnvelope>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RecentEnvelope>) =
                size > RECENT_CAPACITY
        },
    )

    private data class RecentEnvelope(val bytes: ByteArray, val storedAt: Long)

    // ----- Origination -------------------------------------------------------------------

    /** Send a public profile beacon that floods the entire mesh. */
    fun broadcastBeacon(payloadJson: String, now: Long) {
        val message = sign(
            type = MeshMessageType.PROFILE_BEACON,
            recipientId = null,
            payload = payloadJson.toByteArray(Charsets.UTF_8),
            now = now,
        )
        remember(message)
        transport.broadcast(message.toJsonBytes())
    }

    /** Send a directed, end-to-end encrypted message toward [recipientId]. */
    fun sendDirect(
        type: MeshMessageType,
        recipientId: String,
        recipientEncryptionKey: String,
        plaintext: ByteArray,
        now: Long,
    ) {
        val ciphertext = KeyManager.encryptFor(recipientEncryptionKey, plaintext)
        val message = sign(type, recipientId, ciphertext, now)
        remember(message)
        // Flood it; the router on the recipient's phone will pick it out by recipientId.
        transport.broadcast(message.toJsonBytes())
    }

    // ----- Transport callbacks -----------------------------------------------------------

    override fun onPayloadReceived(fromEndpointId: String, bytes: ByteArray) {
        val message = try {
            MeshMessage.fromJsonBytes(bytes)
        } catch (e: Exception) {
            Log.d(TAG, "Dropping malformed envelope", e)
            return
        }
        handle(message, fromEndpointId)
    }

    override fun onPeerConnected(endpointId: String) {
        // Replay still-fresh envelopes to the newcomer so they receive recent backlog.
        val cutoff = oldestAcceptableStamp(newestStamp())
        synchronized(recent) {
            recent.values
                .filter { it.storedAt >= cutoff }
                .forEach { transport.sendTo(endpointId, it.bytes) }
        }
    }

    override fun onPeerDisconnected(endpointId: String) = Unit

    // ----- Core handling -----------------------------------------------------------------

    private fun handle(message: MeshMessage, fromEndpointId: String) {
        if (!seen.add(message.id)) return // already processed → stop the flood

        if (!KeyManager.verify(message.signedBytes(), message.signature, message.senderSigningKey)) {
            Log.d(TAG, "Dropping envelope with bad signature from ${message.senderId}")
            return
        }
        if (message.senderId == keys.nodeId) return // our own echo

        deliverLocallyIfNeeded(message)
        relayIfNeeded(message, excludeEndpoint = fromEndpointId)
        remember(message)
    }

    private fun deliverLocallyIfNeeded(message: MeshMessage) {
        when {
            message.isBroadcast && message.type == MeshMessageType.PROFILE_BEACON -> {
                events.onProfileBeacon(
                    senderId = message.senderId,
                    senderSigningKey = message.senderSigningKey,
                    payloadJson = String(message.payload, Charsets.UTF_8),
                )
            }
            message.recipientId == keys.nodeId -> {
                val plaintext = try {
                    keys.decrypt(message.payload)
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to decrypt directed message ${message.id}", e)
                    return
                }
                events.onDirectMessage(
                    type = message.type,
                    senderId = message.senderId,
                    senderSigningKey = message.senderSigningKey,
                    plaintext = plaintext,
                )
            }
        }
    }

    private fun relayIfNeeded(message: MeshMessage, excludeEndpoint: String) {
        // We are the final recipient of a directed message → no need to relay further.
        if (!message.isBroadcast && message.recipientId == keys.nodeId) return
        if (message.ttl <= 1) return
        if (keys.nodeId in message.path) return // we already forwarded this

        val relayed = message.copy(
            ttl = message.ttl - 1,
            path = message.path + keys.nodeId,
        )
        val outBytes = relayed.toJsonBytes()
        // Re-broadcast to all neighbours except the one we just heard it from.
        transport.connectedPeers.value
            .filter { it != excludeEndpoint }
            .forEach { transport.sendTo(it, outBytes) }
    }

    private fun sign(
        type: MeshMessageType,
        recipientId: String?,
        payload: ByteArray,
        now: Long,
    ): MeshMessage {
        val unsigned = MeshMessage(
            id = MeshMessage.newId(),
            type = type,
            senderId = keys.nodeId,
            senderSigningKey = keys.signingPublicKey,
            recipientId = recipientId,
            timestamp = now,
            payload = payload,
            signature = ByteArray(0),
            ttl = MeshMessage.DEFAULT_TTL,
            path = listOf(keys.nodeId),
        )
        return unsigned.copy(signature = keys.sign(unsigned.signedBytes()))
    }

    private fun remember(message: MeshMessage) {
        recent[message.id] = RecentEnvelope(message.toJsonBytes(), message.timestamp)
    }

    private fun newestStamp(): Long =
        synchronized(recent) { recent.values.maxOfOrNull { it.storedAt } ?: 0L }

    private fun oldestAcceptableStamp(newest: Long): Long = newest - RECENT_TTL_MS

    companion object {
        private const val TAG = "MessageRouter"
        private const val SEEN_CAPACITY = 4096
        private const val RECENT_CAPACITY = 512
        private const val RECENT_TTL_MS = 10 * 60 * 1000L // replay backlog from the last 10 min
    }
}
