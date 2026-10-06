package com.petmed.app.data.repository

import android.content.Context

object UploadConsentStore {

    private const val PREFS_NAME = "upload_consent"
    private const val KEY_CONSENTED = "consented"

    fun hasConsented(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONSENTED, false)
    }

    fun setConsented(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENTED, value)
            .apply()
    }
}
