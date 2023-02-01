package eu.hrabcak.edupagewidget.edupage

import java.text.SimpleDateFormat
import java.util.*

data class LessonDuration(
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