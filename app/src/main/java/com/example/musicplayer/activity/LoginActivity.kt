package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.databinding.ActivityLoginBinding
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        startTime = System.currentTimeMillis()

        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - startTime < 500
        }

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기화
        ContextManager.setContext(this)
        AuthManager.init(this)
        GoogleAuthManager.init(this)
        userRepository = UserRepository(this)

        // 이미 로그인되어 있는지 확인
        checkExistingLogin()

        setupViews()
    }

    private fun checkExistingLogin() {
        if (AuthManager.isLoggedIn()) {
            val userId = AuthManager.getCurrentUserId()
            if (userId != null) {
                lifecycleScope.launch {
                    val user = userRepository.getUserById(userId)
                    if (user != null) {
                        // 이미 로그인된 사용자가 있으면 메인 화면으로 이동
                        userRepository.updateLastLogin(userId)
                        navigateToMain()
                        return@launch
                    }
                }
            }
        }
    }

    private fun setupViews() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGuestLogin.setOnClickListener {
            signInAsGuest()
        }
    }

    private fun signInWithGoogle() {
        showLoading(true)
        val signInIntent = GoogleAuthManager.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = GoogleAuthManager.handleSignInResult(task)
            if (account != null) {
                lifecycleScope.launch {
                    try {
                        // 구글 계정 정보로 사용자 생성 또는 업데이트
                        val existingUser = userRepository.getUserById(account.id ?: "")
                        val user = if (existingUser != null) {
                            // 기존 사용자 - 마지막 로그인 시간 업데이트
                            userRepository.updateLastLogin(existingUser.userId)
                            existingUser
                        } else {
                            // 새 사용자 생성
                            userRepository.createGoogleUser(
                                userId = account.id ?: "",
                                email = account.email,
                                displayName = account.displayName ?: "Unknown",
                                profileImageUrl = account.photoUrl?.toString()
                            )
                        }

                        // 현재 사용자로 저장
                        AuthManager.saveCurrentUser(user.userId)

                        ToastManager.showToast("${user.displayName}님 환영합니다!")
                        showLoading(false)
                        navigateToMain()
                    } catch (e: Exception) {
                        LogManager.e("Failed to save user: ${e.message}")
                        ToastManager.showToast("로그인 처리 중 오류가 발생했습니다")
                        showLoading(false)
                    }
                }
            } else {
                ToastManager.showToast("Google 로그인에 실패했습니다.\n게스트로 로그인해주세요.")
                showLoading(false)
            }
        } catch (e: Exception) {
            LogManager.e("Google sign-in failed: ${e.message}")
            ToastManager.showToast("로그인 오류가 발생했습니다.\n게스트로 로그인해주세요.")
            showLoading(false)
        }
    }

    private fun signInAsGuest() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val guestUser = userRepository.createGuestUser()
                AuthManager.saveCurrentUser(guestUser.userId)

                ToastManager.showToast("게스트로 로그인했습니다")
                showLoading(false)
                navigateToMain()
            } catch (e: Exception) {
                LogManager.e("Failed to create guest user: ${e.message}")
                ToastManager.showToast("게스트 로그인 실패: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnGoogleSignIn.isEnabled = !show
        binding.btnGuestLogin.isEnabled = !show
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
