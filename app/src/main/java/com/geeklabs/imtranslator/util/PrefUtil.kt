package com.geeklabs.imtranslator.util

import android.content.Context
import android.content.SharedPreferences

class PrefUtil(context: Context) {
    val PREFS_FILENAME = "com.geekalabs.imtranslator.prefs"
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    private val LANGAUAGES: String = "languages"
    var lanagues: String
        get() = prefs.getString(LANGAUAGES, "")
        set(value) = prefs.edit().putString(LANGAUAGES, value).apply()
}