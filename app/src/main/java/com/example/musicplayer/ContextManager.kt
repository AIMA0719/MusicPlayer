package com.example.musicplayer;

import android.content.Context;

import com.example.musicplayer.Activity.MainActivity;

public class ContextManager {
    public static Context mainContext;
    public static MainActivity mainActivity;

    public static Context getMainContext() {
        return mainContext;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    public static void setMainActivity(MainActivity mainActivity) {
        ContextManager.mainActivity = mainActivity;
    }

    public static void setMainContext(Context mainContext) {
        ContextManager.mainContext = mainContext;
    }
}
