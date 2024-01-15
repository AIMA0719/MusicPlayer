package com.example.musicplayer.Activity;

import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.ContextManager;
import com.example.musicplayer.ToastManager;
import com.example.musicplayer.databinding.MusicPlayerMainActivityBinding;

public class MainActivity extends AppCompatActivity {
    public MusicPlayerMainActivityBinding binding;
    public boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MusicPlayerMainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hideActionBar();
        ContextManager.setMainContext(this);
        ContextManager.setMainActivity(this);

    }

    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null){
            actionBar.hide();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(doubleBackToExitPressedOnce){
            super.onBackPressed();
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        new ToastManager(this).showAnimatedToast("앱을 종료하려면 다시 한 번 눌러 주세요");
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 1000);
    }
}