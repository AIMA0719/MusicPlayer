package com.example.musicplayer.Manager

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    private val PERMISSION_REQUEST_CODE = 123
    private var currentPermissionIndex = 0

    private val permissions: Array<String> = when {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,         // Android 13 이상: 사진 권한
                Manifest.permission.READ_MEDIA_VIDEO,          // Android 13 이상: 동영상 권한
                Manifest.permission.RECORD_AUDIO,              // 마이크 권한
                Manifest.permission.ACCESS_COARSE_LOCATION,    // 대략적 위치 권한
                Manifest.permission.ACCESS_FINE_LOCATION,      // 정밀 위치 권한
                Manifest.permission.CAMERA                     // 카메라 권한
            )
        }
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,     // Android 10~12: 외부 저장소 읽기 권한
                Manifest.permission.RECORD_AUDIO,              // 마이크 권한
                Manifest.permission.ACCESS_COARSE_LOCATION,    // 대략적 위치 권한
                Manifest.permission.ACCESS_FINE_LOCATION,      // 정밀 위치 권한
                Manifest.permission.CAMERA                     // 카메라 권한
            )
        }
        else -> {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,     // Android 9 이하: 외부 저장소 읽기 권한
                Manifest.permission.WRITE_EXTERNAL_STORAGE,    // Android 9 이하: 외부 저장소 쓰기 권한
                Manifest.permission.RECORD_AUDIO,              // 마이크 권한
                Manifest.permission.ACCESS_COARSE_LOCATION,    // 대략적 위치 권한
                Manifest.permission.ACCESS_FINE_LOCATION,      // 정밀 위치 권한
                Manifest.permission.CAMERA                     // 카메라 권한
            )
        }
    }

    fun checkPermission() {
        requestNextPermission()
    }

    private fun requestNextPermission() {
        if (currentPermissionIndex < permissions.size) {
            val permission = permissions[currentPermissionIndex]
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                // 이미 허가된 경우 다음 권한 요청
                currentPermissionIndex++
                requestNextPermission()
            } else {
                // 권한 요청
                ActivityCompat.requestPermissions(activity, arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 현재 권한이 허가된 경우 다음 권한 요청
                currentPermissionIndex++
                requestNextPermission()
            } else {

            }
        }
    }
}
