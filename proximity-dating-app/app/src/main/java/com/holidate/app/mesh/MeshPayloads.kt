package com.holidate.app.mesh

import com.holidate.app.data.db.ProfileEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON codecs for the app-level payloads carried inside a [MeshMessage].
 *
 * Broadcast beacons travel as plaintext (they are meant to be public); directed payloads
 * (LIKE / MATCH / CHAT / ACK) are encrypted by [MessageRouter] before they hit the wire, so
 * these codecs only ever see plaintext.
 */
object MeshPayloads {

    fun encodeBeacon(self: ProfileEntity): String = JSONObject().apply {
        put("displayName", self.displayName)
        put("age", self.age)
        put("bio", self.bio)
        put("interests", JSONArray(self.interests))
        put("photo", self.photo ?: JSONObject.NULL)
        put("signingKey", self.signingKey)
        put("encryptionKey", self.encryptionKey)
    }.toString()

    /** Decode a beacon into a profile row. [nodeId] and [signingKey] come from the envelope. */
    fun decodeBeacon(nodeId: String, signingKey: String, json: String, now: Long): ProfileEntity {
        val obj = JSONObject(json)
        val interestsJson = obj.optJSONArray("interests") ?: JSONArray()
        return ProfileEntity(
            nodeId = nodeId,
            displayName = obj.optString("displayName", "Someone"),
            age = obj.optInt("age", 0),
            bio = obj.optString("bio", ""),
            interests = List(interestsJson.length()) { interestsJson.getString(it) },
            photo = obj.opt("photo")?.takeIf { it != JSONObject.NULL }?.toString(),
            signingKey = signingKey,
            encryptionKey = obj.optString("encryptionKey", ""),
            lastSeen = now,
            isSelf = false,
        )
    }

    fun encodeChat(messageId: String, text: String): ByteArray = JSONObject().apply {
        put("id", messageId)
        put("text", text)
    }.toString().toByteArray(Charsets.UTF_8)

    data class ChatPayload(val id: String, val text: String)

    fun decodeChat(bytes: ByteArray): ChatPayload {
        val obj = JSONObject(String(bytes, Charsets.UTF_8))
        return ChatPayload(obj.getString("id"), obj.getString("text"))
    }

    /** LIKE / MATCH payloads are empty markers; the meaning is entirely in the message type. */
    val EMPTY: ByteArray = "{}".toByteArray(Charsets.UTF_8)

    fun encodeAck(refId: String): ByteArray =
        JSONObject().put("refId", refId).toString().toByteArray(Charsets.UTF_8)

    fun decodeAckRef(bytes: ByteArray): String =
        JSONObject(String(bytes, Charsets.UTF_8)).getString("refId")
}
