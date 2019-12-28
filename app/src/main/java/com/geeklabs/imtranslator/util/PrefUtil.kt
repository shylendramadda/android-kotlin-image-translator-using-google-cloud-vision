package com.geeklabs.imtranslator.util

import android.content.Context
import android.content.SharedPreferences

class PrefUtil(context: Context) {
    private val PREFS_FILENAME = "com.geekalabs.imtranslator.prefs"
    private val LANGAUAGES: String = "languages"
    private val SELECTED_LANGAUAGE: String = "selected_language"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    var lanagues: String
        get() = prefs.getString(LANGAUAGES, "") ?: ""
        set(value) = prefs.edit().putString(LANGAUAGES, value).apply()

    var selectedLanguageCode: String
        get() = prefs.getString(SELECTED_LANGAUAGE, "") ?: ""
        set(value) = prefs.edit().putString(SELECTED_LANGAUAGE, value).apply()

}