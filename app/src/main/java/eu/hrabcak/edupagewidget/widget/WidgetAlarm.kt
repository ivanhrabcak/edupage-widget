package eu.hrabcak.edupagewidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import eu.hrabcak.edupagewidget.helper.Logger
import java.util.Calendar

object WidgetAlarm {
    private const val ALARM_ID = 0
    var INTERVAL_MILLIS = 30_000

    fun startAlarm(context: Context) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS)

        val alarmIntent = Intent(context, NextLessonWidgetProvider::class.java)
        alarmIntent.action = NextLessonWidgetProvider.ACTION_AUTO_UPDATE

        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
            INTERVAL_MILLIS.toLong(), pendingIntent)
    }

    fun stopAlarm(context: Context) {
        val alarmIntent = Intent(NextLessonWidgetProvider.ACTION_AUTO_UPDATE)
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}