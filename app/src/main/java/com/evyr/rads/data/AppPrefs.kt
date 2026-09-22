package com.evyr.rads.data

import android.content.Context

/** Non-sensitive UI preferences. Keys here never hold personal data. */
object AppPrefs {

    private const val FILE = "rads_prefs"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

