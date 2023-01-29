package eu.hrabcak.edupagewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.util.*

class NextLessonWidgetProvider : AppWidgetProvider() {
    private var edupage: Edupage? = null
    private var remoteViews: RemoteViews? = null

    companion object {
        val ACTION_AUTO_UPDATE = "AUTO_UPDATE"
    }

    override fun onEnabled(context: Context) {
        AppWidgetAlarm.startAlarm(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val componentName = ComponentName(context, NextLessonWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (widgetIds.isEmpty()) {
            AppWidgetAlarm.stopAlarm(context)
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

    private fun showLesson(lesson: EduLesson, index: Int) {
        println("Showing lesson!")

        remoteViews?.setCharSequence(R.id.lesson_number, "setText", (index + 1).toString())
        remoteViews?.setCharSequence(R.id.subject, "setText", lesson.name)
        remoteViews?.setCharSequence(R.id.time, "setText", lesson.time.toString())
        remoteViews?.setCharSequence(R.id.classroom, "setText", lesson.classroom)

        remoteViews?.setViewVisibility(R.id.lessonview, View.VISIBLE)
        remoteViews?.setViewVisibility(R.id.error, View.GONE)
    }

    private fun showMessage(error: String, context: Context) {
        remoteViews?.setCharSequence(R.id.error, "setText", error)

        remoteViews?.setViewVisibility(R.id.error, View.VISIBLE)
        remoteViews?.setViewVisibility(R.id.lessonview, View.GONE)

        applyRemoteViews(context)
    }


    private fun resizeWidget(context: Context, appWidgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        resizeWidget(context, appWidgetId)
        onUpdate(context, appWidgetManager, arrayOf(appWidgetId).toIntArray())
    }

    private fun applyRemoteViews(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
    }

    fun showNextLessonOrError(context: Context) {
        val today = Date()

        if (!edupage!!.getTimetableDates()?.contains(today)!!) {
            println("no school today!")
            showMessage("No school today!", context)
        }

        val timetable = edupage!!.getTimetable(today)
        if (timetable == null) {
            println("Failed to get timetable!")
            showMessage("Error getting timetable", context)
        }

        val nextLesson = timetable?.getNextLesson()
        if (nextLesson == null) {
            println("No more school!")
            showMessage("No more school today!", context)
        }
        else {
            println("Showing lesson!")
            showLesson(nextLesson, timetable.lessons.indexOf(nextLesson))
        }


    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        println("Widget updated!")
        if (remoteViews == null) {
            remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)
        }

        if (edupage == null) {
            val username = PreferencesHelper.getString(context, "username", "no_username")
            if (username == "no_username") {
                showMessage("No username!", context)
                return
            }

            val password = PreferencesHelper.getString(context, "password", "no_password")
            if (password == "no_password") {
                showMessage("No password!", context)
                return
            }

            edupage = Edupage(context)

            edupage!!.login(username, password, object: LoginCallback {
                override fun onError() {
                    println("Failed to log in!")
                    showMessage("Invalid credentials!", context)
                }

                override fun onSuccess() {
                    println("widget logged in!")
                    showNextLessonOrError(context)
                }
            })
        }
        else {
            showNextLessonOrError(context)
        }
    }
}