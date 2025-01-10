package com.example.musicplayer.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.MusicPagingSource
import kotlinx.coroutines.flow.Flow

class MusicViewModel(context: Context) : ViewModel() {
    val musicFlow: Flow<PagingData<MusicItem>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = true),
        pagingSourceFactory = { MusicPagingSource(context) }
    ).flow.cachedIn(viewModelScope)
}
