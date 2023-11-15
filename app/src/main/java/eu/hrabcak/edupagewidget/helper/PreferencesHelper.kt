package eu.hrabcak.edupagewidget.helper

import android.content.Context

object PreferencesHelper {
    private const val preferencesName = "EdupageCredentials"

    fun getString(context: Context, key: String, defaultValue: String?): String {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString(key, defaultValue)!!
    }

    fun putString(context: Context, key: String, value: String) {
        context.getSharedPreferences(preferencesName,  Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}