package com.example.musicplayer;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewConfiguration;

public class UtilManager {
    private static DisplayMetrics mMetrics;
    private static int mMinimumFlingVelocity = 50;
    private static int mMaximumFlingVelocity = 8000;
    public final static double DEG2RAD = (Math.PI / 180.0);
    public final static float FDEG2RAD = ((float) Math.PI / 180.f);

    @SuppressWarnings("unused")
    public final static double DOUBLE_EPSILON = Double.longBitsToDouble(1);

    @SuppressWarnings("unused")
    public final static float FLOAT_EPSILON = Float.intBitsToFloat(1);

    @SuppressWarnings("deprecation")
    public static void init(Context context) {

        try{
            if (context == null) {
                // noinspection deprecation
                mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
                // noinspection deprecation
                mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();

                Log.e("MPChartLib-Utils"
                        , "Utils.init(...) PROVIDED CONTEXT OBJECT IS NULL");

            } else {
                ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
                mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
                mMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

                Resources res = context.getResources();
                mMetrics = res.getDisplayMetrics();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Deprecated
    public static void init(Resources res) {

        mMetrics = res.getDisplayMetrics();

        // noinspection deprecation
        mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
        // noinspection deprecation
        mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
    }


    public static float convertDpToPixel(float dp) {

        if (mMetrics == null) {

            Log.e("MPChartLib-Utils",
                    "Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before" +
                            " calling Utils.convertDpToPixel(...). Otherwise conversion does not " +
                            "take place.");
            return dp;
        }

        return dp * mMetrics.density;
    }
}
