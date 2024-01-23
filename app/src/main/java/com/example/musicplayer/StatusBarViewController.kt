package com.example.musicplayer

import android.view.View
import com.example.musicplayer.Activity.MainActivity

class StatusBarViewController(private val mainActivity: MainActivity) {
    fun setStatusBarView(currentFragment: String?) {
        // todo 페이지 별 서브 기능 아이콘 세팅, Status bar 컬러 세팅

        when (currentFragment) {
            "MainFragment" -> {
                mainActivity.binding?.apply {
                    llStatusBarRight.visibility = View.VISIBLE
                    llStatusBarMiddle.visibility = View.VISIBLE
                }
            }

            else -> {
                mainActivity.binding?.apply {
                    llStatusBarRight.visibility = View.GONE
                    llStatusBarMiddle.visibility = View.GONE
                }
            }
        }
    }
}
