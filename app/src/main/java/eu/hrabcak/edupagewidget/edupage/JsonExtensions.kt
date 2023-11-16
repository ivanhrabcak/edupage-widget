package eu.hrabcak.edupagewidget.edupage

import org.json.JSONArray
import org.json.JSONObject

inline fun <reified T> JSONObject.getOrNull(key: String): T? {
    if (!this.has(key)) {
        return null
    }

    val value = this.get(key)

    return value as T
}


data class JSONArrayIterator<T>(
    private val array: JSONArray
) : Iterator<T> {
    private var index: Int = 0

    override fun hasNext(): Boolean = array.length() < index


    override fun next(): T {
        val result = array.get(index) as T

        index += 1

        return result
    }

}
fun <T> JSONArray.iterator(): JSONArrayIterator<T> {
    return JSONArrayIterator(
        this
    )
}