package com.example.musicplayer.Activity

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.Manager.FragmentMoveManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.MusicListFragment
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding

class MainActivity : AppCompatActivity() {
    private var binding: MusicPlayerMainActivityBinding? = null
    private var doubleBackToExitPressedOnce = false
    private var fragmentTag: String = ""

    private lateinit var fragmentMoveManager: FragmentMoveManager

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        ContextManager.mainContext = this
        ContextManager.mainActivity = this
        fragmentMoveManager = FragmentMoveManager()

        hideActionBar()
    }

    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        binding?.flMainLayout?.setOnClickListener({
            fragmentMoveManager.addFragment(MusicListFragment.newInstance(1))
        })
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

    public fun setFragmentTag(fragmentTag:String){
        this.fragmentTag = fragmentTag;
    }

    public fun getFragmentTag(): String {
        return this.fragmentTag
    }

    override fun onBackPressed() {
        when(getFragmentTag()){
            "MusicListFragment" -> {
                fragmentMoveManager.popFragment()
            }

            "MainFragment" -> {
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

        when(this.supportFragmentManager.backStackEntryCount){
            0 -> setFragmentTag("MainFragment")
        }
    }
}