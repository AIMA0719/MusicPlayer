package com.example.musicplayer.Manager

import android.util.Log
import com.gun0912.tedpermission.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter

object LogManager {
    private const val TAG = "MusicPlayer"
    private const val MAX_LENGTH = 4000

    fun <T> e(message: T) {
        if (BuildConfig.DEBUG) {
            val msg = message.toString()
            if (msg.length > MAX_LENGTH) {
                Log.e(TAG, buildLogMsg(msg.substring(0, MAX_LENGTH)))
                e(msg.substring(MAX_LENGTH))
            } else {
                Log.e(TAG, buildLogMsg(msg))
            }
        }
    }

    fun <T> w(message: T) {
        if (BuildConfig.DEBUG) {
            val msg = message.toString()
            if (msg.length > MAX_LENGTH) {
                Log.w(TAG, buildLogMsg(msg.substring(0, MAX_LENGTH)))
                w(msg.substring(MAX_LENGTH))
            } else {
                Log.w(TAG, buildLogMsg(msg))
            }
        }
    }

    fun <T> i(message: T) {
        if (BuildConfig.DEBUG) {
            val msg = message.toString()
            if (msg.length > MAX_LENGTH) {
                Log.i(TAG, buildLogMsg(msg.substring(0, MAX_LENGTH)))
                i(msg.substring(MAX_LENGTH))
            } else {
                Log.i(TAG, buildLogMsg(msg))
            }
        }
    }

    fun <T> d(message: T) {
        if (BuildConfig.DEBUG) {
            val msg = message.toString()
            if (msg.length > MAX_LENGTH) {
                Log.d(TAG, buildLogMsg(msg.substring(0, MAX_LENGTH)))
                d(msg.substring(MAX_LENGTH))
            } else {
                Log.d(TAG, buildLogMsg(msg))
            }
        }
    }

    fun <T> v(message: T) {
        if (BuildConfig.DEBUG) {
            val msg = message.toString()
            if (msg.length > MAX_LENGTH) {
                Log.v(TAG, buildLogMsg(msg.substring(0, MAX_LENGTH)))
                v(msg.substring(MAX_LENGTH))
            } else {
                Log.v(TAG, buildLogMsg(msg))
            }
        }
    }

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
