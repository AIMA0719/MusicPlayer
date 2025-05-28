package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.musicplayer.ViewModel.RecordingViewModel
import com.example.musicplayer.data.MusicFile
import com.example.musicplayer.databinding.FragmentRecordingBinding

class RecordingFragment : Fragment() {

    private val viewModel: RecordingViewModel by viewModels()
    private lateinit var binding: FragmentRecordingBinding
    private lateinit var music: MusicFile
    private lateinit var pitchArray: FloatArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            music = it.getParcelable("music")!!          // ✅ 그대로 유지
            pitchArray = it.getFloatArray("pitchArray")!! // ✅ 키 이름 수정됨
        }
    }

    companion object {
        fun newInstance(music: MusicFile, pitchArray: FloatArray): RecordingFragment {
            val fragment = RecordingFragment()
            val args = Bundle().apply {
                putParcelable("music", music)
                putFloatArray("pitchArray", pitchArray) // ✅ 동일한 키로 통일
            }
            fragment.arguments = args
            return fragment
        }
    }
}
