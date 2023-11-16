package eu.hrabcak.edupagewidget

import android.content.Context
import eu.hrabcak.edupagewidget.edupage.Edupage
import eu.hrabcak.edupagewidget.helper.NetworkHelper
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


// todo: add cache expiration -> 2 hours?
object EdupageCache {
    // 2 hours
    private const val CACHE_EXPIRATION_MILLIS = 1000 * 60 * 60 * 2

    fun get(context: Context, date: String): Edupage? {
        val cachedDataDate = PreferencesHelper.getString(context, "cached_data_date", "no_cached_data")
        if (cachedDataDate != date) {
            return null
        }

        val lastCacheWrite = PreferencesHelper.getString(context, "last_cache_write", "no_data")
        if (lastCacheWrite != "no_data") {
            val now = Date()
            val lastCacheWriteTimestamp = lastCacheWrite.toLong()
            val isNetworkAvailable = NetworkHelper.isInternetAvailable()



            if (lastCacheWriteTimestamp + CACHE_EXPIRATION_MILLIS <= now.time && isNetworkAvailable) {
                println("Cache expiration triggered!")
                return null
            }
        }

        val cachedData = PreferencesHelper.getString(context, "cached_data", "no_cached_data")
        if (cachedData == "no_cached_data" || cachedData == "null") {
            return null
        }

        println("Returning cached edupage!")

        val edupageData = JSONObject(cachedData)
        val edupage = Edupage()
        edupage.data = edupageData
        edupage.isLoggedIn = true

        return edupage
    }

    fun put(context: Context, date: String, edupage: Edupage) {
        val now = Date()

        PreferencesHelper.putString(context, "last_cache_write", now.time.toString())
        PreferencesHelper.putString(context, "cached_data_date", date)
        PreferencesHelper.putString(context, "cached_data", edupage.data.toString())
    }
}