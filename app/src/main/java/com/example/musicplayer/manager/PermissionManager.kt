package com.example.musicplayer.manager

import android.Manifest
import android.app.Activity
import android.content.Context
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

    companion object {
        private const val PREF_NAME = "permission_prefs"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }

    private val prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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

    fun areAllPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 앱 최초 실행 시 권한 요청 (설명 다이얼로그 포함)
     */
    fun requestPermissionsOnFirstLaunch() {
        if (areAllPermissionsGranted()) {
            onAllPermissionsGranted()
            return
        }

        val hasRequestedBefore = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)

        if (!hasRequestedBefore) {
            // 최초 실행 시 설명 다이얼로그 표시
            showPermissionExplanationDialog()
        } else {
            // 이전에 요청했으면 바로 권한 요청
            checkAndRequestPermissions()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(activity)
            .setTitle("앱 권한 안내")
            .setMessage(
                "노래방 앱을 사용하기 위해 다음 권한이 필요합니다:\n\n" +
                "• 마이크: 노래 녹음 및 음성 분석\n" +
                "• 저장소/미디어: 음악 파일 접근 및 녹음 저장\n\n" +
                "권한을 허용해 주세요."
            )
            .setPositiveButton("확인") { _, _ ->
                prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
                requestPermissions()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestPermissions() {
        val ungranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
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

        // 권한이 영구 거부되었는지 확인 (이전에 요청한 적 있어야 함)
        val hasRequestedBefore = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        val permanentlyDenied = hasRequestedBefore && ungranted.any {
            !activity.shouldShowRequestPermissionRationale(it)
        }

        if (permanentlyDenied) {
            showPermissionDeniedDialog()
        } else if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    fun handlePermissionResult(result: Map<String, Boolean>) {
        val denied = result.filterValues { !it }.keys
        if (denied.isEmpty()) {
            onAllPermissionsGranted()
        } else {
            // 일부 권한이 거부됨
            val permanentlyDenied = denied.any {
                !activity.shouldShowRequestPermissionRationale(it)
            }

            if (permanentlyDenied) {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("권한 필요")
            .setMessage(
                "앱의 주요 기능을 사용하려면 권한이 필요합니다.\n\n" +
                "설정에서 권한을 허용해주세요."
            )
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            }
            .setNegativeButton("나중에", null)
            .show()
    }
}
