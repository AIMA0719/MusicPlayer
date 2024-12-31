package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Adapter.MyItemRecyclerViewAdapter
import com.example.musicplayer.R

class MusicListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_music_list_list, container, false)

        // `is` 는 java 의 instanceOf 랑 비슷한 것 같음
        if (view is RecyclerView) {
            with(view) {
                // `with`는 특정 객체의 속성이나 메서드를 간결하게 사용할 때 사용
                layoutManager = LinearLayoutManager(context)
                adapter = MyItemRecyclerViewAdapter(context)
            }
        }
        return view // 생성된 뷰를 반환
    }

    companion object {
        // 정적 메서드로 Fragment 인스턴스를 생성
        @JvmStatic
        fun newInstance() = MusicListFragment().apply {}
    }
}
