package com.petmed.app.data.repository

import android.content.Context

object OwnerStore {

    private const val PREFS_NAME = "owner_prefs"
    private const val KEY_NAME = "owner_name"

    fun getName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "") ?: ""

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name)
            .apply()
    }
}
