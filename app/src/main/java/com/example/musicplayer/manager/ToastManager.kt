package com.example.musicplayer.manager

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.example.musicplayer.R

object ToastManager {

    private var popupWindow: PopupWindow? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun showToast(message: Any) {
        show(message)
    }

    @JvmStatic
    fun closeToast() {
        dismiss()
    }

    @SuppressLint("InflateParams")
    fun show(message: Any) {
        try {
            val activity = ContextManager.getActivity() ?: return
            if (activity.isFinishing || activity.isDestroyed) return

            mainHandler.post {
                val inflater = LayoutInflater.from(activity)
                val layout = inflater.inflate(R.layout.custom_toast, null)
                val text: TextView = layout.findViewById(R.id.toast_text)

                val messageText = when (message) {
                    is String -> message
                    is Int -> activity.getString(message)
                    else -> ""
                }
                text.text = messageText

                // 팝업 크기 측정
                layout.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val popupWidth = layout.measuredWidth
                val popupHeight = layout.measuredHeight

                val metrics = activity.resources.displayMetrics
                val screenWidth = metrics.widthPixels
                val screenHeight = metrics.heightPixels
                val density = metrics.density

                // 가로 중앙 정렬
                val x = (screenWidth - popupWidth) / 2

                // "디바이스 최하단에서 위로 40dp" 위치
                val bottomMargin = (30f * density).toInt()
                val y = screenHeight - popupHeight - bottomMargin

                // 이전 팝업 제거
                popupWindow?.dismiss()

                popupWindow = PopupWindow(
                    layout,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    false
                ).apply {
                    isFocusable = false
                    isTouchable = false
                    isOutsideTouchable = false

                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                    // 절대 좌표 지정 (TOP|START 기준)
                    showAtLocation(
                        activity.window.decorView,
                        Gravity.TOP or Gravity.START,
                        x,
                        y
                    )
                }

                // 나타날 때 애니메이션 (항상 같은 위치에서 시작)
                val slideIn = AnimationUtils.loadAnimation(activity, R.anim.slide_in_bottom)
                layout.startAnimation(slideIn)

                // 2초 후 닫기
                mainHandler.postDelayed({
                    dismiss()
                }, 2000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        val currentPopup = popupWindow ?: return
        val context = currentPopup.contentView.context

        if (!currentPopup.isShowing) {
            popupWindow = null
            return
        }

        // 사라질 때 애니메이션도 같은 뷰에 적용
        val slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
        slideOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // 애니메이션 끝난 뒤 실제 dismiss
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
