package com.example.musicplayer.Manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission

class PermissionManager(private val activity: Activity) {

    private val PERMISSION_REQUEST_CODE = 123

    private var permissionlistener : PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            // 권한 허가시 실행 할 내용
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            // 권한 미 허가 시 실행 할 내용
        }
    }

    // 권한 체크하는 메소드
    fun checkPermission(){
        if (checkPermissions()) {
            permissionlistener.onPermissionGranted()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )

        ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
    }
}
