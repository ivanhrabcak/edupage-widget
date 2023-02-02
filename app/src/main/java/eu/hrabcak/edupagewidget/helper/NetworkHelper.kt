package eu.hrabcak.edupagewidget.helper

import eu.hrabcak.edupagewidget.edupage.Task
import khttp.get
import khttp.post
import khttp.responses.Response
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

    fun doGET(url: String, result: LinkedBlockingQueue<Response>): Task = Task(thread(false) {
        result.add(get(url))
    })

    fun doPOST(
        url: String,
        data: Map<String, String>,
        headers: Map<String, String>,
        cookies: Map<String, String>,
        result: LinkedBlockingQueue<Response>
    ): Task = Task(thread(false) {
        result.add(post(url, headers, data = data, cookies = cookies))
    })
}