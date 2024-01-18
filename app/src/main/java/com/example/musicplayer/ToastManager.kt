package com.example.musicplayer

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.musicplayer.Activity.MainActivity
import com.example.musicplayer.Activity.SplashActivity
import com.example.musicplayer.UtilManager.convertDpToPixel

class ToastManager(var context: Context?) : Toast(context) {
    private var currentToastLayout: FrameLayout? = null
    fun showAnimatedToast(msg: String?) {
        try {
            val activity = context as Activity?
            if (context != null && !activity!!.isFinishing) {
                removeAnimationToast()
                val inflater = LayoutInflater.from(context)
                val layout = inflater.inflate(R.layout.custom_toast, null)
                val text = layout.findViewById<TextView>(R.id.toast_text)
                text.text = msg

                // Toast처럼 보이게 하기 위해 FrameLayout 사용
                val frameLayout = FrameLayout(context as Activity)

                currentToastLayout = frameLayout
                frameLayout.addView(layout)

                // 애니메이션 설정
                val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
                val slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down)
                slideUp.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        frameLayout.postDelayed({ layout.startAnimation(slideDown) }, 2000)
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                slideDown.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        frameLayout.removeAllViews()
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                if (!activity.isFinishing) {
                    // 화면에 뷰 추가
                    val params = WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT)
                    val activityRootView = activity.findViewById<View>(android.R.id.content)
                    if (activityRootView != null) {
                        val displayMetrics = DisplayMetrics()
                        val keyboardRect = Rect()
                        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val window = activity.window
                        val activityRootView1 = window.decorView
                        activityRootView1.getWindowVisibleDisplayFrame(keyboardRect)
                        val screenHeight = activityRootView.rootView.height
                        activityRootView1.getWindowVisibleDisplayFrame(keyboardRect)
                        window.decorView.getWindowVisibleDisplayFrame(keyboardRect)
                        val keypadHeight1 = screenHeight - keyboardRect.bottom
                        if (keypadHeight1 > screenHeight * 0.15) {  // 키보드가 뜬 경우
                            val yOffset = Math.round(convertDpToPixel(85f))
                            params.y = screenHeight - (keypadHeight1 + yOffset) // 키보드 높이와 yOffset을 더한 값
                            params.gravity = Gravity.TOP
                        } else {  // 키보드가 내려간 경우
                            params.y = 0
                            params.gravity = Gravity.BOTTOM
                        }
                        val wm = (context as Activity).getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        wm?.addView(frameLayout, params)

                        // slide_up 애니메이션 시작
                        layout.startAnimation(slideUp)
                        if (activity is SplashActivity) {
                            activity.onBackPressedDispatcher.addCallback((activity as SplashActivity?)!!, object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() {
                                    removeAnimationToast() // 커스텀 토스트 해제
                                    activity.onBackPressed() // 액티비티 종료
                                }
                            })
                        } else if (activity is MainActivity) {
                            activity.onBackPressedDispatcher.addCallback((activity as MainActivity?)!!, object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() {
                                    removeAnimationToast() // 커스텀 토스트 해제
                                    activity.onBackPressed() // 액티비티 종료
                                }
                            })
                        }
                    }
                }
            }
        } catch (e: Exception) {
            removeAnimationToast()
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
