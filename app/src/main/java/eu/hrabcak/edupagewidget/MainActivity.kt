package eu.hrabcak.edupagewidget

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.slider.Slider
import eu.hrabcak.edupagewidget.helper.PreferencesHelper
import eu.hrabcak.edupagewidget.widget.NextLessonWidgetProvider
import eu.hrabcak.edupagewidget.widget.WidgetAlarm
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var subdomainEditText: EditText
    lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    lateinit var updateIntervalSlider: Slider
    lateinit var updateIntervalValue: TextView


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subdomainEditText = findViewById(R.id.subdomain_edittext)
        usernameEditText = findViewById(R.id.username_edittext)
        passwordEditText = findViewById(R.id.password_edittext)

        updateIntervalSlider = findViewById(R.id.update_interval)
        updateIntervalValue = findViewById(R.id.update_interval_value)

        val savedSubdomain = PreferencesHelper.getString(this, "subdomain", "no_subdomain")
        val savedUsername = PreferencesHelper.getString(this, "username", "no_username")
        val savedPassword = PreferencesHelper.getString(this, "password", "no_password")
        val savedUpdateInterval = PreferencesHelper.getString(this, "updateInterval", "30").toInt()

        if (savedSubdomain != "no_subdomain") {
            subdomainEditText.setText(savedSubdomain)
        }

        if (savedUsername != "no_username") {
            usernameEditText.setText(savedUsername)
        }

        if (savedPassword != "no_password") {
            passwordEditText.setText(savedPassword)
        }

        updateIntervalSlider.value = savedUpdateInterval.toFloat()
        updateIntervalValue.text = savedUpdateInterval.toString()

        updateIntervalSlider.addOnChangeListener { _, value, _ ->
            updateIntervalValue.text = value.toInt().toString()
        }

        findViewById<Button>(R.id.themes_button).setOnClickListener {
            val themingActivityIntent = Intent(this, ThemingActivity::class.java)
            startActivity(themingActivityIntent)
        }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val subdomain = subdomainEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val updateInterval = updateIntervalSlider.value.toInt().toString()

            PreferencesHelper.putString(this, "subdomain", subdomain)
            PreferencesHelper.putString(this, "username", username)
            PreferencesHelper.putString(this, "password", password)
            PreferencesHelper.putString(this, "updateInterval", updateInterval)

            WidgetAlarm.INTERVAL_MILLIS = updateIntervalSlider.value.toInt() * 1000

            val updateWidgetIntent = Intent(this, NextLessonWidgetProvider::class.java)
            updateWidgetIntent.action = NextLessonWidgetProvider.ACTION_AUTO_UPDATE

            sendBroadcast(updateWidgetIntent)
        }
    }
}