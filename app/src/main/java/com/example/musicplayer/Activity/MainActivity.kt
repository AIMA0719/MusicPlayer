package com.example.musicplayer.Activity

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.Manager.FragmentMoveManager
import com.example.musicplayer.Manager.PermissionManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.MusicListFragment
import com.example.musicplayer.StatusBarViewController
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding

class MainActivity : AppCompatActivity(){
    var binding: MusicPlayerMainActivityBinding? = null
    var doubleBackToExitPressedOnce = false
    private var fragmentTag: String = ""

    private lateinit var fragmentMoveManager: FragmentMoveManager

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        binding = MusicPlayerMainActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setBaseSetting();
        setFragmentTag("MainFragment")
        hideActionBar()
        PermissionManager(this).checkPermission()
    }
    private fun setBaseSetting() {
        ContextManager.mainContext = this
        ContextManager.mainActivity = this
        fragmentMoveManager = FragmentMoveManager()
    }

    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        StatusBarViewController(this).setStatusBarView(getFragmentTag())
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
                fragmentMoveManager.popFragment("MainFragment")
            }

            "MainFragment" -> {
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed()
                    ToastManager(this).removeAnimationToast()
                    finish()
                    return
                }
                doubleBackToExitPressedOnce = true
                ToastManager(this).showAnimatedToast("앱을 종료하려면 다시 한 번 눌러 주세요")
                Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 3000)
            }
        }
    }

}