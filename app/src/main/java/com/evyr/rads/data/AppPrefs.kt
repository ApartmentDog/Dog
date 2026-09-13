package com.evyr.rads.data

import android.content.Context

/** Non-sensitive UI preferences. Keys here never hold personal data. */
object AppPrefs {

    private const val FILE = "rads_prefs"
    private const val KEY_BOOT = "boot_sequence"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun bootSequenceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BOOT, true)

    fun setBootSequenceEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_BOOT, value).apply()
    }
}
