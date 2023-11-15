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
import eu.hrabcak.edupagewidget.helper.EdupageCredentials
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import java.util.*

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

    private var appWidgetManager: AppWidgetManager? = null

    private fun getAppWidgetManager(context: Context): AppWidgetManager {
        if (appWidgetManager == null) {
            appWidgetManager = AppWidgetManager.getInstance(context)
        }

        return appWidgetManager!!
    }

    private fun getAppWidgetIds(context: Context): IntArray {
        val appWidgetManager = getAppWidgetManager(context)
        val componentName = ComponentName(context, this::class.java)

        return appWidgetManager.getAppWidgetIds(componentName)
    }

    private fun createRemoteViews(context: Context): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.nextlesson_appwidget)

        val intent = Intent(ACTION_AUTO_UPDATE)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE)
        )

        remoteViews.setOnClickPendingIntent(R.id.widget_parent, pendingIntent)

        return remoteViews
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

    private fun createNewEdupage(credentials: EdupageCredentials): Edupage {
        val edupage = Edupage()
        val task = edupage.login(credentials.username, credentials.password, credentials.subdomain)

        task.start()
        task.join()

        return edupage
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

    private fun updateTheme(context: Context, remoteViews: RemoteViews) {
        val widgetBackgroundColorString = PreferencesHelper.getString(context, "widgetBackgroundColor", "#001627")
        val lessonBackgroundColorString = PreferencesHelper.getString(context, "lessonBackgroundColor", "#041c2c")
        val textColorString = PreferencesHelper.getString(context, "textColor", "white")

        val widgetBackgroundColor = Color.parseColor(widgetBackgroundColorString)
        val lessonBackgroundColor = Color.parseColor(lessonBackgroundColorString)
        val textColor = Color.parseColor(textColorString)

        val textViews = listOf(
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

        val provider = ComponentName(context, this::class.java)
        getAppWidgetManager(context).updateAppWidget(provider, remoteViews)
    }

    override fun onEnabled(context: Context) {
        println("onEnabled")
        WidgetAlarm.startAlarm(context)
        onUpdate(context, getAppWidgetManager(context), getAppWidgetIds(context))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        println("onDeleted")
        val widgetIds = getAppWidgetIds(context)

        if (widgetIds.isEmpty()) {
            WidgetAlarm.stopAlarm(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        println("onReceive")
        println(intent.action)
        if (intent.action == ACTION_AUTO_UPDATE) {
            val widgetIds = getAppWidgetIds(context)

            onUpdate(context, getAppWidgetManager(context), widgetIds)
        } else {
            super.onReceive(context, intent)
        }

    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        println("Options changed!")
        val remoteViews = createRemoteViews(context)
        getAppWidgetManager(context).updateAppWidget(appWidgetId, remoteViews)

        onUpdate(context, appWidgetManager, arrayOf(appWidgetId).toIntArray())
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        println("Updating...")
        WidgetAlarm.stopAlarm(context)
        WidgetAlarm.startAlarm(context)

        val remoteViews = createRemoteViews(context)

        val credentials = PreferencesHelper.getCredentials(context)
        if (credentials == null) {
            showMessage("Invalid credentials!", context, remoteViews)
            return
        }

        val today = Date()
        val dateFormat = android.icu.text.SimpleDateFormat("yy-dd-MM")

        val formattedDate = dateFormat.format(today)

        val edupage = EdupageCache.get(context, formattedDate)
            ?: createNewEdupage(credentials)

        EdupageCache.put(context, formattedDate, edupage)

        val timetableDates = edupage.getTimetableDates() ?: listOf()
        if (!timetableDates.containsDate(today)) {
            showMessage("No school today!", context, remoteViews)
            return
        }

        val timetable = edupage.getTimetable(today)
        if (timetable == null) {
            showMessage("Error getting timetable", context, remoteViews)
            return
        }

        val nextLesson = timetable.getNextLesson()
        if (nextLesson == null) {
            showMessage("No more school today!", context, remoteViews)
        } else {
            println("Showing lesson...")
            showLesson(nextLesson, remoteViews)
        }

        updateTheme(context, remoteViews)
    }
}