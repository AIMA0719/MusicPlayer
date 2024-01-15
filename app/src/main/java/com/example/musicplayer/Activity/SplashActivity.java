package com.example.musicplayer.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.databinding.ActivitySplashBinding;

import java.util.Objects;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000;
    public Handler mHideHandler;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        mHideHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        setContentView(binding.getRoot());

        new Handler().postDelayed(splashRunnable,SPLASH_DELAY);
    }

    Runnable splashRunnable = () -> {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    };
}