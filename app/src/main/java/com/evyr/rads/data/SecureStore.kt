package com.evyr.rads.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API keys live in EncryptedSharedPreferences, never in source or Room.
 */
object SecureStore {

    private const val FILE = "rads_secure"
    private const val KEY_GEMINI = "gemini_api_key"
    private const val KEY_MODEL = "gemini_model"
    private const val KEY_USDA = "usda_api_key"

    const val DEFAULT_MODEL = "gemini-2.0-flash"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun geminiKey(context: Context): String =
        runCatching { prefs(context).getString(KEY_GEMINI, "") ?: "" }.getOrDefault("")

    fun setGeminiKey(context: Context, value: String) {
        runCatching { prefs(context).edit().putString(KEY_GEMINI, value.trim()).apply() }
    }

    fun geminiModel(context: Context): String =
        runCatching {
            prefs(context).getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        }.getOrDefault(DEFAULT_MODEL)

    fun setGeminiModel(context: Context, value: String) {
        val v = value.trim().ifBlank { DEFAULT_MODEL }
        runCatching { prefs(context).edit().putString(KEY_MODEL, v).apply() }
    }

    fun hasGeminiKey(context: Context): Boolean = geminiKey(context).isNotBlank()

    /** Blank means fall back to USDA's shared DEMO_KEY (rate limited). */
    fun usdaKey(context: Context): String =
        runCatching { prefs(context).getString(KEY_USDA, "") ?: "" }.getOrDefault("")

    fun setUsdaKey(context: Context, value: String) {
        runCatching { prefs(context).edit().putString(KEY_USDA, value.trim()).apply() }
    }
}
