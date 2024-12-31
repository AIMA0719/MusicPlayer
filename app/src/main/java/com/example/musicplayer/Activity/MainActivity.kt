package com.example.musicplayer.Activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.Fragment.MusicListFragment
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.Manager.PermissionManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding
import com.example.musicplayer.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MusicPlayerMainActivityBinding
    private lateinit var toastManager: ToastManager

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater) // 객체 바인딩
        viewModel = ViewModelProvider(this)[MainViewModel::class.java] // 메인 뷰 모델

        setContentView(binding.root) // 화면 세팅
        setBaseSetting() // 메인 context 및 메인 activity 캐싱
        hideActionBar() // 액션 바 숨기기
        observeViewModel() // 화면 이동 및 토스트 메시지 처리
        PermissionManager(this).checkPermission() // 앱 실행 시 권한 요청
        setOnBackPressed() // 페이지 별 뒤로 가기 처리
        setUserInterFace() // 버튼 클릭 리스너 세팅
    }

    private fun setUserInterFace() {
        // todo 일단 화면 클릭 하면 뮤직 리스트로 이동
        binding.flMainLayout.setOnClickListener {
            viewModel.addFragment(MusicListFragment.newInstance())
        }
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isFragmentStackEmpty()) {
                    if (viewModel.isDoubleBackToExit()) {
                        toastManager.removeAnimationToast()
                        finish()
                    } else {
                        viewModel.triggerDoubleBackToExit()
                    }
                } else {
                    viewModel.popFragment()
                }
            }
        })
    }

    private fun setBaseSetting() {
        ContextManager.mainContext = this
        ContextManager.mainActivity = this
        toastManager = ToastManager(this)
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun observeViewModel() {
        // 현재 Fragment 상태 변경 시 UI 업데이트
        viewModel.currentFragment.observe(this) { fragmentTag ->
            binding.tvStatusBarTitle.text = fragmentTag
        }

        // Toast 메시지 처리
        viewModel.toastMessage.observe(this) { message ->
            toastManager.showAnimatedToast(message)
        }
    }
}