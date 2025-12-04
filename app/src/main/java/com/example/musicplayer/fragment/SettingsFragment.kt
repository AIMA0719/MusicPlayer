package com.example.musicplayer.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.activity.LoginActivity
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.repository.UserRepository
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var sharedPrefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            Toast.makeText(requireContext(), "앱을 재시작하면 테마가 적용됩니다", Toast.LENGTH_SHORT).show()
        }

        // Notifications Switch
        view.findViewById<SwitchCompat>(R.id.switchNotifications).setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications", isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "알림이 켜졌습니다" else "알림이 꺼졌습니다", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "녹음 파일 관리 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
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
            showWebView("https://example.com/terms", "이용약관")
        }

        // Privacy Policy
        view.findViewById<TextView>(R.id.btnPrivacyPolicy).setOnClickListener {
            showWebView("https://example.com/privacy", "개인정보 처리방침")
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

    private fun showDifficultySelectionDialog() {
        val difficulties = arrayOf("피스 오브 케익 모드", "이지 모드", "노말 모드 (권장)", "고수 모드", "초고수 모드")
        val currentDifficulty = sharedPrefs.getInt("default_difficulty", 2)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("기본 채점 난이도")
            .setSingleChoiceItems(difficulties, currentDifficulty) { dialog, which ->
                sharedPrefs.edit().putInt("default_difficulty", which).apply()
                Toast.makeText(requireContext(), "${difficulties[which]} 선택됨", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
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
            val size = deleteDir(cacheDir)
            Toast.makeText(requireContext(), "캐시가 삭제되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "캐시 삭제 실패", Toast.LENGTH_SHORT).show()
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
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "[Music Player] 문의")
        }
        try {
            startActivity(Intent.createChooser(intent, "이메일 앱 선택"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "이메일 앱이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
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

    private fun showWebView(url: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "웹 브라우저를 열 수 없습니다", Toast.LENGTH_SHORT).show()
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
