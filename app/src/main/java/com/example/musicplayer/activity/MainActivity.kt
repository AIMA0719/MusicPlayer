package com.example.musicplayer.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.fragment.MainFragment
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.manager.PermissionManager
import com.example.musicplayer.manager.ProgressDialogManager
import com.example.musicplayer.manager.ScoreDialogManager
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding
import com.example.musicplayer.viewmodel.MainActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MusicPlayerMainActivityBinding
    lateinit var viewModel: MainActivityViewModel

    private lateinit var permissionManager: PermissionManager

    private var startTime: Long = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionManager.handlePermissionResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        startTime = System.currentTimeMillis()

        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - startTime < 500
        }

        super.onCreate(savedInstanceState)

        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater) // 객체 바인딩
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java] // 메인 뷰 모델

        setContentView(binding.root) // 화면 세팅
        setBaseSetting() // 메인 context 및 메인 activity 캐싱
        hideActionBar() // 액션 바 숨기기
        observeViewModel() // 화면 이동 및 토스트 메시지 처리
        setOnBackPressed() // 페이지 별 뒤로 가기 처리
        setMainFragment() // 메인 화면 설정

        permissionManager = PermissionManager(
            activity = this,
            permissionLauncher = permissionLauncher,
            onAllPermissionsGranted = {
                // 모든 권한 허용된 경우의 처리
            }
        )
    }

    override fun onResume() {
        super.onResume()
        permissionManager.checkAndRequestPermissions()
    }

    private fun setMainFragment() {
        FragmentMoveManager.instance.pushFragment(MainFragment.newInstance())
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isFragmentStackEmpty()) {
                    if (viewModel.isDoubleBackToExit()) {
                        ToastManager.closeToast()

                        finishAffinity() // 모든 액티비티 종료
                        android.os.Process.killProcess(android.os.Process.myPid()) // 현재 프로세스 종료
                        exitProcess(0) // 안전하게 JVM 종료
                    } else {
                        viewModel.triggerDoubleBackToExit()
                    }
                } else {
                    FragmentMoveManager.instance.popFragment()
                }
            }
        })
    }

    private fun setBaseSetting() {
        ContextManager.mainContext = this
        ContextManager.mainActivity = this
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun observeViewModel() {
        // 현재 Fragment 상태 변경 시 UI 업데이트
        /*viewModel.currentFragment.observe(this) { fragmentTag ->
            binding.tvStatusBarTitle.text = fragmentTag
        }*/

        // Toast 메시지 처리
        viewModel.toastMessage.observe(this) { message ->
            ToastManager.show(message)
        }
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onDestroy() {
        super.onDestroy()
        ProgressDialogManager.dismiss()
        ScoreDialogManager.dismiss()
        scope.cancel()
        job.cancel()
    }
}