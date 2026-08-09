package com.holidate.app.mesh

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** The kind of payload an envelope carries through the mesh. */
enum class MeshMessageType {
    /** Public, broadcast profile advertisement. Plaintext payload, floods the whole mesh. */
    PROFILE_BEACON,

    /** Directed "I like you" signal, encrypted to the recipient. */
    LIKE,

    /** Directed confirmation that two people liked each other, encrypted to the recipient. */
    MATCH,

    /** Directed chat message, encrypted to the recipient. */
    CHAT,

    /** Directed delivery receipt, encrypted to the recipient. */
    ACK,
}

/**
 * A self-contained, signed envelope that hops from phone to phone.
 *
 * Fields split into two groups:
 *  - **Immutable, signed** ([id], [type], [senderId], [senderSigningKey], [recipientId],
 *    [timestamp], [payload]) — these are covered by [signature] and never change in transit,
 *    so any relay or recipient can authenticate the origin.
 *  - **Mutable, unsigned** ([ttl], [path]) — these are rewritten at every hop for routing and
 *    loop prevention, and are deliberately excluded from the signature.
 *
 * A `null`/blank [recipientId] means broadcast (e.g. profile beacons); everything else is
 * addressed to a single [recipientId] and its [payload] is end-to-end encrypted to that node.
 */
data class MeshMessage(
    val id: String,
    val type: MeshMessageType,
    val senderId: String,
    val senderSigningKey: String,
    val recipientId: String?,
    val timestamp: Long,
    val payload: ByteArray,
    val signature: ByteArray,
    val ttl: Int,
    val path: List<String>,
) {
    val isBroadcast: Boolean get() = recipientId.isNullOrBlank()

    /** Canonical byte representation of the signed fields, used for signing and verification. */
    fun signedBytes(): ByteArray = buildString {
        append(id).append('|')
        append(type.name).append('|')
        append(senderId).append('|')
        append(senderSigningKey).append('|')
        append(recipientId ?: "").append('|')
        append(timestamp).append('|')
        append(Base64.encodeToString(payload, Base64.NO_WRAP))
    }.toByteArray(Charsets.UTF_8)

    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("senderId", senderId)
        put("senderSigningKey", senderSigningKey)
        put("recipientId", recipientId ?: JSONObject.NULL)
        put("timestamp", timestamp)
        put("payload", Base64.encodeToString(payload, Base64.NO_WRAP))
        put("signature", Base64.encodeToString(signature, Base64.NO_WRAP))
        put("ttl", ttl)
        put("path", JSONArray(path))
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        const val DEFAULT_TTL = 8

        fun fromJsonBytes(bytes: ByteArray): MeshMessage {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            val pathJson = json.getJSONArray("path")
            return MeshMessage(
                id = json.getString("id"),
                type = MeshMessageType.valueOf(json.getString("type")),
                senderId = json.getString("senderId"),
                senderSigningKey = json.getString("senderSigningKey"),
                recipientId = json.opt("recipientId")
                    ?.takeIf { it != JSONObject.NULL }?.toString(),
                timestamp = json.getLong("timestamp"),
                payload = Base64.decode(json.getString("payload"), Base64.NO_WRAP),
                signature = Base64.decode(json.getString("signature"), Base64.NO_WRAP),
                ttl = json.getInt("ttl"),
                path = List(pathJson.length()) { pathJson.getString(it) },
            )
        }

        fun newId(): String = UUID.randomUUID().toString()
    }

    // data class equals/hashCode need explicit handling for the ByteArray members.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshMessage) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
