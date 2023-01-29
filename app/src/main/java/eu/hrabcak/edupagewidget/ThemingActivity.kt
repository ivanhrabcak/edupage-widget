package eu.hrabcak.edupagewidget

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import eu.hrabcak.edupagewidget.widget.NextLessonWidgetProvider
import eu.hrabcak.edupagewidget.widget.WidgetTheme

class ThemingActivity : Activity() {
    private var appliedTheme: WidgetTheme = THEMES[0]

    companion object {
        val THEMES = arrayOf(
            WidgetTheme("Edupage", "#001627", "#041c2c", "white"),
            WidgetTheme("Solarized", "#fdf6e3", "#eee8d5", "#073642"),
            WidgetTheme("Dark", "#1D1F21", "#282A2E", "#C5C8C6"),
            WidgetTheme("OLED", "#000000", "#202525", "white"),
            WidgetTheme("Light", "white", "#E3E2E6", "black"),
            WidgetTheme("RedBrown", "#160B09", "#3A2521", "#E6D3D0"),
            WidgetTheme("Swamp", "#161C16", "#3B473A", "#BBDDB9"),
            WidgetTheme("Deep Purple", "#382336", "#563953", "#E5D2E2"),
            WidgetTheme("Retro", "#C2B28F", "#E4D8B4", "#272324")
        )
    }

    private fun applyTheme(widgetTheme: WidgetTheme) {
        val widgetView = findViewById<LinearLayout>(R.id.preview_view)
        widgetView.removeAllViews()

        val child = layoutInflater.inflate(R.layout.nextlesson_appwidget, null)
        widgetView.addView(child)

        appliedTheme = widgetTheme


        val parent = widgetView.findViewById<LinearLayout>(R.id.widget_parent)
        val lessonCell = widgetView.findViewById<LinearLayout>(R.id.lessonview)

        val nextLessonTitle = widgetView.findViewById<TextView>(R.id.next_lesson_title)
        val lessonNumber = widgetView.findViewById<TextView>(R.id.lesson_number)
        val subject = widgetView.findViewById<TextView>(R.id.subject)
        val time = widgetView.findViewById<TextView>(R.id.time)
        val classroom = widgetView.findViewById<TextView>(R.id.classroom)

        val errorText = widgetView.findViewById<TextView>(R.id.error)

        val textViews = arrayOf(
            nextLessonTitle, lessonNumber, subject, time, classroom, errorText
        )

        textViews.forEach {
            it.setTextColor(Color.parseColor(widgetTheme.textColor))
        }

        parent.setBackgroundColor(Color.parseColor(widgetTheme.widgetBackgroundColor))
        lessonCell.setBackgroundColor(Color.parseColor(widgetTheme.lessonBackgroundColor))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theming)

        val chosenThemeIndex = PreferencesHelper.getString(this, "chosenThemeIndex", "0").toInt()

        val adapter = ChooseableRecyclerViewAdapter(THEMES, chosenThemeIndex) { theme ->
            applyTheme(theme)
        }

        val themeListView = findViewById<RecyclerView>(R.id.theme_list_view)
        themeListView.layoutManager = LinearLayoutManager(this)
        themeListView.adapter = adapter

        findViewById<Button>(R.id.go_back).setOnClickListener {
            PreferencesHelper.putString(this, "widgetBackgroundColor", appliedTheme.widgetBackgroundColor)
            PreferencesHelper.putString(this, "lessonBackgroundColor", appliedTheme.lessonBackgroundColor)
            PreferencesHelper.putString(this, "textColor", appliedTheme.textColor)

            PreferencesHelper.putString(this, "chosenThemeIndex", adapter.chosenThemeIndex.toString())

            val updateWidgetIntent = Intent(this, NextLessonWidgetProvider::class.java)
            updateWidgetIntent.action = NextLessonWidgetProvider.ACTION_AUTO_UPDATE

            sendBroadcast(updateWidgetIntent)

            finish()
        }
    }
}