package com.everett.blindwatermark

import android.app.Application
import android.content.Context
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class BlindWatermarkApp : Application() {

    companion object {
        private const val CRASH_LOG_FILENAME = "crash_log.txt"

        fun getCrashLogFile(context: Context): File {
            return File(context.filesDir, CRASH_LOG_FILENAME)
        }

        fun hasCrashLog(context: Context): Boolean {
            return getCrashLogFile(context).exists() && getCrashLogFile(context).length() > 0
        }

        fun readCrashLog(context: Context): String {
            return try {
                getCrashLogFile(context).readText()
            } catch (e: Exception) {
                "无法读取崩溃日志: ${e.message}"
            }
        }

        fun clearCrashLog(context: Context) {
            try {
                getCrashLogFile(context).delete()
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val sw = StringWriter()
                val pw = PrintWriter(sw)
                pw.println("=== 隐印 崩溃日志 ===")
                pw.println("时间: $timestamp")
                pw.println("线程: ${thread.name}")
                pw.println("异常: ${throwable.javaClass.name}")
                pw.println("消息: ${throwable.message}")
                pw.println()
                pw.println("堆栈跟踪:")
                throwable.printStackTrace(pw)

                // Write to file
                val logFile = getCrashLogFile(this)
                logFile.writeText(sw.toString())

                Log.e("CrashHandler", "崩溃日志已保存到: ${logFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("CrashHandler", "保存崩溃日志失败", e)
            }

            // Call default handler to let the app crash normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
