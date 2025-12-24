package com.example.musicplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.BottomSheetSongDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 곡 선택 시 모드 선택을 위한 바텀시트
 */
class SongDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSongDetailBinding? = null
    private val binding get() = _binding!!

    private var title: String = ""
    private var artist: String = ""
    private var duration: Long = 0L
    private var isOnline: Boolean = false
    private var thumbnailUrl: String? = null

    private var actionListener: OnActionListener? = null

    interface OnActionListener {
        fun onPracticeMode()
        fun onChallengeMode()
        fun onDownload()
    }

    fun setOnActionListener(listener: OnActionListener) {
        this.actionListener = listener
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ARTIST = "arg_artist"
        private const val ARG_DURATION = "arg_duration"
        private const val ARG_IS_ONLINE = "arg_is_online"
        private const val ARG_THUMBNAIL_URL = "arg_thumbnail_url"

        fun newInstance(
            title: String,
            artist: String,
            duration: Long,
            isOnline: Boolean,
            thumbnailUrl: String? = null
        ): SongDetailBottomSheet {
            return SongDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_ARTIST, artist)
                    putLong(ARG_DURATION, duration)
                    putBoolean(ARG_IS_ONLINE, isOnline)
                    putString(ARG_THUMBNAIL_URL, thumbnailUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE, "")
            artist = it.getString(ARG_ARTIST, "")
            duration = it.getLong(ARG_DURATION, 0L)
            isOnline = it.getBoolean(ARG_IS_ONLINE, false)
            thumbnailUrl = it.getString(ARG_THUMBNAIL_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSongDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 곡 정보 설정
        binding.tvSongTitle.text = title
        binding.tvArtist.text = if (artist.isNotEmpty() && artist != "<unknown>") artist else "알 수 없는 아티스트"

        // 재생 시간 포맷
        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        binding.tvDuration.text = String.format("%d:%02d", minutes, seconds)

        // 썸네일
        if (!thumbnailUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_karaoke)
                .error(R.drawable.ic_karaoke)
                .centerCrop()
                .into(binding.ivThumbnail)
        }

        // 다운로드 버튼 (온라인 곡만 표시)
        binding.cardDownload.isVisible = isOnline

        // 클릭 리스너
        binding.cardPracticeMode.setOnClickListener {
            actionListener?.onPracticeMode()
            dismiss()
        }

        binding.cardChallengeMode.setOnClickListener {
            actionListener?.onChallengeMode()
            dismiss()
        }

        binding.cardDownload.setOnClickListener {
            actionListener?.onDownload()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
