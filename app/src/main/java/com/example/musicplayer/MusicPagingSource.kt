package com.example.musicplayer

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.ListObjects.MusicItem
import com.example.musicplayer.Manager.MusicLoaderManager

class MusicPagingSource(private val context: Context) : PagingSource<Int, MusicItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MusicItem> {
        val currentPage = params.key ?: 0
        val pageSize = params.loadSize
        val allAudioFiles = MusicLoaderManager.loadAudioList(context)
        val pagedData = allAudioFiles.drop(currentPage * pageSize).take(pageSize)

        return LoadResult.Page(
            data = pagedData,
            prevKey = if (currentPage == 0) null else currentPage - 1,
            nextKey = if (pagedData.size < pageSize) null else currentPage + 1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, MusicItem>): Int? {
        return state.anchorPosition?.div(state.config.pageSize)
    }
}
