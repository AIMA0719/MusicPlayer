package com.example.musicplayer.manager

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.musicplayer.R
import java.util.Stack

class FragmentMoveManager private constructor() {

    private val fragmentStack: Stack<Fragment> = Stack()

    companion object {
        val instance: FragmentMoveManager by lazy { FragmentMoveManager() }
    }

    private fun getFragmentManager(): FragmentManager? {
        return ContextManager.mainActivity?.supportFragmentManager
    }

    // Fragment를 추가하고 스택에 push
    fun pushFragment(fragment: Fragment) {
        val fragmentManager = getFragmentManager()

        // 중복된 프래그먼트 연속 추가 방지
        if (fragmentStack.isNotEmpty() && fragmentStack.peek().javaClass == fragment.javaClass) {
            return // 이전과 동일한 프래그먼트라면 추가하지 않음
        }

        if (fragmentManager != null && ContextManager.mainActivity?.findViewById<View>(R.id.fl_main_layout) != null) {
            val transaction = fragmentManager.beginTransaction()

            // 슬라이드 애니메이션 설정
            transaction.setCustomAnimations(
                R.anim.slide_in_right, // 새 Fragment가 들어오는 애니메이션
                R.anim.slide_out_left // 기존 Fragment가 나가는 애니메이션
            )

            // 기존에 보이는 Fragment 숨기기
            if (fragmentStack.isNotEmpty()) {
                transaction.hide(fragmentStack.peek())
            }

            transaction.add(R.id.fl_main_layout, fragment)
            transaction.addToBackStack(null)
            transaction.commitAllowingStateLoss()

            fragmentStack.push(fragment) // 스택에 추가

            ContextManager.mainActivity?.viewModel?._currentFragment?.value = fragment.javaClass.simpleName
        }

        ToastManager.closeToast()
    }

    // Fragment를 제거하고 스택에서 pop
    fun popFragment() {
        val fragmentManager = getFragmentManager()

        if (fragmentManager != null && fragmentManager.backStackEntryCount > 0 && fragmentStack.isNotEmpty()) {
            val transaction = fragmentManager.beginTransaction()

            // 슬라이드 애니메이션 설정
            transaction.setCustomAnimations(
                R.anim.slide_in_left, // 뒤로 가기 시 새 Fragment가 들어오는 애니메이션
                R.anim.slide_out_right // 뒤로 가기 시 기존 Fragment가 나가는 애니메이션
            )

            transaction.remove(fragmentStack.pop()) // 스택에서 제거한 Fragment 삭제
            transaction.commitAllowingStateLoss()

            if (fragmentStack.isNotEmpty()) {
                transaction.show(fragmentStack.peek()) // 이전 Fragment 다시 표시
            }

            fragmentManager.popBackStack()

            ContextManager.mainActivity?.viewModel?._currentFragment?.value = getCurrentFragment() ?: "MainFragment"
        }

        ToastManager.closeToast()
    }

    fun getCurrentFragment(): String? {
        return if (fragmentStack.isNotEmpty()) {
            fragmentStack.last().javaClass.simpleName
        } else {
            null
        }
    }
}
