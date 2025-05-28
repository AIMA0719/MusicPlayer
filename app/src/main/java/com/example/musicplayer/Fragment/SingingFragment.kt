package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentSingingBinding

class SingingFragment : Fragment() {

    private lateinit var binding: FragmentSingingBinding
    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = it.getParcelable("music")!!
            pitchArray = it.getFloatArray("pitch")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSingingBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance(music: MusicFile, pitchArray: FloatArray): SingingFragment {
            val fragment = SingingFragment()
            val args = Bundle().apply {
                putParcelable("music", music)
                putFloatArray("pitchArray", pitchArray)
            }
            fragment.arguments = args
            return fragment
        }
    }

}