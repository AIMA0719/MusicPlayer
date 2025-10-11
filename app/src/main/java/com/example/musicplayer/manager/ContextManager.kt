package com.example.musicplayer.manager

import android.content.Context
import java.lang.ref.WeakReference

object ContextManager {
    private var mainContextRef: WeakReference<Context>? = null
    
    fun setContext(context: Context) {
        mainContextRef = WeakReference(context)
    }
    
    fun getContext(): Context? {
        return mainContextRef?.get()
    }
    
    fun clearContext() {
        mainContextRef?.clear()
        mainContextRef = null
    }
}
