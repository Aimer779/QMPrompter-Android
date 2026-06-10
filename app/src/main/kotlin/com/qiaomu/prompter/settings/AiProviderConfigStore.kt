package com.qiaomu.prompter.settings

import android.content.Context

data class AiProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = ""
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}

class AiProviderConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): AiProviderConfig =
        AiProviderConfig(
            apiKey = preferences.getString(KEY_API_KEY, "").orEmpty(),
            baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty(),
            model = preferences.getString(KEY_MODEL, "").orEmpty()
        )

    fun save(config: AiProviderConfig) {
        preferences.edit()
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_MODEL, config.model)
            .apply()
    }

    fun clearApiKey() {
        preferences.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "ai_provider_config"

        private const val KEY_API_KEY = "apiKey"
        private const val KEY_BASE_URL = "baseUrl"
        private const val KEY_MODEL = "model"
    }
}
