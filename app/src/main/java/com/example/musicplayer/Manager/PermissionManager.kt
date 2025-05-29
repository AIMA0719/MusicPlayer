package com.example.musicplayer.Manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: Activity,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val onAllPermissionsGranted: () -> Unit
) {

    private val requiredPermissions: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.RECORD_AUDIO
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private fun areAllPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkAndRequestPermissions() {
        if (areAllPermissionsGranted()) {
            onAllPermissionsGranted()
            return
        }

        val ungranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        val permanentlyDenied = ungranted.any {
            !activity.shouldShowRequestPermissionRationale(it) &&
                    ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_DENIED
        }

        if (permanentlyDenied) {
            showPermissionDeniedDialog()
        } else {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    fun handlePermissionResult(result: Map<String, Boolean>) {
        val denied = result.filterValues { !it }.keys
        if (denied.isEmpty()) {
            onAllPermissionsGranted()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(activity)
            .setTitle("권한 필요")
            .setMessage("이 앱의 기능을 사용하려면 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            }
            .setNegativeButton("취소", null)

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.post {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                ContextCompat.getColor(activity, android.R.color.black))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(activity, android.R.color.black))
        }
    }
}
