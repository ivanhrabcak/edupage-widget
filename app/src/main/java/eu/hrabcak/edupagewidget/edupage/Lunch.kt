package eu.hrabcak.edupagewidget.edupage

import java.util.Date

class Lunch(
    val menus: List<Menu>,
    var title: String = "",
    var servedFrom: Date? = null,
    var servedTo: Date? = null
) {
    var isCooking = true

    companion object {
        fun notCooking(): Lunch {
            val lunch = Lunch(listOf())

            lunch.isCooking = false

            return lunch
        }
    }
}


// TODO: rating?
data class Menu(
    val name: String,
    val allergens: String,
    val weight: String,
    val number: String,
)