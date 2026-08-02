package com.holidate.app.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.SignatureConfig
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Owns this device's long-lived cryptographic identity.
 *
 * Two keysets are generated on first launch and stored encrypted by the Android Keystore:
 *  - a signing keyset (Ed25519) that authenticates every message we originate, and
 *  - a hybrid encryption keyset (HPKE / X25519) that lets other users encrypt private
 *    messages to us that no relaying phone in the mesh can read.
 *
 * The public halves of both keysets travel inside our profile beacon so peers can verify
 * our signatures and encrypt to us. The private halves never leave the device.
 */
class KeyManager private constructor(
    private val signingKeyset: KeysetHandle,
    private val hybridKeyset: KeysetHandle,
) {
    private val signer: PublicKeySign = signingKeyset.getPrimitive(PublicKeySign::class.java)
    private val decryptor: HybridDecrypt =
        hybridKeyset.getPrimitive(HybridDecrypt::class.java)

    /** Serialized public signing keyset (base64). Used by peers to verify our signatures. */
    val signingPublicKey: String = exportPublic(signingKeyset)

    /** Serialized public hybrid keyset (base64). Used by peers to encrypt private messages to us. */
    val encryptionPublicKey: String = exportPublic(hybridKeyset)

    /**
     * Stable, compact identity string derived from the signing public key.
     * This is the address every mesh message is routed to.
     */
    val nodeId: String = deriveNodeId(signingPublicKey)

    fun sign(data: ByteArray): ByteArray = signer.sign(data)

    /** Decrypt a private payload that was encrypted to our public hybrid key. */
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray =
        decryptor.decrypt(ciphertext, associatedData)

    companion object {
        /**
         * Load the identity from encrypted storage, generating it on first launch.
         * The Tink configs are idempotent to register, so this is safe to call repeatedly.
         */
        fun getOrCreate(context: Context): KeyManager {
            SignatureConfig.register()
            HybridConfig.register()

            val signing = AndroidKeysetManager.Builder()
                .withSharedPref(context, "signing_keyset", "holidate_keys")
                .withKeyTemplate(KeyTemplates.get("ED25519"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            val hybrid = AndroidKeysetManager.Builder()
                .withSharedPref(context, "hybrid_keyset", "holidate_keys")
                .withKeyTemplate(
                    KeyTemplates.get("DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM"),
                )
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            return KeyManager(signing, hybrid)
        }

        /** Verify [signature] over [data] using a peer's exported signing public key. */
        fun verify(data: ByteArray, signature: ByteArray, signerPublicKey: String): Boolean =
            try {
                importPublic(signerPublicKey)
                    .getPrimitive(PublicKeyVerify::class.java)
                    .verify(signature, data)
                true
            } catch (_: Exception) {
                false
            }

        /** Encrypt [plaintext] to a peer identified by their exported hybrid public key. */
        fun encryptFor(
            recipientEncryptionKey: String,
            plaintext: ByteArray,
            associatedData: ByteArray = ByteArray(0),
        ): ByteArray =
            importPublic(recipientEncryptionKey)
                .getPrimitive(HybridEncrypt::class.java)
                .encrypt(plaintext, associatedData)

        /** Derive the routable node id from a signing public key (SHA-256, base64url). */
        fun deriveNodeId(signingPublicKey: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(Base64.decode(signingPublicKey, Base64.NO_WRAP))
            return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.URL_SAFE)
                .take(22)
        }

        private const val MASTER_KEY_URI = "android-keystore://holidate_master_key"

        private fun exportPublic(handle: KeysetHandle): String {
            val out = ByteArrayOutputStream()
            CleartextKeysetHandle.write(
                handle.publicKeysetHandle,
                com.google.crypto.tink.BinaryKeysetWriter.withOutputStream(out),
            )
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

        private fun importPublic(exported: String): KeysetHandle =
            CleartextKeysetHandle.read(
                BinaryKeysetReader.withBytes(Base64.decode(exported, Base64.NO_WRAP)),
            )
    }
}
