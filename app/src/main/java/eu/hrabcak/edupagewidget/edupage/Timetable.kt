package eu.hrabcak.edupagewidget.edupage

import java.util.*

data class Timetable(
    val lessons: List<Lesson>
) {
    fun getNextLesson(): Lesson? {
        val now = Date()

        for (lesson in lessons) {
            if (now.before(lesson.time.start) || lesson.time.start == now) {
                return lesson
            }
        }
        return null
    }

    fun getCurrentLesson(): Lesson? {
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

                return Lesson.breakLesson(LessonDuration(breakStart, breakEnd))
            }
            else if (lesson.time.start.after(now) ||
                lesson.time.start == now) {
                isAfterPreviousLesson = true
            }
        }

        return null
    }
}