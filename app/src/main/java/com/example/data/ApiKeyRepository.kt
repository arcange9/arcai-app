package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiProvider
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

    private val secureKeyStore = SecureKeyStore()

    private fun keyForProvider(providerId: String) = stringPreferencesKey("key_$providerId")
    private fun modelKeyForProvider(providerId: String) = stringPreferencesKey("$SELECTED_MODEL_PREFIX$providerId")
    private fun statusKeyForProvider(providerId: String) = stringPreferencesKey("$STATUS_PREFIX$providerId")
    private fun verifiedTimeKeyForProvider(providerId: String) = longPreferencesKey("$LAST_VERIFIED_PREFIX$providerId")

    private fun decryptStoredKey(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return secureKeyStore.decrypt(raw) ?: raw // Backward-compatible read for legacy installs.
    }

    val defaultProviderFlow: Flow<AiProvider> = context.apiKeysDataStore.data.map { prefs ->
        val providerId = prefs[DEFAULT_PROVIDER_KEY] ?: AiProvider.OPENAI.id
        AiProvider.fromId(providerId)
    }

    fun getKeyFlow(providerId: String): Flow<String?> = context.apiKeysDataStore.data.map { prefs ->
        decryptStoredKey(prefs[keyForProvider(providerId)]).ifBlank { null }
    }

    fun getKeyInfoFlow(provider: AiProvider): Flow<StoredKeyInfo> = context.apiKeysDataStore.data.map { prefs ->
        val key = decryptStoredKey(prefs[keyForProvider(provider.id)])
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
                prefs[keyForProvider(provider.id)] = secureKeyStore.encrypt(apiKey.trim())
                prefs[statusKeyForProvider(provider.id)] = status.name
                prefs[verifiedTimeKeyForProvider(provider.id)] = lastVerifiedTime
            }
        }
    }

    /** Re-encrypts keys saved by versions of ArcAI that used plaintext DataStore values. */
    suspend fun migrateLegacyKeys(): Int {
        var migrated = 0
        context.apiKeysDataStore.edit { prefs ->
            for (provider in AiProvider.entries) {
                val key = keyForProvider(provider.id)
                val raw = prefs[key]
                if (!raw.isNullOrBlank() && !secureKeyStore.isEncrypted(raw)) {
                    prefs[key] = secureKeyStore.encrypt(raw)
                    migrated++
                }
            }
        }
        return migrated
    }

    suspend fun saveSelectedModel(provider: AiProvider, modelId: String) {
        context.apiKeysDataStore.edit { prefs ->
            prefs[modelKeyForProvider(provider.id)] = modelId.trim()
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

    suspend fun exportKeysAsJson(): String {
        val prefs = context.apiKeysDataStore.data.first()
        val jsonArray = JSONArray()
        for (provider in AiProvider.entries) {
            val key = decryptStoredKey(prefs[keyForProvider(provider.id)])
            if (key.isNotBlank()) {
                jsonArray.put(JSONObject().apply {
                    put("providerId", provider.id)
                    put("apiKey", key)
                    put("selectedModel", prefs[modelKeyForProvider(provider.id)] ?: provider.defaultModel)
                    put("status", prefs[statusKeyForProvider(provider.id)] ?: KeyStatus.UNTESTED.name)
                })
            }
        }
        return jsonArray.toString(2)
    }

    suspend fun importKeysFromJson(jsonString: String): Int {
        var importedCount = 0
        runCatching {
            val jsonArray = JSONArray(jsonString)
            context.apiKeysDataStore.edit { prefs ->
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val providerId = obj.optString("providerId")
                    val apiKey = obj.optString("apiKey")
                    val provider = AiProvider.fromId(providerId)
                    if (providerId.isNotBlank() && apiKey.isNotBlank() && provider != null) {
                        prefs[keyForProvider(providerId)] = secureKeyStore.encrypt(apiKey.trim())
                        prefs[statusKeyForProvider(providerId)] = obj.optString("status", KeyStatus.UNTESTED.name)
                        obj.optString("selectedModel").takeIf { it.isNotBlank() }?.let {
                            prefs[modelKeyForProvider(providerId)] = it
                        }
                        importedCount++
                    }
                }
            }
        }
        return importedCount
    }
}
