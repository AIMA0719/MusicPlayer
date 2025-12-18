package com.example.musicplayer.core.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.example.musicplayer.R

/**
 * iOS 스타일의 프로그레스 다이얼로그
 */
class ProgressDialog(context: Context) {

    private val dialog: Dialog = Dialog(context)
    private var messageTextView: TextView? = null

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            LayoutInflater.from(context).inflate(R.layout.dialog_ios_progress, null)
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        messageTextView = dialog.findViewById(R.id.tvMessage)
    }

    fun show(message: String = "로딩 중...") {
        messageTextView?.text = message
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun showCancelable(message: String = "로딩 중...", onCancel: () -> Unit) {
        messageTextView?.text = message
        dialog.setCancelable(true)
        dialog.setOnCancelListener { onCancel() }
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun updateMessage(message: String) {
        messageTextView?.text = message
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean = dialog.isShowing
}
