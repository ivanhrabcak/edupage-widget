package eu.hrabcak.edupagewidget.helper

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

object NetworkHelper {
    fun isInternetAvailable(): Boolean {
        val result = LinkedBlockingQueue<Boolean>()

        thread {
            result.add(try {
                val url = URL("https://www.google.com/")
                val urlc = url.openConnection() as HttpURLConnection
                urlc.setRequestProperty("User-Agent", "test")
                urlc.setRequestProperty("Connection", "close")
                urlc.connectTimeout = 1000 // mTimeout is in seconds
                urlc.connect()

                urlc.responseCode == 200
            } catch (e: IOException) {
                false
            })
        }

        return result.take()
    }
}