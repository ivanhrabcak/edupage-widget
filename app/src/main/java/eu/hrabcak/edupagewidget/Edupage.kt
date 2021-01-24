package eu.hrabcak.edupagewidget

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log


data class Time(
        val hour: Int,
        val minute: Int
) {

    companion object {
        fun now(): Time {
            val format = SimpleDateFormat("HH:mm")
            val currentTime = format.format(Calendar.getInstance().time)

            return Time(currentTime)
        }
    }

    constructor(formattedTime: String) : this(formattedTime.split(":")[0].toInt(), formattedTime.split(":")[1].toInt())

    fun isBefore(time: Time): Boolean {
        return time.minute < minute &&
                time.hour < hour
    }

    fun isExact(time: Time): Boolean {
        return time.minute == minute &&
                time.hour == hour
    }

    fun isAfter(time: Time): Boolean {
        return !isBefore(time)
    }
}

data class EduLessonTime(
        val start: Time,
        val end: Time
)

data class EduDate(
        val year: String,
        val day: String,
        val month: String
) {

    constructor(formattedDate: String) : this(formattedDate.split("-")[0], formattedDate.split("-")[1], formattedDate.split("-")[2])

    companion object {
        fun today(): EduDate {
            val format = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = format.format(Calendar.getInstance().time)

            return EduDate(currentDate)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun tomorrow(): EduDate {
            val format = SimpleDateFormat("yyyy-MM-dd")
            val tomorrowDate = format.format(Date.from(
                    Calendar.getInstance().time
                            .toInstant()
                            .plus(Duration.ofDays(1L))))

            return EduDate(tomorrowDate)
        }
    }

    fun toEduFormat(): String {
        return "${year}-${month}-${day}"
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
        fun isNetworkAvailable(context: Context): Boolean { // no better way to do this unfortunately :(((
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }


}

data class Timetable(
        val lessons: List<EduLesson>
) {
    fun getNextLesson(): EduLesson? {
        var previousLesson: EduLesson? = null
        for (lesson in lessons) {
            if (lesson.time.start.isAfter(Time.now()) || lesson.time.start.isExact(Time.now())) {
                if (previousLesson == null) {
                    return lessons[0]
                }

                return previousLesson
            }

            if (previousLesson == null) {
                previousLesson = lesson
            }
        }
        return null
    }

    fun getCurrentLesson(): EduLesson? {
        var isAfterPreviousLesson = false
        for (lesson in lessons) {
            if (lesson.time.start.isAfter(Time.now()) ||
                    lesson.time.start.isExact(Time.now()) &&
                    lesson.time.end.isBefore(Time.now()) ||
                    lesson.time.end.isExact(Time.now())) {
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
            else if (lesson.time.start.isAfter(Time.now()) ||
                    lesson.time.start.isExact(Time.now())) {
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

    fun idToTeacher(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return if (dbi != null) {
            val teacherData = dbi.getJSONObject("teachers").getJSONObject(id)
            teacherData.getString("firstname") + " " + teacherData.getString("lastname")
        } else {
            null
        }
    }

    fun idToSubject(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return dbi?.getJSONObject("subjects")?.getJSONObject(id)?.getString("short")
    }

    fun idToClassroom(id: String): String? {
        val dbi = data?.getJSONObject("dbi")
        return dbi?.getJSONObject("classrooms")?.getJSONObject(id)?.getString("short")
    }

    fun getTimetableDates(): List<EduDate>? {
        if (!isLoggedIn) {
            return null
        }

        if (data == null) {
            return null
        }

        val dp = data!!.getJSONObject("dp")
        val dates = dp.getJSONObject("dates")

        val timetableDates: MutableList<EduDate> = mutableListOf()
        for (date in dates.keys()) {
            timetableDates.add(EduDate(date))
        }

        return timetableDates
    }

    fun getTimetable(date: EduDate): Timetable? {
        if (!isLoggedIn) {
            return null
        }

        if (data == null) {
            return null
        }

        val dp = data!!.getJSONObject("dp")
        val dates = dp.getJSONObject("dates")
        if (!dates.has(date.toEduFormat())) {
            return null
        }

        val datePlans = dates.getJSONObject(date.toEduFormat())
        val plan = datePlans.getJSONArray("plan")

        val timetable: MutableList<EduLesson> = mutableListOf()
        for (i in 0 until plan.length()) {
            val subj = plan.getJSONObject(i)

            val header = subj.getJSONArray("header")
            if (header.length() == 0) {
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
            val eduLessonTime = EduLessonTime(Time(start), Time(end))

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
