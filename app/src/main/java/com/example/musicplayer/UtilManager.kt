package com.example.musicplayer

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewConfiguration

object UtilManager {
    private var mMetrics: DisplayMetrics? = null
    private var mMinimumFlingVelocity = 50
    private var mMaximumFlingVelocity = 8000
    const val DEG2RAD = Math.PI / 180.0
    const val FDEG2RAD = Math.PI.toFloat() / 180f

    @Suppress("unused")
    val DOUBLE_EPSILON = java.lang.Double.longBitsToDouble(1)

    @Suppress("unused")
    val FLOAT_EPSILON = java.lang.Float.intBitsToFloat(1)
    @Suppress("deprecation")
    fun init(context: Context?) {
        try {
            if (context == null) {
                // noinspection deprecation
                mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity()
                // noinspection deprecation
                mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity()
                Log.e("MPChartLib-Utils", "Utils.init(...) PROVIDED CONTEXT OBJECT IS NULL")
            } else {
                val viewConfiguration = ViewConfiguration.get(context)
                mMinimumFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
                mMaximumFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
                val res = context.resources
                mMetrics = res.displayMetrics
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("")
    fun init(res: Resources) {
        mMetrics = res.displayMetrics

        // noinspection deprecation
        mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity()
        // noinspection deprecation
        mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity()
    }

    @JvmStatic
    fun convertDpToPixel(dp: Float): Float {
        if (mMetrics == null) {
            Log.e("MPChartLib-Utils",
                    "Utils NOT INITIALIZED. You need to call Utils.init(...) at least once before" +
                            " calling Utils.convertDpToPixel(...). Otherwise conversion does not " +
                            "take place.")
            return dp
        }
        return dp * mMetrics!!.density
    }
}
