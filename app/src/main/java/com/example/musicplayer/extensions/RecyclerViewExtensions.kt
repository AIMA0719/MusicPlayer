package com.example.musicplayer.extensions

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 수직 레이아웃 설정
 */
fun RecyclerView.setupVertical(
    adapter: RecyclerView.Adapter<*>,
    addDivider: Boolean = false
) {
    layoutManager = LinearLayoutManager(context)
    this.adapter = adapter

    if (addDivider) {
        addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
    }
}

/**
 * RecyclerView 수평 레이아웃 설정
 */
fun RecyclerView.setupHorizontal(
    adapter: RecyclerView.Adapter<*>,
    addDivider: Boolean = false
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    this.adapter = adapter

    if (addDivider) {
        addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)
        )
    }
}
