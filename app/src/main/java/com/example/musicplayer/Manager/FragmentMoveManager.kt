package com.example.musicplayer.Manager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.musicplayer.R
import com.example.musicplayer.StatusBarViewController

class FragmentMoveManager() {
    private val fragmentManager: FragmentManager? = ContextManager.mainActivity?.supportFragmentManager

    // 현재 화면 에서 new Fragment add
    fun addFragment(fragment: Fragment) {
        if(fragmentManager != null){
            val transaction = fragmentManager.beginTransaction()
            transaction.add(R.id.fl_main_layout, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    // 현재 맨 위에 있는 Fragment pop
    fun popFragment(nextFragment:String?) {
        if(fragmentManager != null && fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        }

        if(nextFragment != null){
            ContextManager.mainActivity?.setFragmentTag(nextFragment)
            ContextManager.mainActivity?.let { StatusBarViewController(it).setStatusBarView(nextFragment) }
        }
    }

    // 모든 Fragment pop
    fun popBackStackToMain() {
        fragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}