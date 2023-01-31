package eu.hrabcak.edupagewidget

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import eu.hrabcak.edupagewidget.edupage.LoginCallback
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class EduLessonTime(
    val start: Date,
    val end: Date
) {
    override fun toString(): String {
        val dateFormat = SimpleDateFormat("H:mm")

        val startFormatted = dateFormat.format(start)
        val endFormatted = dateFormat.format(end)

        return "$startFormatted-$endFormatted"
    }
}

data class EduLesson(
        val name: String?,
        val teacher: String?,
        val classroom: String?,
        val time: EduLessonTime,
        val onlineLessonLink: String?
) {
    companion object {
        fun breakLesson(time: EduLessonTime): EduLesson {
            return EduLesson("Break", "No Teacher", "No Classroom", time, null)
        }
    }
}

class NetworkUtil {
    companion object {
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


}

fun String.toIntNoLeadingZero(): Int {
    return if (this.startsWith("0")) {
        this.slice(1 until this.length).toInt()
    } else {
        this.toInt()
    }
}

data class Timetable(
        val lessons: List<EduLesson>
) {
    fun getNextLesson(): EduLesson? {
        val now = Date()

        for (lesson in lessons) {
            if (now.before(lesson.time.start) || lesson.time.start == now) {
                return lesson
            }
        }
        return null
    }

    fun getCurrentLesson(): EduLesson? {
        val now = Date()

        var isAfterPreviousLesson = false
        for (lesson in lessons) {
            if (lesson.time.start.after(now) ||
                    lesson.time.start == now &&
                    lesson.time.end.before(now) ||
                    lesson.time.end == now) {
                return lesson
            }
            else if (isAfterPreviousLesson) {
                val previousLessonIndex = lessons.indexOf(lesson) - 1

                if (previousLessonIndex == -1) {
                    return null
                }

                val previousLesson = lessons[previousLessonIndex]

                val breakStart = previousLesson.time.end
                val breakEnd = lesson.time.start

                return EduLesson.breakLesson(EduLessonTime(breakStart, breakEnd))
            }
            else if (lesson.time.start.after(now) ||
                    lesson.time.start == now) {
                isAfterPreviousLesson = true
            }
        }

        return null
    }
}

class Edupage(context: Context) {
    var data: JSONObject? = null
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    private var isLoggedIn = false

    fun login(username: String, password: String, loginCallback: LoginCallback) {
        if (isLoggedIn) {
            loginCallback.onError()
        }

        val future: RequestFuture<String> = RequestFuture.newFuture()

        val url = "https://portal.edupage.org/index.php?jwid=jw2&module=Login"
        val parameters: Map<String, String> = mapOf(Pair("meno", username), Pair("password", password), Pair("akcia", "login"))
        val stringRequest: StringRequest = object : StringRequest(Method.POST, url, future, future) {
            override fun getParams(): Map<String, String> {
                return parameters
            }
        }

        requestQueue.add(stringRequest)
        val thread = object : Thread() {

            override fun run() {
                super.run()
                try {
                    val response = future.get(5, TimeUnit.SECONDS)
                    val unparsedJson = response.split("\$j(document).ready(function() {")[1]
                            .split(");")[0]
                            .replace("\t", "")
                            .split("userhome(")[1]
                            .replace("\n", "")
                            .replace("\r", "")
                    data = JSONObject(unparsedJson)
                    isLoggedIn = true
                    loginCallback.onSuccess()
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    loginCallback.onError()
                }
            }
        }
        thread.start()
    }

    private fun idToTeacher(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return if (dbi != null) {
            val teacherData = dbi.getJSONObject("teachers").getJSONObject(id)
            teacherData.getString("firstname") + " " + teacherData.getString("lastname")
        } else {
            null
        }
    }

    private fun idToSubject(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return dbi?.getJSONObject("subjects")?.getJSONObject(id)?.getString("short")
    }

    private fun idToClassroom(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return dbi?.getJSONObject("classrooms")?.getJSONObject(id)?.getString("short")
    }

    fun getTimetableDates(): List<Date>? {
        if (!isLoggedIn) {
            return null
        }

        if (data == null) {
            return null
        }

        val dp = data!!.getJSONObject("dp")
        val dates = dp.getJSONObject("dates")


        val calendar = Calendar.getInstance()

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)

        val timetableDates: MutableList<Date> = mutableListOf()
        for (date in dates.keys()) {
            val (year, month, day) = date.split("-")

            calendar.set(Calendar.YEAR, year.toIntNoLeadingZero())
            calendar.set(Calendar.MONTH, month.toIntNoLeadingZero() - 1)
            calendar.set(Calendar.DAY_OF_MONTH, day.toIntNoLeadingZero())

            timetableDates.add(calendar.time)
        }

        return timetableDates
    }

    fun getTimetable(date: Date): Timetable? {
        if (!isLoggedIn) {
            return null
        }

        if (data == null) {
            return null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val formattedDate = dateFormat.format(date)

        val dp = data!!.getJSONObject("dp")
        val dates = dp.getJSONObject("dates")
        if (!dates.has(formattedDate)) {
            return null
        }

        val datePlans = dates.getJSONObject(formattedDate)
        val plan = datePlans.getJSONArray("plan")

        val timetable: MutableList<EduLesson> = mutableListOf()
        for (i in 0 until plan.length()) {
            val subj = plan.getJSONObject(i)

            val header = subj.getJSONArray("header")
            if (header.length() == 0 || subj.getString("type") != "lesson") {
                continue
            }

            val subjectId = subj.getString("subjectid")
            val subject = idToSubject(subjectId)

            val teacherId = subj.getJSONArray("teacherids").getString(0)
            val teacher = idToTeacher(teacherId)

            val classroomId = subj.getJSONArray("classroomids").getString(0)
            val classroomNumber = idToClassroom(classroomId)

            val start = subj.getString("starttime")
            val end = subj.getString("endtime")

            val (startHour, startMinute) = start.split(":")
            val (endHour, endMinute) = end.split(":")

            val calendar = Calendar.getInstance()
            calendar.time = date

            calendar.set(Calendar.SECOND, 0)

            calendar.set(Calendar.HOUR_OF_DAY, startHour.toInt())
            calendar.set(Calendar.MINUTE, startMinute.toInt())

            val startDate = calendar.time

            calendar.set(Calendar.HOUR_OF_DAY, endHour.toInt())
            calendar.set(Calendar.MINUTE, endMinute.toInt())

            val endDate = calendar.time

            val eduLessonTime = EduLessonTime(startDate, endDate)

            val onlineLessonLink: String? = if (subj.has("ol_url")) {
                subj.getString("ol_url")
            } else {
                null
            }

            val lesson = EduLesson(subject, teacher, classroomNumber, eduLessonTime, onlineLessonLink)
            timetable.add(lesson)
        }

        return Timetable(timetable)
    }
}
