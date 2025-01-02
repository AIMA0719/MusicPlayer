package com.example.musicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.Manager.FragmentMoveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private val _currentFragment = MutableLiveData<String>("MainFragment")
    val currentFragment: LiveData<String> get() = _currentFragment

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private var doubleBackToExit = false

    fun isFragmentStackEmpty(): Boolean {
        return FragmentMoveManager.instance.getStackState().isEmpty()
    }

    fun isDoubleBackToExit(): Boolean {
        return doubleBackToExit
    }

    fun triggerDoubleBackToExit() {
        doubleBackToExit = true
        _toastMessage.value = "앱을 종료하려면 다시 한 번 눌러 주세요"
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000L)
            doubleBackToExit = false
        }
    }

    fun addFragment(fragment: androidx.fragment.app.Fragment) {
        FragmentMoveManager.instance.pushFragment(fragment)
        _currentFragment.value = fragment.javaClass.simpleName
    }

    fun popFragment() {
        FragmentMoveManager.instance.popFragment()
        _currentFragment.value = FragmentMoveManager.instance.getCurrentFragment() ?: "MainFragment"
    }
}
