package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.musicplayer.manager.FragmentMoveManager
import com.example.musicplayer.R

class MainFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_goto_recording).setOnClickListener {
            FragmentMoveManager.instance.pushFragment(RecordingOnlyFragment.newInstance())
        }

        view.findViewById<Button>(R.id.tv_mainFragment_score).setOnClickListener {
            FragmentMoveManager.instance.pushFragment(MusicListFragment.newInstance())
        }

        view.findViewById<Button>(R.id.btn_goto_download).setOnClickListener {
            FragmentMoveManager.instance.pushFragment(MusicDownloadFragment.newInstance())
        }
    }

}