package eu.hrabcak.edupagewidget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.slider.Slider
import java.util.*

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val subdomainEditText = findViewById<EditText>(R.id.subdomain_edittext)
        val usernameEditText = findViewById<EditText>(R.id.username_edittext)
        val passwordEditText = findViewById<EditText>(R.id.password_edittext)

        val updateIntervalSlider = findViewById<Slider>(R.id.update_interval)
        val updateIntervalValue = findViewById<TextView>(R.id.update_interval_value)

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

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val subdomain = subdomainEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val updateInterval = updateIntervalSlider.value.toInt().toString()

            PreferencesHelper.putString(this, "subdomain", subdomain)
            PreferencesHelper.putString(this, "username", username)
            PreferencesHelper.putString(this, "password", password)
            PreferencesHelper.putString(this, "updateInterval", updateInterval)

            AppWidgetAlarm.INTERVAL_MILLIS = updateIntervalSlider.value.toInt() * 1000

            val updateWidgetIntent = Intent(this, NextLessonWidgetProvider::class.java)
            updateWidgetIntent.action = NextLessonWidgetProvider.ACTION_AUTO_UPDATE

            sendBroadcast(updateWidgetIntent)
        }
    }
}