package com.example.musicplayer.fragment

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.musicplayer.R
import java.text.SimpleDateFormat
import java.util.*

class VersionInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_version_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadVersionInfo(view)
    }

    private fun loadVersionInfo(view: View) {
        val tvVersion = view.findViewById<TextView>(R.id.tvVersion)
        val tvVersionCode = view.findViewById<TextView>(R.id.tvVersionCode)
        val tvBuildDate = view.findViewById<TextView>(R.id.tvBuildDate)

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().packageManager.getPackageInfo(
                    requireContext().packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            }

            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            tvVersion.text = "버전 $versionName"
            tvVersionCode.text = versionCode.toString()

            // Get build date (approximation from package install time)
            val installTime = packageInfo.firstInstallTime
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            tvBuildDate.text = dateFormat.format(Date(installTime))

        } catch (e: Exception) {
            e.printStackTrace()
            tvVersion.text = "버전 1.0.0"
            tvVersionCode.text = "1"
            tvBuildDate.text = "Unknown"
        }
    }

    companion object {
        fun newInstance() = VersionInfoFragment()
    }
}
