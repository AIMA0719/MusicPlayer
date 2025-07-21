package com.example.musicplayer.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.viewModel.MusicListViewModel

class MusicListViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
