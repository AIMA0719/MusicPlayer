package com.example.musicplayer.manager

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import com.example.musicplayer.R

object ProgressDialogManager {
    private var dialog: AlertDialog? = null

    fun show(context: Context) {
        if (dialog == null) {
            val builder = AlertDialog.Builder(context)
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_progress, null)

            builder.setView(view)
            builder.setCancelable(false) // 뒤로 가기 버튼 방지
            dialog = builder.create()
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.show()
    }

    fun dismiss() {
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
        dialog = null
    }
}
