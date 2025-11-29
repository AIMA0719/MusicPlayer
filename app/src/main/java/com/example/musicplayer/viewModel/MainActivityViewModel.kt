package com.example.musicplayer.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.fragment.MainFragment
import com.example.musicplayer.manager.FragmentMoveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    val _currentFragment = MutableLiveData("MainFragment")
    val currentFragment: LiveData<String> get() = _currentFragment

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private var doubleBackToExit = false

    fun isFragmentStackEmpty(): Boolean {
        val currentFragment = FragmentMoveManager.instance.getCurrentFragment()
        return currentFragment == null || currentFragment == MainFragment::class.java.simpleName
    }

    fun isDoubleBackToExit(): Boolean {
        return doubleBackToExit
    }

    fun triggerDoubleBackToExit() {
        doubleBackToExit = true
        _toastMessage.value = "앱을 종료하려면 다시 한 번 눌러 주세요"
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000L)
            doubleBackToExit = false
        }
    }
}
