package eu.hrabcak.edupagewidget

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        println(EduDate.tomorrow())

        val edupage: Edupage = Edupage(this)
        println(edupage.login("Usernam", "Password", object: LoginCallback {
            override fun onError() {
                println("error!")
            }

            override fun onSuccess() {
                println("Logged in!")
                println(edupage.getTimetable(EduDate("2021", "25", "01")))
            }

        }))
    }
}