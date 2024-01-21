package com.example.musicplayer.Manager

import android.Manifest
import android.content.Context
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission

class PermissionManager (context: Context) {
    var thisContext : Context = context

    var permissionlistener : PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            // 권한 허가시 실행 할 내용
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            // 권한 미 허가 시 실행 할 내용
        }
    }

    // 권한 체크하는 메소드
    fun checkPermission(){
        TedPermission.create()
            .setPermissionListener(permissionlistener)
            .setRationaleMessage("앱의 기능을 사용하기 위해서는 권한이 필요합니다.")
            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
            .setDeniedCloseButtonText("닫기")
            .setGotoSettingButtonText("설정")
            .setRationaleTitle("HELLO")
            .setPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.CALL_PHONE)
            .check()
    }

}