package com.example.musicplayer.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.musicplayer.R
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.manager.PermissionManager
import com.example.musicplayer.manager.ProgressDialogManager
import com.example.musicplayer.manager.ScoreDialogManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.viewModel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MusicPlayerMainActivityBinding
    lateinit var viewModel: MainActivityViewModel
    private lateinit var navController: NavController
    private lateinit var permissionManager: PermissionManager

    private var startTime: Long = 0

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
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

        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        setContentView(binding.root)
        setBaseSetting()
        hideActionBar()
        setupNavigation()
        observeViewModel()
        setOnBackPressed()

        permissionManager = PermissionManager(
            activity = this,
            permissionLauncher = permissionLauncher,
            onAllPermissionsGranted = {
                // 모든 권한 허용된 경우의 처리
            }
        )
    }

    private fun setupNavigation() {
        // NavHostFragment 가져오기
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // BottomNavigationView와 NavController 연결
        binding.bottomNavigation.setupWithNavController(navController)

        // 하단 네비게이션 아이템 선택 리스너 (백스택 초기화용)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_music_list -> {
                    // 음악 탭 클릭 시 백스택 모두 제거하고 MusicListFragment로 이동
                    if (navController.currentDestination?.id != R.id.navigation_music_list) {
                        navController.navigate(R.id.navigation_music_list, null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.navigation_music_list, true)
                                .build()
                        )
                    }
                    true
                }
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.navigation_home) {
                        navController.navigate(R.id.navigation_home)
                    }
                    true
                }
                R.id.navigation_search -> {
                    if (navController.currentDestination?.id != R.id.navigation_search) {
                        navController.navigate(R.id.navigation_search)
                    }
                    true
                }
                R.id.navigation_record -> {
                    if (navController.currentDestination?.id != R.id.navigation_record) {
                        navController.navigate(R.id.navigation_record)
                    }
                    true
                }
                R.id.navigation_settings -> {
                    if (navController.currentDestination?.id != R.id.navigation_settings) {
                        navController.navigate(R.id.navigation_settings)
                    }
                    true
                }
                else -> false
            }
        }

        // Destination 변경 리스너 설정
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val title = when (destination.id) {
                R.id.navigation_home -> "홈"
                R.id.navigation_search -> "검색"
                R.id.navigation_record -> "녹음"
                R.id.navigation_music_list -> "음악"
                R.id.navigation_settings -> "설정"
                R.id.recordingFragment -> "피치 매칭 녹음"
                R.id.achievementsFragment -> "도전과제"
                R.id.versionInfoFragment -> "버전 정보"
                R.id.libraryInfoFragment -> "오픈소스 라이선스"
                else -> ""
            }

            binding.tvStatusBarTitle.text = title
            viewModel._currentFragment.value = destination.label?.toString() ?: ""

            // 하단 네비게이션에 포함되지 않은 화면에서는 뒤로가기 버튼 표시
            val isTopLevelDestination = destination.id in setOf(
                R.id.navigation_home,
                R.id.navigation_search,
                R.id.navigation_record,
                R.id.navigation_music_list,
                R.id.navigation_settings
            )

            if (isTopLevelDestination) {
                binding.ivBackButton.visibility = android.view.View.GONE
            } else {
                binding.ivBackButton.visibility = android.view.View.VISIBLE
                binding.ivBackButton.setOnClickListener {
                    navController.navigateUp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionManager.checkAndRequestPermissions()
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 최상위 레벨 화면인지 확인
                val currentDestination = navController.currentDestination?.id
                val isTopLevelDestination = currentDestination in setOf(
                    R.id.navigation_home,
                    R.id.navigation_search,
                    R.id.navigation_record,
                    R.id.navigation_music_list,
                    R.id.navigation_settings
                )

                if (isTopLevelDestination) {
                    // 모든 최상위 화면에서 뒤로가기: 앱 종료 확인
                    if (viewModel.isDoubleBackToExit()) {
                        finish()
                    } else {
                        viewModel.triggerDoubleBackToExit()
                        ToastManager.showToast("앱을 종료하려면 다시 한 번 눌러주세요")
                    }
                } else {
                    // 하위 화면에서는 뒤로가기
                    navController.navigateUp()
                }
            }
        })
    }

    private fun setBaseSetting() {
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun observeViewModel() {
        // Toast 메시지 처리
        viewModel.toastMessage.observe(this) { message ->
            ToastManager.showToast(message)
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
