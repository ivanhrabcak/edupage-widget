package eu.hrabcak.edupagewidget.helper

import android.content.Context

data class EdupageCredentials(
    val username: String,
    val password: String,
    val subdomain: String
)

object PreferencesHelper {
    private const val preferencesName = "EdupageCredentials"

    fun getString(context: Context, key: String, defaultValue: String?): String {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString(key, defaultValue)!!
    }

    fun putString(context: Context, key: String, value: String) {
        context.getSharedPreferences(preferencesName,  Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    fun getCredentials(context: Context): EdupageCredentials? {
        val username = PreferencesHelper.getString(context, "username", "no_username")
        val password = PreferencesHelper.getString(context, "password", "no_password")
        val subdomain = PreferencesHelper.getString(context, "subdomain", "no_subdomain")
        if (username == "no_username" || password == "no_password" || subdomain == "no_subdomain") {
            return null
        }

        return EdupageCredentials(
            username, password, subdomain
        )
    }
}