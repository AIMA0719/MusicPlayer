package com.example.musicplayer.Manager

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.example.musicplayer.R

class ToastManager(var context: Context?) : Toast(context) {
    private var currentToastLayout: FrameLayout? = null

    fun showAnimatedToast(message: String) {
        try {
            if (context is Activity) {
                val activity = context as Activity

                if (!activity.isFinishing) {
                    // 토스트 레이아웃을 생성
                    val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null)

                    // 텍스트 설정
                    val text: TextView = layout.findViewById(R.id.toast_text)
                    text.text = message

                    // 팝업 윈도우 생성
                    val popupWindow = PopupWindow(
                            layout,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    // 투명한 배경 설정
                    popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    popupWindow.isOutsideTouchable = true
                    popupWindow.showAtLocation(activity.window.decorView, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)

                    // 아래에서 위로 올라오는 애니메이션 적용
                    val toastView = popupWindow.contentView
                    toastView?.translationY = 500f

                    toastView?.animate()
                            ?.translationYBy(-500f)
                            ?.withEndAction {
                                // 3초 후에 추가적인 애니메이션 적용
                                Handler(Looper.getMainLooper()).postDelayed({
                                    // 애니메이션이 완료된 후 팝업 닫기
                                    toastView.animate()
                                            ?.translationY(500f)
                                            ?.withEndAction { popupWindow.dismiss() }
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

    fun removeAnimationToast() {
        try {
            if (currentToastLayout?.parent != null) {
                val wm = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
                wm?.removeView(currentToastLayout)
                currentToastLayout = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
