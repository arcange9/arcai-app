package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Android Keystore backed encryption for provider API keys. */
class SecureKeyStore {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "arcai_api_keys_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val VERSION_PREFIX = "v1:"
        private const val IV_LENGTH = 12
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        require(value.isNotBlank()) { "Cannot encrypt an empty value" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val data = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$VERSION_PREFIX$iv:$data"
    }

    fun decrypt(value: String): String? {
        if (!value.startsWith(VERSION_PREFIX)) return null
        return runCatching {
            val payload = value.removePrefix(VERSION_PREFIX)
            val separator = payload.indexOf(':')
            require(separator > 0) { "Invalid encrypted key format" }
            val iv = Base64.decode(payload.substring(0, separator), Base64.NO_WRAP)
            require(iv.size == IV_LENGTH) { "Invalid IV" }
            val ciphertext = Base64.decode(payload.substring(separator + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    fun isEncrypted(value: String?): Boolean = value?.startsWith(VERSION_PREFIX) == true
}
