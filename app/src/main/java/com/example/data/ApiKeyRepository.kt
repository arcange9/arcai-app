package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiProvider
import com.example.security.ApiKeyCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.apiKeysDataStore: DataStore<Preferences> by preferencesDataStore(name = "arcai_secure_keys")

enum class KeyStatus {
    VERIFIED,
    INVALID,
    UNTESTED
}

data class StoredKeyInfo(
    val providerId: String,
    val apiKey: String,
    val status: KeyStatus,
    val lastVerifiedTime: Long,
    val selectedModel: String
)

class ApiKeyRepository(private val context: Context) {

    companion object {
        private val DEFAULT_PROVIDER_KEY = stringPreferencesKey("default_provider_id")
        private const val SELECTED_MODEL_PREFIX = "selected_model_"
        private const val STATUS_PREFIX = "status_"
        private const val LAST_VERIFIED_PREFIX = "last_verified_"
    }

    private fun keyForProvider(providerId: String) = stringPreferencesKey("key_$providerId")
    private fun modelKeyForProvider(providerId: String) = stringPreferencesKey("$SELECTED_MODEL_PREFIX$providerId")
    private fun statusKeyForProvider(providerId: String) = stringPreferencesKey("$STATUS_PREFIX$providerId")
    private fun verifiedTimeKeyForProvider(providerId: String) = longPreferencesKey("$LAST_VERIFIED_PREFIX$providerId")

    private fun decodeStoredKey(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return ApiKeyCipher.decrypt(value)
    }

    val defaultProviderFlow: Flow<AiProvider> = context.apiKeysDataStore.data.map { prefs ->
        val providerId = prefs[DEFAULT_PROVIDER_KEY] ?: AiProvider.OPENAI.id
        AiProvider.fromId(providerId)
    }

    fun getKeyFlow(providerId: String): Flow<String?> = context.apiKeysDataStore.data.map { prefs ->
        prefs[keyForProvider(providerId)]?.let(::decodeStoredKey)?.ifBlank { null }
    }

    fun getKeyInfoFlow(provider: AiProvider): Flow<StoredKeyInfo> = context.apiKeysDataStore.data.map { prefs ->
        val key = decodeStoredKey(prefs[keyForProvider(provider.id)])
        val model = prefs[modelKeyForProvider(provider.id)] ?: provider.defaultModel
        val statusStr = prefs[statusKeyForProvider(provider.id)] ?: KeyStatus.UNTESTED.name
        val time = prefs[verifiedTimeKeyForProvider(provider.id)] ?: 0L
        StoredKeyInfo(
            providerId = provider.id,
            apiKey = key,
            status = runCatching { KeyStatus.valueOf(statusStr) }.getOrDefault(KeyStatus.UNTESTED),
            lastVerifiedTime = time,
            selectedModel = model
        )
    }

    suspend fun saveApiKey(
        provider: AiProvider,
        apiKey: String,
        status: KeyStatus = KeyStatus.UNTESTED,
        lastVerifiedTime: Long = System.currentTimeMillis()
    ) {
        context.apiKeysDataStore.edit { prefs ->
            if (apiKey.isBlank()) {
                prefs.remove(keyForProvider(provider.id))
                prefs.remove(statusKeyForProvider(provider.id))
                prefs.remove(verifiedTimeKeyForProvider(provider.id))
            } else {
                prefs[keyForProvider(provider.id)] = ApiKeyCipher.encrypt(apiKey.trim())
                prefs[statusKeyForProvider(provider.id)] = status.name
                prefs[verifiedTimeKeyForProvider(provider.id)] = lastVerifiedTime
            }
        }
    }

    /**
     * One-time migration for keys written by pre-v0.2 builds.
     * Plaintext values are converted to Android Keystore-backed ciphertext and
     * are never exported by the repository's backup/export API.
     */
    suspend fun migrateLegacyPlaintextKeys() {
        context.apiKeysDataStore.edit { prefs ->
            for (provider in AiProvider.entries) {
                val preference = keyForProvider(provider.id)
                val stored = prefs[preference]
                if (!stored.isNullOrBlank() && !stored.startsWith("enc:v1:")) {
                    prefs[preference] = ApiKeyCipher.encrypt(stored.trim())
                }
            }
        }
    }

    suspend fun saveSelectedModel(provider: AiProvider, modelId: String) {
        context.apiKeysDataStore.edit { prefs ->
            prefs[modelKeyForProvider(provider.id)] = modelId
        }
    }

    suspend fun setDefaultProvider(provider: AiProvider) {
        context.apiKeysDataStore.edit { prefs ->
            prefs[DEFAULT_PROVIDER_KEY] = provider.id
        }
    }

    suspend fun removeApiKey(provider: AiProvider) {
        context.apiKeysDataStore.edit { prefs ->
            prefs.remove(keyForProvider(provider.id))
            prefs.remove(statusKeyForProvider(provider.id))
            prefs.remove(verifiedTimeKeyForProvider(provider.id))
        }
    }

    /**
     * Exports provider metadata without exporting plaintext API keys.
     * This intentionally cannot recreate credentials on another device.
     */
    suspend fun exportKeysAsJson(): String {
        val prefs = context.apiKeysDataStore.data.first()
        val jsonArray = JSONArray()
        for (provider in AiProvider.entries) {
            val key = decodeStoredKey(prefs[keyForProvider(provider.id)])
            if (key.isNotBlank()) {
                jsonArray.put(JSONObject().apply {
                    put("providerId", provider.id)
                    put("hasApiKey", true)
                    put("selectedModel", prefs[modelKeyForProvider(provider.id)] ?: provider.defaultModel)
                    put("status", prefs[statusKeyForProvider(provider.id)] ?: KeyStatus.UNTESTED.name)
                })
            }
        }
        return jsonArray.toString(2)
    }

    /**
     * Imports metadata and the legacy plaintext-key format. Imported credentials
     * are encrypted immediately with Android Keystore and are never logged.
     */
    suspend fun importKeysFromJson(jsonString: String): Int {
        return runCatching {
            val jsonArray = JSONArray(jsonString)
            var importedCount = 0
            context.apiKeysDataStore.edit { prefs ->
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val providerId = obj.optString("providerId").trim()
                    if (providerId.isEmpty() || AiProvider.fromId(providerId) == null) continue

                    val legacyApiKey = obj.optString("apiKey").trim()
                    if (legacyApiKey.isNotEmpty()) {
                        prefs[keyForProvider(providerId)] = ApiKeyCipher.encrypt(legacyApiKey)
                    } else if (!obj.optBoolean("hasApiKey", false)) {
                        continue
                    }

                    val status = runCatching {
                        KeyStatus.valueOf(obj.optString("status", KeyStatus.UNTESTED.name))
                    }.getOrDefault(KeyStatus.UNTESTED)
                    prefs[statusKeyForProvider(providerId)] = status.name

                    val model = obj.optString("selectedModel").trim()
                    if (model.isNotEmpty()) prefs[modelKeyForProvider(providerId)] = model
                    importedCount++
                }
            }
            importedCount
        }.getOrDefault(0)
    }
}
