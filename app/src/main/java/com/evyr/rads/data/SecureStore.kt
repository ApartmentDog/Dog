package com.evyr.rads.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException

/**
 * API keys live in EncryptedSharedPreferences, never in source or Room.
 *
 * create() throws when the Keystore-backed master key and this file's
 * encrypted contents fall out of sync -- most often after a reinstall, an
 * app backup/restore, or the device's Keystore itself getting corrupted.
 * Every read/write used to swallow that with runCatching, so a key that
 * failed to save (or failed to read back) looked identical to an empty
 * field: no error, no signal, just gone. If a write throws now, it deletes
 * the broken file and retries once on a fresh one -- if that still fails,
 * the exception surfaces so the UI can actually say saving failed, instead
 * of quietly discarding what was typed.
 */
object SecureStore {

    private const val FILE = "rads_secure"
    private const val KEY_GEMINI = "gemini_api_key"
    private const val KEY_MODEL = "gemini_model"
    private const val KEY_USDA = "usda_api_key"

    const val DEFAULT_MODEL = "gemini-3.6-flash"

    /**
     * Model names get retired. Anything here is silently upgraded to the
     * current default so a saved-but-dead name doesn't break scanning.
     */
    private val RETIRED_MODELS = setOf(
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-2.0-flash",
        "gemini-2.0-flash-exp",
        "gemini-2.5-flash"
    )

    private fun masterKey(context: Context) =
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun open(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    /**
     * Deletes the encrypted file and its Keystore entry, then reopens fresh.
     * Whatever was in it is unrecoverable -- the point is turning a
     * permanently broken store back into a working, empty one, since a
     * corrupted store otherwise fails forever.
     */
    private fun openAfterWipe(context: Context): SharedPreferences {
        val app = context.applicationContext
        app.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().commit()
        app.deleteSharedPreferences(FILE)
        return open(app)
    }

    private fun prefs(context: Context): SharedPreferences = open(context)

    /**
     * A read that hit a broken store returns null (caller decides the
     * fallback) rather than silently reporting "no value saved."
     * KeyPermanentlyInvalidatedException is a GeneralSecurityException
     * subtype, so one catch covers both.
     */
    private fun readSafely(context: Context, block: (SharedPreferences) -> String): String? =
        try {
            block(prefs(context))
        } catch (e: GeneralSecurityException) {
            Log.e("SecureStore", "read failed, store looks corrupted", e)
            null
        }

    /**
     * A write that hits a broken store wipes it and retries once. Throws if
     * that still fails, so the caller can tell the person saving failed
     * instead of pretending it worked.
     */
    private fun writeSafely(context: Context, block: (SharedPreferences) -> Unit) {
        try {
            block(prefs(context))
        } catch (e: GeneralSecurityException) {
            Log.e("SecureStore", "write failed, wiping and retrying", e)
            block(openAfterWipe(context))
        }
    }

    fun geminiKey(context: Context): String =
        readSafely(context) { it.getString(KEY_GEMINI, "") ?: "" } ?: ""

    fun setGeminiKey(context: Context, value: String) {
        writeSafely(context) { it.edit().putString(KEY_GEMINI, value.trim()).apply() }
    }

    fun geminiModel(context: Context): String {
        val stored = readSafely(context) { it.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL }
            ?: DEFAULT_MODEL
        if (stored.isBlank() || stored in RETIRED_MODELS) {
            setGeminiModel(context, DEFAULT_MODEL)
            return DEFAULT_MODEL
        }
        return stored
    }

    fun setGeminiModel(context: Context, value: String) {
        val v = value.trim().ifBlank { DEFAULT_MODEL }
        writeSafely(context) { it.edit().putString(KEY_MODEL, v).apply() }
    }

    fun hasGeminiKey(context: Context): Boolean = geminiKey(context).isNotBlank()

    /** Blank means fall back to USDA's shared DEMO_KEY (rate limited). */
    fun usdaKey(context: Context): String =
        readSafely(context) { it.getString(KEY_USDA, "") ?: "" } ?: ""

    fun setUsdaKey(context: Context, value: String) {
        writeSafely(context) { it.edit().putString(KEY_USDA, value.trim()).apply() }
    }
}

