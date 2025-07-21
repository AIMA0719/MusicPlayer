package com.example.musicplayer.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.example.musicplayer.R
import java.lang.ref.WeakReference

class ToastManager private constructor() {
    private var popupWindow: PopupWindow? = null
    private var contextRef: WeakReference<Context>? = null

    companion object {
        private var instance: ToastManager? = null

        fun getInstance(): ToastManager {
            if (instance == null) {
                instance = ToastManager()
            }
            return instance!!
        }
        
        @JvmStatic
        fun showToast(message: Any) {
            getInstance().show(message)
        }
        
        @JvmStatic
        fun closeToast() {
            getInstance().popupWindow?.dismiss()
            getInstance().popupWindow = null
        }
    }

    @SuppressLint("InflateParams")
    fun show(message: Any) {
        try {
            val context = contextRef?.get() ?: ContextManager.mainContext ?: return
            if (context is Activity) {
                if (!context.isFinishing && !context.isDestroyed) {
                    val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null)
                    val text: TextView = layout.findViewById(R.id.toast_text)

                    val messageText = when(message) {
                        is String -> message
                        is Int -> context.getString(message)
                        else -> ""
                    }
                    text.text = messageText

                    popupWindow?.dismiss()
                    popupWindow = PopupWindow(
                        layout,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                        isOutsideTouchable = true
                        showAtLocation(context.findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        popupWindow?.dismiss()
                        popupWindow = null
                    }, 2000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setContext(context: Context) {
        contextRef = WeakReference(context)
    }
}
