package com.example.musicplayer.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.Fragment.MusicListFragment
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.Manager.PermissionManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding
import com.example.musicplayer.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    lateinit var binding: MusicPlayerMainActivityBinding
    private lateinit var toastManager: ToastManager

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setBaseSetting()
        hideActionBar()
        observeViewModel()
        PermissionManager(this).checkPermission()

        // Fragment 변경 이벤트 처리
        binding.flMainLayout.setOnClickListener {
            viewModel.addFragment(MusicListFragment.newInstance(1))
        }
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

    override fun onBackPressed() {
        if (viewModel.isFragmentStackEmpty()) {
            if (viewModel.isDoubleBackToExit()) {
                super.onBackPressed()
                toastManager.removeAnimationToast()
                finish()
                return
            }
            viewModel.triggerDoubleBackToExit()
        } else {
            viewModel.popFragment()
        }
    }
}