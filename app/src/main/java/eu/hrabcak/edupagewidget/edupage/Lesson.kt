package eu.hrabcak.edupagewidget.edupage

data class Lesson(
    val name: String?,
    val teacher: String?,
    val classroom: String?,
    val time: LessonDuration,
    val onlineLessonLink: String?,
    val lessonNumber: Int
) {
    companion object {
        fun breakLesson(time: LessonDuration): Lesson {
            return Lesson("Break", "No Teacher", "No Classroom", time, null, -1)
        }
    }
}