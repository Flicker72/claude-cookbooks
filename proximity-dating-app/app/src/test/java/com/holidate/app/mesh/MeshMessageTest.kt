package com.holidate.app.mesh

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [MeshMessage] uses android.util.Base64 and org.json, so it runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class MeshMessageTest {

    private fun directMessage(
        ttl: Int = MeshMessage.DEFAULT_TTL,
        path: List<String> = listOf("origin"),
    ) = MeshMessage(
        id = "msg-1",
        type = MeshMessageType.CHAT,
        senderId = "alice",
        senderSigningKey = "alice-signing-key",
        recipientId = "bob",
        timestamp = 1234567890L,
        payload = byteArrayOf(1, 2, 3, 4),
        signature = byteArrayOf(9, 8, 7),
        ttl = ttl,
        path = path,
    )

    @Test
    fun jsonRoundTripPreservesAllFields() {
        val original = directMessage()
        val restored = MeshMessage.fromJsonBytes(original.toJsonBytes())

        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.senderId, restored.senderId)
        assertEquals(original.senderSigningKey, restored.senderSigningKey)
        assertEquals(original.recipientId, restored.recipientId)
        assertEquals(original.timestamp, restored.timestamp)
        assertArrayEquals(original.payload, restored.payload)
        assertArrayEquals(original.signature, restored.signature)
        assertEquals(original.ttl, restored.ttl)
        assertEquals(original.path, restored.path)
    }

    @Test
    fun broadcastRoundTripKeepsNullRecipient() {
        val beacon = directMessage().copy(recipientId = null, type = MeshMessageType.PROFILE_BEACON)
        assertTrue(beacon.isBroadcast)

        val restored = MeshMessage.fromJsonBytes(beacon.toJsonBytes())
        assertNull(restored.recipientId)
        assertTrue(restored.isBroadcast)
    }

    @Test
    fun signedBytesIgnoreMutableRoutingFields() {
        // ttl and path are rewritten at every hop, so they must not affect the signed bytes.
        val a = directMessage(ttl = 8, path = listOf("origin"))
        val b = directMessage(ttl = 1, path = listOf("origin", "relay-1", "relay-2"))

        assertArrayEquals(a.signedBytes(), b.signedBytes())
    }

    @Test
    fun signedBytesChangeWhenSignedFieldChanges() {
        val a = directMessage()
        val b = directMessage().copy(payload = byteArrayOf(5, 5, 5))

        assertFalse(a.signedBytes().contentEquals(b.signedBytes()))
    }
}
