package com.example.musicplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.musicplayer.R
import com.example.musicplayer.manager.AuthManager
import com.example.musicplayer.manager.ContextManager
import com.example.musicplayer.manager.GoogleAuthManager
import com.example.musicplayer.manager.PermissionManager
import com.example.musicplayer.manager.ProgressDialogManager
import com.example.musicplayer.manager.ScoreDialogManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding
import com.example.musicplayer.manager.ToastManager
import com.example.musicplayer.repository.UserRepository
import com.example.musicplayer.viewModel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MusicPlayerMainActivityBinding
    lateinit var viewModel: MainActivityViewModel
    private lateinit var navController: NavController
    private lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var userRepository: UserRepository

    private var startTime: Long = 0
    private var isInitialized = false

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionManager.handlePermissionResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 다크모드 설정 적용 (스플래시 전에 적용해야 깜빡임 방지)
        applyDarkModeSetting()

        val splashScreen = installSplashScreen()

        startTime = System.currentTimeMillis()

        // 스플래시 화면을 초기화 완료까지 유지
        splashScreen.setKeepOnScreenCondition {
            !isInitialized || System.currentTimeMillis() - startTime < 500
        }

        super.onCreate(savedInstanceState)

        // 인증 매니저 초기화
        AuthManager.init(this)
        GoogleAuthManager.init(this)

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

        // 자동 게스트 로그인 처리
        ensureUserLoggedIn()

        // 앱 시작 시 권한 요청 (최초 실행 시 설명 다이얼로그 포함)
        permissionManager.requestPermissionsOnFirstLaunch()
    }

    private fun ensureUserLoggedIn() {
        lifecycleScope.launch {
            if (!AuthManager.isLoggedIn()) {
                // 로그인되어 있지 않으면 자동으로 게스트 생성
                val guestUser = userRepository.createGuestUser()
                AuthManager.saveCurrentUser(guestUser.userId)
            } else {
                // 기존 로그인 사용자 확인
                val userId = AuthManager.getCurrentUserId()
                if (userId != null) {
                    val user = userRepository.getUserById(userId)
                    if (user == null) {
                        // 사용자 데이터가 없으면 새 게스트 생성
                        val guestUser = userRepository.createGuestUser()
                        AuthManager.saveCurrentUser(guestUser.userId)
                    }
                }
            }
            isInitialized = true
        }
    }

    /**
     * 탭 메뉴 아이템 ID를 네비게이션 destination ID로 변환
     */
    private fun getDestinationId(itemId: Int): Int? = when (itemId) {
        R.id.navigation_home -> R.id.navigation_home
        R.id.navigation_karaoke -> R.id.navigation_karaoke
        R.id.navigation_myroom -> R.id.navigation_myroom
        R.id.navigation_settings -> R.id.navigation_settings
        else -> null
    }

    /**
     * 화면 ID를 해당하는 탭 메뉴 아이템 ID로 변환
     */
    private fun getTabItemId(destinationId: Int): Int? = when (destinationId) {
        R.id.navigation_home -> R.id.navigation_home
        R.id.navigation_karaoke, R.id.recordingFragment -> R.id.navigation_karaoke
        R.id.navigation_myroom, R.id.recordingHistoryFragment -> R.id.navigation_myroom
        R.id.navigation_settings, R.id.achievementsFragment,
        R.id.versionInfoFragment, R.id.libraryInfoFragment,
        R.id.statisticsFragment, R.id.micTestFragment -> R.id.navigation_settings
        else -> null
    }

    private fun setupNavigation() {
        // NavHostFragment 가져오기
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 하단 네비게이션 아이템 선택 시 - 백스택 초기화 후 해당 탭으로 이동
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val destinationId = getDestinationId(item.itemId)
                ?: return@setOnItemSelectedListener false

            // 현재 위치가 이미 해당 탭이면 백스택만 정리
            if (navController.currentDestination?.id == destinationId) {
                navController.popBackStack(destinationId, inclusive = false)
            } else {
                // 다른 탭으로 이동: 백스택 정리 후 navigate
                navController.popBackStack(R.id.navigation_home, inclusive = false)
                if (destinationId != R.id.navigation_home) {
                    navController.navigate(destinationId)
                }
            }
            true
        }

        // 하단 네비게이션 아이템 재선택 시 - 해당 탭의 루트로 이동
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            val destinationId = getDestinationId(item.itemId) ?: return@setOnItemReselectedListener
            navController.popBackStack(destinationId, inclusive = false)
        }

        // Destination 변경 리스너 설정
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val title = when (destination.id) {
                R.id.navigation_home -> "홈"
                R.id.navigation_karaoke -> "노래방"
                R.id.navigation_myroom -> "마이룸"
                R.id.navigation_settings -> "설정"
                R.id.recordingFragment -> "노래방"
                R.id.musicPlayerFragment -> "음악 재생"
                R.id.achievementsFragment -> "도전과제"
                R.id.versionInfoFragment -> "버전 정보"
                R.id.libraryInfoFragment -> "오픈소스 라이선스"
                R.id.recordingHistoryFragment -> "녹음 기록"
                R.id.statisticsFragment -> "통계"
                else -> ""
            }

            binding.tvStatusBarTitle.text = title
            viewModel._currentFragment.value = destination.label?.toString() ?: ""

            // 하단 네비게이션에 포함되지 않은 화면에서는 뒤로가기 버튼 표시
            val isTopLevelDestination = destination.id in setOf(
                R.id.navigation_home,
                R.id.navigation_karaoke,
                R.id.navigation_myroom,
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

            // BottomNavigation 선택 상태 동기화 (하위 화면에서도 올바른 탭 선택 유지)
            getTabItemId(destination.id)?.let { tabItemId ->
                if (binding.bottomNavigation.selectedItemId != tabItemId) {
                    binding.bottomNavigation.menu.findItem(tabItemId)?.isChecked = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정에서 돌아왔을 때 권한 상태 확인
        if (permissionManager.areAllPermissionsGranted()) {
            // 권한이 모두 허용됨
        }
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 최상위 레벨 화면인지 확인
                val currentDestination = navController.currentDestination?.id
                val isTopLevelDestination = currentDestination in setOf(
                    R.id.navigation_home,
                    R.id.navigation_karaoke,
                    R.id.navigation_myroom,
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

    private fun applyDarkModeSetting() {
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        ProgressDialogManager.dismiss()
        ScoreDialogManager.dismiss()
    }
}
