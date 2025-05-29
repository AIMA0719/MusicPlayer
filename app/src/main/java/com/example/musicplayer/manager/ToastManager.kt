package com.example.musicplayer.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.example.musicplayer.R

class ToastManager {

    companion object {
        private var popupWindow: PopupWindow? = null

        @SuppressLint("InflateParams")
        fun show(context: Context, message: Any) {
            try {
                if (context is Activity) {
                    if (!context.isFinishing && !context.isDestroyed) {
                        val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null)
                        val text: TextView = layout.findViewById(R.id.toast_text)

                        val messageText = when(message){
                            is String -> message
                            is Int -> context.getString(message)
                            else -> ""
                        }
                        text.text = messageText

                        popupWindow = PopupWindow(
                            layout,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        popupWindow!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        popupWindow!!.isOutsideTouchable = true
                        popupWindow!!.showAtLocation(
                            context.window.decorView,
                            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                            0,
                            150
                        )

                        val toastView = popupWindow!!.contentView
                        toastView?.translationY = 500f
                        toastView?.animate()
                            ?.translationYBy(-500f)
                            ?.withEndAction {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    toastView.animate()
                                        ?.translationY(500f)
                                        ?.withEndAction { popupWindow!!.dismiss() }
                                        ?.start()
                                }, 2000)
                            }
                            ?.start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun closeToast() {
            if (popupWindow != null && popupWindow!!.isShowing) {
                popupWindow!!.dismiss()
            }
        }
    }
}
