package eu.hrabcak.edupagewidget.edupage

import eu.hrabcak.edupagewidget.exception.AlreadyLoggedInException
import eu.hrabcak.edupagewidget.helper.NetworkHelper
import khttp.post
import khttp.responses.Response
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue

fun String.toIntNoLeadingZero(): Int {
    return if (this.startsWith("0")) {
        this.slice(1 until this.length).toInt()
    } else {
        this.toInt()
    }
}

class Edupage {
    var data: JSONObject? = null
    private var sessionCookie: String? = null
    private var subdomain: String? = null

    var isLoggedIn = false

    fun login(username: String, password: String, subdomain: String): Task {
        if (isLoggedIn) {
            throw AlreadyLoggedInException()
        }

        var requestUrl = "https://${subdomain}.edupage.org/login/index.php"
        var csrfToken: String? = null

        val response = LinkedBlockingQueue<Response>()
        val task = NetworkHelper.doGET(requestUrl, response)
            .then {
                val csrfResponse = response.take()

                csrfToken = csrfResponse.text
                    .split("name=\"csrfauth\" value=\"")[1]
                    .split("\"")[0]

                sessionCookie = csrfResponse.cookies["PHPSESSID"]
            }.then {
                val parameters = mapOf(
                    "username" to username,
                    "password" to password,
                    "csrfauth" to csrfToken!!
                )

                requestUrl = "https://${subdomain}.edupage.org/login/edubarLogin.php"
                val response = post(
                    requestUrl,
                    headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    data = parameters,
                    cookies = mapOf("PHPSESSID" to sessionCookie!!)
                )

                val rawJSON = response.text.split("\$j(document).ready(function() {")[1]
                    .split(");")[0]
                    .replace("\t", "")
                    .split("userhome(")[1]
                    .replace("\n", "")
                    .replace("\r", "")
                data = JSONObject(rawJSON)
                isLoggedIn = true
                this.subdomain = subdomain
            }

        return task
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

    fun getLunch(date: Date): Lunch? {
        if (!isLoggedIn) {
            return null
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val formattedDate = dateFormat.format(date)

        val requestUrl = "https://${subdomain}.edupage.org/menu/?date=$formattedDate"

        val result = LinkedBlockingQueue<Lunch?>()

        val response = LinkedBlockingQueue<Response>()
        val task = NetworkHelper.doGET(
            requestUrl,
            response,
            mapOf(
                "PHPSESSID" to sessionCookie!!
            )
        ).then {
            val lunchDataResponse = response.take()

            val lunchDataRaw = lunchDataResponse.text.split("edupageData: ")[1].split(",\r\n")[0]
            val lunchData = JSONObject(lunchDataRaw)

            val lunchesData = lunchData.getJSONObject(subdomain!!)

            val lunchDateFormat = SimpleDateFormat("yyyy-MM-dd")

            var lunch = lunchesData.getOrNull<JSONObject>("novyListok")?.getOrNull<JSONObject>(lunchDateFormat.format(date))
            if (lunch == null) {
                result.add(null)
                return@then
            }

            lunch = lunch.getOrNull<JSONObject>("2")

            if (lunch?.getBoolean("isCooking") != true) {
                result.add(Lunch.notCooking())
                return@then
            }

            val servedFromRaw = lunch.getOrNull<String>("vydaj_od")
            val servedToRaw = lunch.getOrNull<String>("vyday_do")

            val servingDateFormat = SimpleDateFormat("H-mm")

            val servedFrom = if (servedFromRaw != null) {
               servingDateFormat.parse(servedFromRaw)
            } else {
                null
            }

            val servedTo = if (servedToRaw != null) {
                servingDateFormat.parse(servedToRaw)
            } else {
                null
            }

            val title = lunch.getString("nazov")

            val menus = mutableListOf<Menu>()
            for (food in lunch.getJSONArray("rows").iterator<JSONObject?>()) {
                if (food == null) {
                    continue
                }

                val name = food.getString("nazov")
                val allergens = food.getString("alergenyStr")
                val weight = food.getString("hmotnostiStr")
                var number = food.getOrNull<String>("menusStr") ?: ""

                number = number.replace(": ", "")

                menus.add(Menu(
                    name,
                    allergens,
                    weight,
                    number
                ))
            }

            result.add(Lunch(
                menus,
                title,
                servedFrom,
                servedTo
            ))
        }.onError {
            it.printStackTrace()
            result.add(null)
        }

        task.start()
        task.join()

        return result.take()
    }

    fun getTimetable(date: Date): Timetable? {
        if (!isLoggedIn) {
            println("not logged in")
            return null
        }

        if (data == null) {
            println("data is null")
            return null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val formattedDate = dateFormat.format(date)

        val dp = data!!.getJSONObject("dp")
        val dates = dp.getJSONObject("dates")
        if (!dates.has(formattedDate)) {
            println("date not in data")
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

            val lessonNumber = subj.getString("period").toInt()

            val lesson = Lesson(subject, teacher, classroomNumber, lessonDuration, onlineLessonLink, lessonNumber)
            timetable.add(lesson)
        }

        return Timetable(timetable)
    }
}
