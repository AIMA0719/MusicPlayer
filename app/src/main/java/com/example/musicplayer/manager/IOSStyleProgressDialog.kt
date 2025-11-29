package com.example.musicplayer.manager

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.example.musicplayer.databinding.DialogIosProgressBinding
import com.example.musicplayer.view.IOSStyleProgressView

class IOSStyleProgressDialog(context: Context) {
    private val dialog: Dialog = Dialog(context)
    private val binding: DialogIosProgressBinding

    init {
        binding = DialogIosProgressBinding.inflate(LayoutInflater.from(context))
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

    fun dismiss() {
        binding.progressView.stopAnimating()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun updateMessage(message: String) {
        binding.tvMessage.text = message
    }

    fun isShowing(): Boolean {
        return dialog.isShowing
    }
}
