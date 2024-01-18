package com.example.musicplayer.Activity

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.ContextManager
import com.example.musicplayer.ToastManager
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding

class MainActivity : AppCompatActivity() {
    var binding: MusicPlayerMainActivityBinding? = null
    var doubleBackToExitPressedOnce = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        ContextManager.mainContext = this
        ContextManager.mainActivity = this
        hideActionBar()
    }

    fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finish()
            return
        }
        doubleBackToExitPressedOnce = true
        ToastManager(this).showAnimatedToast("앱을 종료하려면 다시 한 번 눌러 주세요")
        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 3000)
    }
}