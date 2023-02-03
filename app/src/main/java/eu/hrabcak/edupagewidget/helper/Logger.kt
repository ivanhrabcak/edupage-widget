package eu.hrabcak.edupagewidget.helper

import android.content.Context
import android.os.Environment
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_FILENAME = "%date%.log"

    fun log(context: Context, message: String) {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())

        val logFilename = LOG_FILENAME.replace("%date%", today)
        val logFilePath = context.filesDir.path + File.separator + logFilename
        val logFile = File(logFilePath)
        logFile.createNewFile()

        val writer = BufferedWriter(FileWriter(logFile, true))

        writer.write(message)
        writer.newLine()
        writer.close()
    }

    fun getAvailableLogs(context: Context): List<String> = context.fileList().filter {
        it.endsWith(".log")
    }

    fun getLog(context: Context, filename: String): String {
        val logFilePath = context.filesDir.path + File.separator + filename
        val logFile = File(logFilePath)

        val reader = BufferedReader(FileReader(logFile))
        val fileContent = reader.readLines().joinToString("\n")

        reader.close()

        return fileContent
    }
}