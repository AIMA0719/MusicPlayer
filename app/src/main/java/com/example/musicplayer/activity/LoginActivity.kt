package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.databinding.ActivityLoginBinding
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.manager.LogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 로그인 액티비티 - 설정에서 로그인 필요 시 호출됨
 * 더 이상 런처 액티비티가 아님
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var userRepository: UserRepository

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기화
        AuthManager.init(this)
        GoogleAuthManager.init(this)

        setupViews()
    }

    private fun setupViews() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGuestLogin.setOnClickListener {
            // 게스트 버튼은 더 이상 필요 없지만, 이미 게스트이면 그냥 종료
            finish()
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

                        AuthManager.saveCurrentUser(user.userId)

                        ToastManager.showToast("${user.displayName}님으로 로그인했습니다")
                        showLoading(false)

                        // 로그인 성공 시 결과 설정하고 종료
                        setResult(RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        LogManager.e("Failed to save user: ${e.message}")
                        ToastManager.showToast("로그인 처리 중 오류가 발생했습니다")
                        showLoading(false)
                    }
                }
            } else {
                ToastManager.showToast("Google 로그인에 실패했습니다")
                showLoading(false)
            }
        } catch (e: Exception) {
            LogManager.e("Google sign-in failed: ${e.message}")
            ToastManager.showToast("로그인 오류가 발생했습니다")
            showLoading(false)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnGoogleSignIn.isEnabled = !show
        binding.btnGuestLogin.isEnabled = !show
    }
}
