package com.example.musicplayer.manager

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object LogManager {
    private const val TAG = "MusicPlayer"
    private const val MAX_LENGTH = 4000

    private enum class LogLevel(val logFunction: (String, String) -> Int) {
        ERROR(Log::e),
        WARN(Log::w),
        INFO(Log::i),
        DEBUG(Log::d),
        VERBOSE(Log::v)
    }

    private fun log(level: LogLevel, message: String) {
        if (message.length > MAX_LENGTH) {
            level.logFunction(TAG, buildLogMsg(message.substring(0, MAX_LENGTH)))
            log(level, message.substring(MAX_LENGTH))
        } else {
            level.logFunction(TAG, buildLogMsg(message))
        }
    }

    fun <T> e(message: T) = log(LogLevel.ERROR, message.toString())
    fun <T> w(message: T) = log(LogLevel.WARN, message.toString())
    fun <T> i(message: T) = log(LogLevel.INFO, message.toString())
    fun <T> d(message: T) = log(LogLevel.DEBUG, message.toString())
    fun <T> v(message: T) = log(LogLevel.VERBOSE, message.toString())

    private fun buildLogMsg(message: String): String {
        val ste = Thread.currentThread().stackTrace[4]
        val sb = StringBuilder()

        try {
            sb.append("[")
                .append(ste.methodName)
                .append("()")
                .append("]")
                .append(" :: ")
                .append(message)
                .append(" (")
                .append(ste.fileName)
                .append(":")
                .append(ste.lineNumber)
                .append(")")
        } catch (e: Exception) {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))

            sb.append("[")
                .append(ste.methodName)
                .append("()")
                .append("]")
                .append(" :: ")
                .append(" (")
                .append(ste.fileName)
                .append(":")
                .append(ste.lineNumber)
                .append(")")
        }

        return sb.toString()
    }
}
