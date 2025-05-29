package com.example.musicplayer.manager

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.example.musicplayer.R

object ScoreDialogManager {
    private var dialog: Dialog? = null

    @SuppressLint("InflateParams")
    fun show(context: Context, score: Int) {
        dismiss()

        // 다이얼로그 생성 및 설정
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE) // 타이틀 제거
            setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_score, null))
            setCancelable(true) // 다이얼로그 바깥을 터치해서 dismiss
        }

        val scoreTextView: TextView = dialog!!.findViewById(R.id.tv_dialog_score)
        val messageTextView: TextView = dialog!!.findViewById(R.id.tv_dialog_message)

        // 점수에 따른 메시지 설정
        val message = when {
            score >= 90 -> "훌륭합니다! 거진 가수네유."
            score >= 80 -> "좋아요! 가수 도전 해볼 실력이네유"
            score >= 70 -> "Shit! 좀 더 연습하세유"
            else -> "안 부른 수준입니다!!!"
        }
        messageTextView.text = message

        // 점수 카운트 업 애니메이션
        var displayedScore = 0
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                if (displayedScore <= score) {
                    scoreTextView.text = "$displayedScore 점"
                    displayedScore++
                    handler.postDelayed(this, 20) // 20ms 간격으로 증가
                } else {
                    handler.removeCallbacks(this)
                }
            }
        }
        handler.post(runnable)

        dialog!!.show()
    }

    /**
     * 다이얼로그 닫기 함수
     */
    fun dismiss() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
        dialog = null
    }
}
