package com.example.musicplayer.manager

import android.annotation.SuppressLint
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

import android.view.animation.Animation
import android.view.animation.AnimationUtils

class ToastManager private constructor() {
    private var popupWindow: PopupWindow? = null

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
            getInstance().dismiss()
        }
    }

    @SuppressLint("InflateParams")
    fun show(message: Any) {
        try {
            val context = ContextManager.getActivity() ?: return
            if (!context.isFinishing && !context.isDestroyed) {
                val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null)
                val text: TextView = layout.findViewById(R.id.toast_text)

                val messageText = when(message) {
                    is String -> message
                    is Int -> context.getString(message)
                    else -> ""
                }
                text.text = messageText

                // 화면 최하단으로부터 위로 40dp 떨어진 위치
                val yOffset = (40 * context.resources.displayMetrics.density).toInt()

                popupWindow?.dismiss() // 이전 팝업 즉시 제거
                popupWindow = PopupWindow(
                    layout,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    animationStyle = R.style.ToastAnimationStyle
                    isFocusable = false
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    // Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL로 중앙 하단에 표시
                    showAtLocation(context.findViewById(android.R.id.content), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffset)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    dismiss()
                }, 2000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismiss() {
        val currentPopup = popupWindow ?: return
        val context = currentPopup.contentView.context

        if (currentPopup.isShowing) {
            val slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
            slideOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    if (popupWindow == currentPopup) {
                        currentPopup.dismiss()
                        popupWindow = null
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            currentPopup.contentView.startAnimation(slideOut)
        }
    }
}
