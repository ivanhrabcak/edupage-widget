package eu.hrabcak.edupagewidget

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import eu.hrabcak.edupagewidget.edupage.Lesson
import eu.hrabcak.edupagewidget.edupage.LessonDuration
import eu.hrabcak.edupagewidget.edupage.Task
import eu.hrabcak.edupagewidget.edupage.Timetable
import eu.hrabcak.edupagewidget.exception.NotLoggedInException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun String.toIntNoLeadingZero(): Int {
    return if (this.startsWith("0")) {
        this.slice(1 until this.length).toInt()
    } else {
        this.toInt()
    }
}

class Edupage(context: Context) {
    var data: JSONObject? = null
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    private var isLoggedIn = false

    fun login(username: String, password: String): Task {
        if (isLoggedIn) {
            throw NotLoggedInException()
        }

        val future: RequestFuture<String> = RequestFuture.newFuture()

        val url = "https://portal.edupage.org/index.php?jwid=jw2&module=Login"
        val parameters: Map<String, String> = mapOf(
            "meno" to username,
            "password" to password,
            "akcia" to "login"
        )

        val stringRequest: StringRequest = object : StringRequest(Method.POST, url, future, future) {
            override fun getParams(): Map<String, String> {
                return parameters
            }
        }

        requestQueue.add(stringRequest)
        val thread = thread(false) {
            try {
                val response = future.get(5, TimeUnit.SECONDS)
                val rawJSON = response.split("\$j(document).ready(function() {")[1]
                        .split(");")[0]
                        .replace("\t", "")
                        .split("userhome(")[1]
                        .replace("\n", "")
                        .replace("\r", "")
                data = JSONObject(rawJSON)
                isLoggedIn = true
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Task(thread)
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

        val timetable: MutableList<Lesson> = mutableListOf()
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

            val lessonDuration = LessonDuration(startDate, endDate)

            val onlineLessonLink: String? = if (subj.has("ol_url")) {
                subj.getString("ol_url")
            } else {
                null
            }

            val lesson = Lesson(subject, teacher, classroomNumber, lessonDuration, onlineLessonLink)
            timetable.add(lesson)
        }

        return Timetable(timetable)
    }
}
