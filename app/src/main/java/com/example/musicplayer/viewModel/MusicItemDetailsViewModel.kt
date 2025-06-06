package com.example.musicplayer.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.listObjects.MusicItem

class MusicItemDetailsViewModel : ViewModel() {

    // 내부에서만 변경 가능한 MutableLiveData
    private val _musicItem = MutableLiveData<MusicItem>()

    // 외부에서는 읽기만 가능한 LiveData
    val musicItem: LiveData<MusicItem> get() = _musicItem

    // MusicItem 설정 메서드
    fun setMusicItem(item: MusicItem) {
        _musicItem.value = item
    }
}
