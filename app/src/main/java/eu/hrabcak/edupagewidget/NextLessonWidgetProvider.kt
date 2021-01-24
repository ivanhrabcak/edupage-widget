package eu.hrabcak.edupagewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.view.marginLeft
import kotlin.math.floor

class NextLessonWidgetProvider : AppWidgetProvider() {
    private var edupage: Edupage? = null
    private var remoteViews: RemoteViews? = null

    private fun showLesson(lesson: EduLesson, index: Int) {
        remoteViews?.setViewVisibility(R.id.lessonview, View.VISIBLE)
        remoteViews?.setViewVisibility(R.id.error, View.GONE)

        remoteViews?.setCharSequence(R.id.lesson_number, "setText", (index + 1).toString())
        remoteViews?.setCharSequence(R.id.subject, "setText", lesson.name)
        remoteViews?.setCharSequence(R.id.time, "setText", lesson.time.toString())
        remoteViews?.setCharSequence(R.id.classroom, "setText", lesson.classroom)
    }

    private fun showMessage(error: String) {
//        remoteViews?.setViewVisibility(R.id.error, View.VISIBLE)
//        remoteViews?.setViewVisibility(R.id.lessonview, View.GONE)
//
//        remoteViews?.setCharSequence(R.id.error, "setText", error)
    }

    private fun smallerWidget(context: Context, appWidgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)
        remoteViews?.apply {
            setViewVisibility(R.id.subject, View.VISIBLE)
            setViewVisibility(R.id.time, View.GONE)
            setViewVisibility(R.id.classroom, View.GONE)
            setTextViewTextSize(R.id.lesson_number, TypedValue.COMPLEX_UNIT_DIP, 32f)
            setViewPadding(R.id.lesson_number, 0, 0, 15, 0)
            setInt(R.id.lessonview, "setOrientation", LinearLayout.HORIZONTAL)
        }

        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
        println("applied!")
    }

    private fun largerWidget(context: Context, appWidgetId: Int) {
        val remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)
        remoteViews.apply {
            setViewVisibility(R.id.time, View.VISIBLE)
            setViewVisibility(R.id.classroom, View.VISIBLE)
            setTextViewTextSize(R.id.lesson_number, TypedValue.COMPLEX_UNIT_DIP, 18f)
            setViewPadding(R.id.lesson_number, 0, 0, 0, 0)
            setInt(R.id.lessonview, "setOrientation", LinearLayout.VERTICAL)
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        println("I am called!")
        if (context == null || appWidgetManager == null) {
            println("I've returned because context is null!")
            return
        }

        val height = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        if (height == 94) {
            println("smaller!")
            smallerWidget(context, appWidgetId)
        }
        else {
            println("bigger! $height")
            largerWidget(context, appWidgetId)
        }

    }

    private fun applyRemoteViews(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
    }

    fun showNextLessonOrError(context: Context) {
        val today = EduDate.today()
        if (!edupage!!.getTimetableDates()?.contains(today)!!) {
            showMessage("No school today!")
        }
        val timetable = edupage!!.getTimetable(today)
        if (timetable == null) {
            showMessage("Error getting timetable")
        }

        val nextLesson = timetable?.getNextLesson()
        if (nextLesson == null) {
            showMessage("No more school today!")
        }
        else {
            showLesson(nextLesson, timetable.lessons.indexOf(nextLesson))
        }

        if (timetable != null) {
            showLesson(timetable.lessons[0], 0)
        }


    }

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {

        if (context == null) {
            println("context is null")
            return
        }

        val id = PreferencesHelper.getString(context, "widgetNumber", "1").toInt()

        PreferencesHelper.putString(context, "widgetNumber", (id + 1).toString())

        if (remoteViews == null) {
            remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)
        }

        if (edupage == null) {
            val username = PreferencesHelper.getString(context, "username_${id}", "no_username")
            if (username == "no_username") {
                showMessage("No username!")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
                return
            }

            val password = PreferencesHelper.getString(context, "password_${id}", "no_password")
            if (password == "no_password") {
                showMessage("No password!")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
                return
            }

            edupage = Edupage(context)

            edupage!!.login(username, password, object: LoginCallback {
                override fun onError() {
                    println("Failed to log in!")
                    showMessage("Invalid credentials!")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.updateAppWidget(ComponentName(context, this::class.java), remoteViews)
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