package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.activity.LoginActivity
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.ScoreFeedbackDialogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.UserRepository
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var sharedPrefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = UserRepository(requireContext())
        sharedPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // 순서 중요: 값을 먼저 설정하고 리스너를 나중에 설정
        loadSettings(view)
        setupViews(view)
    }

    private fun loadSettings(view: View) {
        // Dark mode - 초기값 설정
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).isChecked = isDarkMode

        // Notifications - 초기값 설정
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications", true)
        view.findViewById<SwitchCompat>(R.id.switchNotifications).isChecked = isNotificationsEnabled
    }

    private fun setupViews(view: View) {
        // Dark Mode Switch - App-level theme only
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("dark_mode", isChecked) }
            ToastManager.showToast("앱을 재시작하면 테마가 적용됩니다")
        }

        // Notifications Switch
        view.findViewById<SwitchCompat>(R.id.switchNotifications).setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("notifications", isChecked) }
            ToastManager.showToast(if (isChecked) "알림이 켜졌습니다" else "알림이 꺼졌습니다")
        }

        // Logout
        view.findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmDialog()
        }

        // Achievements
        view.findViewById<TextView>(R.id.btnAchievements).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_achievements)
        }

        // Default Difficulty
        view.findViewById<TextView>(R.id.btnDefaultDifficulty).setOnClickListener {
            showDifficultySelectionDialog()
        }

        // Clear Cache
        view.findViewById<TextView>(R.id.btnClearCache).setOnClickListener {
            showClearCacheDialog()
        }

        // Manage Recordings
        view.findViewById<TextView>(R.id.btnManageRecordings).setOnClickListener {
            ToastManager.showToast("녹음 파일 관리 기능은 준비 중입니다")
        }

        // Contact
        view.findViewById<TextView>(R.id.btnContact).setOnClickListener {
            sendEmailToSupport()
        }

        // Rate App
        view.findViewById<TextView>(R.id.btnRateApp).setOnClickListener {
            openPlayStore()
        }

        // Share App
        view.findViewById<TextView>(R.id.btnShareApp).setOnClickListener {
            shareApp()
        }

        // Terms of Service
        view.findViewById<TextView>(R.id.btnTermsOfService).setOnClickListener {
            showWebView("https://example.com/terms")
        }

        // Privacy Policy
        view.findViewById<TextView>(R.id.btnPrivacyPolicy).setOnClickListener {
            showWebView("https://example.com/privacy")
        }

        // Version Info
        view.findViewById<TextView>(R.id.btnVersionInfo).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_version)
        }

        // Library Info
        view.findViewById<TextView>(R.id.btnLibraryInfo).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_library)
        }
    }

    @SuppressLint("InflateParams")
    private fun showDifficultySelectionDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_difficulty_select, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        val currentDifficultyIndex = sharedPrefs.getInt("default_difficulty", 2)

        val difficultyViews = mapOf(
            0 to dialogView.findViewById(R.id.difficulty_very_easy),
            1 to dialogView.findViewById(R.id.difficulty_easy),
            2 to dialogView.findViewById(R.id.difficulty_normal),
            3 to dialogView.findViewById(R.id.difficulty_hard),
            4 to dialogView.findViewById<LinearLayout>(R.id.difficulty_very_hard)
        )

        val difficultyEnums = ScoreFeedbackDialogManager.ScoringDifficulty.entries.toTypedArray()

        fun updateSelection(selectedIndex: Int) {
            difficultyViews.forEach { (index, view) ->
                val titleTextView = view.getChildAt(0) as TextView
                val descriptionTextView = view.getChildAt(1) as TextView

                if (index == selectedIndex) {
                    view.setBackgroundResource(R.drawable.bg_button_primary)
                    titleTextView.setTextColor(android.graphics.Color.WHITE)
                    descriptionTextView.setTextColor("#E3F2FD".toColorInt())
                } else {
                    view.setBackgroundResource(R.drawable.bg_button_secondary)
                    titleTextView.setTextColor("#212121".toColorInt())
                    descriptionTextView.setTextColor("#757575".toColorInt())
                }
            }
        }

        updateSelection(currentDifficultyIndex)

        difficultyViews.forEach { (index, view) ->
            view.setOnClickListener {
                sharedPrefs.edit { putInt("default_difficulty", index) }
                val selectedDifficulty = difficultyEnums[index]
                ToastManager.showToast("${selectedDifficulty.displayName} 선택됨")
                dialog.dismiss()
            }
        }

        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 너비 설정: 화면 너비 - 좌우 각 10dp
        dialog.window?.let { window ->
            val displayMetrics = requireContext().resources.displayMetrics
            val marginDp = 10f
            val marginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginDp,
                displayMetrics
            ).toInt()

            val params = window.attributes
            params.width = displayMetrics.widthPixels - (marginPx * 2)
            window.attributes = params
        }
    }

    private fun showClearCacheDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("캐시 삭제")
            .setMessage("캐시를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                clearCache()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun clearCache() {
        try {
            val cacheDir = requireContext().cacheDir
            deleteDir(cacheDir)
            ToastManager.showToast("캐시가 삭제되었습니다")
        } catch (_: Exception) {
            ToastManager.showToast("캐시 삭제 실패")
        }
    }

    private fun deleteDir(dir: File?): Long {
        var size = 0L
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (child in children ?: emptyArray()) {
                val temp = File(dir, child)
                size += if (temp.isDirectory) {
                    deleteDir(temp)
                } else {
                    val fileSize = temp.length()
                    temp.delete()
                    fileSize
                }
            }
        }
        return size
    }

    private fun sendEmailToSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf("msizaplayer@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "[Music Player] 문의")
        }
        try {
            startActivity(Intent.createChooser(intent, "이메일 앱 선택"))
        } catch (_: Exception) {
            ToastManager.showToast("이메일 앱이 설치되어 있지 않습니다")
        }
    }

    private fun openPlayStore() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Music Player 앱을 사용해보세요!\nhttps://play.google.com/store/apps/details?id=${requireContext().packageName}")
        }
        startActivity(Intent.createChooser(shareIntent, "앱 공유"))
    }

    private fun showWebView(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (_: Exception) {
            ToastManager.showToast("웹 브라우저를 열 수 없습니다")
        }
    }

    private fun showLogoutConfirmDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Google 로그아웃
        lifecycleScope.launch {
            val userId = AuthManager.getCurrentUserId()
            if (userId != null) {
                val userEntity = userRepository.getUserById(userId)
                if (userEntity != null && userEntity.loginType == LoginType.GOOGLE) {
                    com.example.musicplayer.manager.GoogleAuthManager.signOut {
                        // Google 로그아웃 완료 후 처리
                    }
                }
            }
        }

        AuthManager.logout()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
