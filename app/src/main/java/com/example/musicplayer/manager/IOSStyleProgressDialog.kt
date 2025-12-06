package com.example.musicplayer.manager

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import com.example.musicplayer.databinding.DialogIosProgressBinding

class IOSStyleProgressDialog(context: Context) {
    private val dialog: Dialog = Dialog(context)
    private val binding: DialogIosProgressBinding = DialogIosProgressBinding.inflate(LayoutInflater.from(context))
    private var onCancelListener: (() -> Unit)? = null

    init {
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    fun show(message: String = "로딩 중...") {
        binding.tvMessage.text = message
        binding.progressView.startAnimating()
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    /**
     * 취소 가능한 다이얼로그 표시
     * @param message 표시할 메시지
     * @param onCancel 취소 시 호출되는 콜백
     */
    fun showCancelable(message: String = "로딩 중...", onCancel: () -> Unit) {
        onCancelListener = onCancel
        binding.tvMessage.text = message
        binding.progressView.startAnimating()

        dialog.setCancelable(true)
        dialog.setOnCancelListener {
            onCancelListener?.invoke()
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                onCancelListener?.invoke()
                dismiss()
                true
            } else {
                false
            }
        }

        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss() {
        binding.progressView.stopAnimating()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        // 취소 리스너 초기화
        dialog.setCancelable(false)
        dialog.setOnCancelListener(null)
        dialog.setOnKeyListener(null)
        onCancelListener = null
    }

    fun updateMessage(message: String) {
        binding.tvMessage.text = message
    }

    fun isShowing(): Boolean {
        return dialog.isShowing
    }
}
