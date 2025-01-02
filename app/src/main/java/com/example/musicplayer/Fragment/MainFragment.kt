package com.example.musicplayer.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.Manager.ContextManager
import com.example.musicplayer.Manager.ToastManager
import com.example.musicplayer.R
import com.example.musicplayer.ViewModel.MainFragmentViewModel

class MainFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment().apply {  }
    }

    private lateinit var viewModel: MainFragmentViewModel
    private lateinit var toastManager: ToastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainFragmentViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        toastManager = ToastManager(context)
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