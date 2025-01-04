package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.R

class MainFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment().apply {  }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.tv_mainFragment_score).setOnClickListener {
            ContextManager.mainActivity?.viewModel?.addFragment(MusicListFragment.newInstance())
        }
    }

}