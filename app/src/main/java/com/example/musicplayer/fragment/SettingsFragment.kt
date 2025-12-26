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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.activity.LoginActivity
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.manager.AudioEffectManager
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.manager.PitchShiftManager
import com.example.musicplayer.manager.ScoreFeedbackDialogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var userRepository: UserRepository
    private lateinit var sharedPrefs: android.content.SharedPreferences

    // Google Sign-In 런처
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // 순서 중요: 값을 먼저 설정하고 리스너를 나중에 설정
        loadSettings(view)
        setupViews(view)
        setupAccountSection(view)
    }

    override fun onResume() {
        super.onResume()
        // 화면 복귀 시 계정 상태 업데이트
        view?.let { setupAccountSection(it) }
    }

    private fun loadSettings(view: View) {
        // Dark mode - 초기값 설정
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).isChecked = isDarkMode

        // Notifications - 초기값 설정
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications", true)
        view.findViewById<SwitchCompat>(R.id.switchNotifications).isChecked = isNotificationsEnabled

        // Reverb - 초기값 설정
        val audioPrefs = requireContext().getSharedPreferences("audio_effect_settings", Context.MODE_PRIVATE)
        val isReverbEnabled = audioPrefs.getBoolean("reverb_enabled", false)
        view.findViewById<SwitchCompat>(R.id.switchReverb).isChecked = isReverbEnabled
    }

    private fun setupViews(view: View) {
        // Dark Mode Switch
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("dark_mode", isChecked) }
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                ToastManager.showToast("다크 모드가 켜졌습니다")
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                ToastManager.showToast("다크 모드가 꺼졌습니다")
            }
        }

        // Notifications Switch
        view.findViewById<SwitchCompat>(R.id.switchNotifications).setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("notifications", isChecked) }
            ToastManager.showToast(if (isChecked) "알림이 켜졌습니다" else "알림이 꺼졌습니다")
        }

        // Reverb Switch
        view.findViewById<SwitchCompat>(R.id.switchReverb).setOnCheckedChangeListener { _, isChecked ->
            val audioPrefs = requireContext().getSharedPreferences("audio_effect_settings", Context.MODE_PRIVATE)
            audioPrefs.edit { putBoolean("reverb_enabled", isChecked) }
            ToastManager.showToast(if (isChecked) "리버브가 켜졌습니다" else "리버브가 꺼졌습니다")
        }

        // Reverb Preset
        view.findViewById<TextView>(R.id.btnReverbPreset).setOnClickListener {
            showReverbPresetDialog()
        }

        // Default Key
        view.findViewById<TextView>(R.id.btnDefaultKey).setOnClickListener {
            showDefaultKeyDialog()
        }

        // Mic Test
        view.findViewById<TextView>(R.id.btnMicTest).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_mic_test)
        }

        // Achievements
        view.findViewById<TextView>(R.id.btnAchievements).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_achievements)
        }

        // Statistics
        view.findViewById<TextView>(R.id.btnStatistics).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_statistics)
        }

        // Default Difficulty
        view.findViewById<TextView>(R.id.btnDefaultDifficulty).setOnClickListener {
            showDifficultySelectionDialog()
        }

        // Clear Cache
        view.findViewById<TextView>(R.id.btnClearCache).setOnClickListener {
            showClearCacheDialog()
        }

        // Manage Recordings (Recording History)
        view.findViewById<TextView>(R.id.btnManageRecordings).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_recording_history)
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
        lifecycleScope.launch {
            val userId = AuthManager.getCurrentUserId()
            if (userId != null) {
                val userEntity = userRepository.getUserById(userId)
                if (userEntity != null && userEntity.loginType == LoginType.GOOGLE) {
                    GoogleAuthManager.signOut {
                        // Google 로그아웃 완료
                    }
                }
            }

            // 로그아웃 후 새 게스트 계정 생성
            AuthManager.logout()
            val guestUser = userRepository.createGuestUser()
            AuthManager.saveCurrentUser(guestUser.userId)

            ToastManager.showToast("로그아웃되었습니다")

            // UI 업데이트
            view?.let { setupAccountSection(it) }
        }
    }

    private fun setupAccountSection(view: View) {
        val layoutAccountInfo = view.findViewById<LinearLayout>(R.id.layoutAccountInfo)
        val btnLogin = view.findViewById<TextView>(R.id.btnLogin)
        val btnLogout = view.findViewById<TextView>(R.id.btnLogout)
        val dividerAccount = view.findViewById<View>(R.id.dividerAccount)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val ivProfileImage = view.findViewById<ImageView>(R.id.ivProfileImage)

        lifecycleScope.launch {
            val userId = AuthManager.getCurrentUserId()
            val user = userId?.let { userRepository.getUserById(it) }

            if (user != null && user.loginType == LoginType.GOOGLE) {
                // Google 로그인 상태 - 계정 정보와 로그아웃 표시
                layoutAccountInfo.isVisible = true
                btnLogin.isVisible = false
                dividerAccount.isVisible = true
                btnLogout.isVisible = true

                tvUserName.text = user.displayName
                tvUserEmail.text = user.email ?: ""

                // 프로필 이미지 로드
                user.profileImageUrl?.let { url ->
                    Glide.with(requireContext())
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(ivProfileImage)
                }
            } else {
                // 게스트 상태 - 로그인 버튼만 표시
                layoutAccountInfo.isVisible = false
                btnLogin.isVisible = true
                dividerAccount.isVisible = false
                btnLogout.isVisible = false
            }
        }

        // 로그인 버튼 클릭
        btnLogin.setOnClickListener {
            signInWithGoogle()
        }

        // 로그아웃 버튼 클릭
        btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = GoogleAuthManager.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = GoogleAuthManager.handleSignInResult(task)
            if (account != null) {
                lifecycleScope.launch {
                    try {
                        // Google 계정으로 사용자 생성 또는 업데이트
                        val existingUser = userRepository.getUserById(account.id ?: "")
                        val user = if (existingUser != null) {
                            userRepository.updateLastLogin(existingUser.userId)
                            existingUser
                        } else {
                            userRepository.createGoogleUser(
                                userId = account.id ?: "",
                                email = account.email,
                                displayName = account.displayName ?: "Unknown",
                                profileImageUrl = account.photoUrl?.toString()
                            )
                        }

                        // 현재 사용자로 저장
                        AuthManager.saveCurrentUser(user.userId)

                        ToastManager.showToast("${user.displayName}님으로 로그인했습니다")

                        // UI 업데이트
                        view?.let { setupAccountSection(it) }
                    } catch (e: Exception) {
                        ToastManager.showToast("로그인 처리 중 오류가 발생했습니다")
                    }
                }
            } else {
                ToastManager.showToast("Google 로그인에 실패했습니다")
            }
        } catch (e: Exception) {
            ToastManager.showToast("로그인 오류가 발생했습니다")
        }
    }

    private fun showReverbPresetDialog() {
        val presets = AudioEffectManager.Companion.ReverbPreset.entries.toTypedArray()
        val presetNames = presets.map { it.displayName }.toTypedArray()

        val audioPrefs = requireContext().getSharedPreferences("audio_effect_settings", Context.MODE_PRIVATE)
        val currentPresetIndex = audioPrefs.getInt("reverb_preset", 0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("리버브 프리셋")
            .setSingleChoiceItems(presetNames, currentPresetIndex) { dialog, which ->
                audioPrefs.edit { putInt("reverb_preset", which) }
                ToastManager.showToast("${presetNames[which]} 선택됨")
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDefaultKeyDialog() {
        val keyOptions = arrayOf(
            "-6 (낮음)",
            "-5",
            "-4",
            "-3",
            "-2",
            "-1",
            "0 (원키)",
            "+1",
            "+2",
            "+3",
            "+4",
            "+5",
            "+6 (높음)"
        )

        val currentKeyIndex = sharedPrefs.getInt("default_key", 6) // 0이 기본값, 인덱스 6

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("기본 키 설정")
            .setSingleChoiceItems(keyOptions, currentKeyIndex) { dialog, which ->
                sharedPrefs.edit { putInt("default_key", which) }
                val semitones = which - 6 // 인덱스를 반음 단위로 변환
                ToastManager.showToast("기본 키: ${PitchShiftManager.semitonesToKeyString(semitones)}")
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
