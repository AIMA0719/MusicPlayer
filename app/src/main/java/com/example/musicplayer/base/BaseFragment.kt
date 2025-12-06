package com.example.musicplayer.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.musicplayer.manager.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Fragment 기본 클래스
 * - ViewBinding 자동 처리
 * - 생명주기 안전한 Flow 수집
 * - 공통 유틸리티 메서드 제공
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 뷰 초기화 (클릭 리스너 등)
     */
    protected open fun setupViews() {}

    /**
     * 데이터 관찰 (ViewModel의 StateFlow/LiveData)
     */
    protected open fun observeData() {}

    /**
     * 현재 로그인한 사용자 ID 가져오기
     */
    protected fun getCurrentUserId(): String {
        return AuthManager.getCurrentUserId() ?: "guest"
    }

    /**
     * 생명주기 안전한 Flow 수집
     */
    protected fun <T> Flow<T>.collectWithLifecycle(
        state: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (T) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state) {
                collect(action)
            }
        }
    }

    /**
     * 생명주기 안전한 코루틴 실행
     */
    protected fun launchWithLifecycle(
        state: Lifecycle.State = Lifecycle.State.STARTED,
        block: suspend () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state) {
                block()
            }
        }
    }
}
