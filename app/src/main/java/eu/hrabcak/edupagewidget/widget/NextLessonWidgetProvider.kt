package eu.hrabcak.edupagewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import eu.hrabcak.edupagewidget.*
import eu.hrabcak.edupagewidget.edupage.Edupage
import eu.hrabcak.edupagewidget.edupage.Lesson
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

//fun Date(): Date {
//    val calendar = Calendar.getInstance()
//    calendar.add(Calendar.DAY_OF_YEAR, 1)
//    calendar.set(Calendar.HOUR_OF_DAY, 8)
//    calendar.set(Calendar.MINUTE, 0)
//
//    return calendar.time
//}

fun List<Date>.containsDate(date: Date): Boolean {
    val calendar = Calendar.getInstance()

    return this.any {
        calendar.time = date

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        calendar.time = it

        val compareDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val compareYear = calendar.get(Calendar.YEAR)

        dayOfYear == compareDayOfYear && year == compareYear
    }
}

class NextLessonWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_AUTO_UPDATE = "AUTO_UPDATE"
    }

    override fun onEnabled(context: Context) {
        WidgetAlarm.startAlarm(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val componentName = ComponentName(context, NextLessonWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (widgetIds.isEmpty()) {
            WidgetAlarm.stopAlarm(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action != ACTION_AUTO_UPDATE) {
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)

        val componentName = ComponentName(context, NextLessonWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        onUpdate(context, appWidgetManager, widgetIds)
    }

    private fun showLesson(lesson: Lesson, remoteViews: RemoteViews) {
        remoteViews.run {
            setCharSequence(R.id.lesson_number, "setText", lesson.lessonNumber.toString() + ".")
            setCharSequence(R.id.subject, "setText", lesson.name)
            setCharSequence(R.id.time, "setText", lesson.time.toString())
            setCharSequence(R.id.classroom, "setText", lesson.classroom)

            setViewVisibility(R.id.lessonview, View.VISIBLE)
            setViewVisibility(R.id.next_lesson_title, View.VISIBLE)
            setViewVisibility(R.id.error, View.GONE)
        }
    }

    private fun showMessage(error: String, context: Context, remoteViews: RemoteViews) {
        remoteViews.run {
            setCharSequence(R.id.error, "setText", error)
            setViewVisibility(R.id.error, View.VISIBLE)
            setViewVisibility(R.id.lessonview, View.GONE)
            setViewVisibility(R.id.next_lesson_title, View.GONE)
        }

        updateTheme(context, remoteViews)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val remoteViews = createRemoteViews(context)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)

        onUpdate(context, appWidgetManager, arrayOf(appWidgetId).toIntArray())
    }

    private fun applyRemoteViews(context: Context, remoteViews: RemoteViews) {
        println("Applying views..")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
    }

    private fun createRemoteViews(context: Context): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)

        // update widget on click
        val intent = Intent(ACTION_AUTO_UPDATE)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        remoteViews.setOnClickPendingIntent(R.id.widget_parent, pendingIntent)

        return remoteViews
    }

    private fun updateTheme(context: Context, remoteViews: RemoteViews) {
        val widgetBackgroundColorString = PreferencesHelper.getString(context, "widgetBackgroundColor", "#001627")
        val lessonBackgroundColorString = PreferencesHelper.getString(context, "lessonBackgroundColor", "#041c2c")
        val textColorString = PreferencesHelper.getString(context, "textColor", "white")

        val widgetBackgroundColor = Color.parseColor(widgetBackgroundColorString)
        val lessonBackgroundColor = Color.parseColor(lessonBackgroundColorString)
        val textColor = Color.parseColor(textColorString)

        val textViews = arrayOf(
            R.id.next_lesson_title, R.id.lesson_number, R.id.subject,
            R.id.time, R.id.classroom, R.id.error
        )

        textViews.forEach {
            remoteViews.setTextColor(it, textColor)
        }

        remoteViews.run {
            textViews.forEach {
                setTextColor(it, textColor)
            }

            setInt(R.id.widget_parent, "setBackgroundColor", widgetBackgroundColor)
            setInt(R.id.lessonview, "setBackgroundColor", lessonBackgroundColor)
        }

        applyRemoteViews(context, remoteViews)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        WidgetAlarm.stopAlarm(context)
        WidgetAlarm.startAlarm(context)

        val remoteViews = createRemoteViews(context)

        val username = PreferencesHelper.getString(context, "username", "no_username")
        val password = PreferencesHelper.getString(context, "password", "no_password")
        val subdomain = PreferencesHelper.getString(context, "subdomain", "no_subdomain")
        if (username == "no_username" || password == "no_password" || subdomain == "no_subdomain") {
            showMessage("Invalid credentials!", context, remoteViews)
            return
        }

        val today = Date()
        val dateFormat = SimpleDateFormat("yy-dd-MM")

        val todayDateString = dateFormat.format(today)

        val edupage = Edupage()
        edupage.login(username, password, subdomain).then {
            if (!edupage.getTimetableDates()?.containsDate(today)!!) {
                showMessage("No school today!", context, remoteViews)
                return@then
            }

            val timetable = edupage.getTimetable(today)
            if (timetable == null) {
                showMessage("Error getting timetable", context, remoteViews)
                return@then
            }

            EdupageCache.put(context, todayDateString, edupage)

            val nextLesson = timetable.getNextLesson()
            if (nextLesson == null) {
                showMessage("No more school today!", context, remoteViews)
            } else {
                println("Showing lesson...")
                showLesson(nextLesson, remoteViews)
                applyRemoteViews(context, remoteViews)
            }
        }.onError { e ->
            if (e is UnknownHostException) {
                val cached = EdupageCache.get(context, todayDateString)

                if (cached == null) {
                    showMessage("Network down!", context, remoteViews)
                    return@onError
                }

                val timetable = cached.getTimetable(today)
                if (timetable == null) {
                    showMessage("Error getting timetable", context, remoteViews)
                    return@onError
                }

                println("Will be using cached!")

                val nextLesson = timetable.getNextLesson()
                if (nextLesson == null) {
                    showMessage("No more school today!", context, remoteViews)
                } else {
                    showLesson(nextLesson, remoteViews)
                    applyRemoteViews(context, remoteViews)
                }
            } else {
                e.printStackTrace()
            }
        }.start()

    }
}