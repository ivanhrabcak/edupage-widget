package eu.hrabcak.edupagewidget

import android.content.Context
import eu.hrabcak.edupagewidget.edupage.Edupage
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object EdupageCache {
    fun get(context: Context, date: String): Edupage? {
        val cachedDataDate = PreferencesHelper.getString(context, "cached_data_date", "no_cached_data")
        if (cachedDataDate != date) {
            return null
        }

        val cachedData = PreferencesHelper.getString(context, "cached_data", "no_cached_data")
        if (cachedData == "no_cached_data") {
            return null
        }

        val edupageData = JSONObject(cachedData)
        val edupage = Edupage(context)
        edupage.data = edupageData
        edupage.isLoggedIn = true

        return edupage
    }

    fun put(context: Context, date: String, edupage: Edupage) {
        PreferencesHelper.putString(context, "cached_data_date", date)
        PreferencesHelper.putString(context, "cached_data", edupage.data.toString())
    }
}