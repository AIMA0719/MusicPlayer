package com.example.musicplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R

class LibraryInfoFragment : Fragment() {

    private lateinit var adapter: LibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_library_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        loadLibraries()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvLibraries)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LibraryAdapter()
        recyclerView.adapter = adapter
    }

    private fun loadLibraries() {
        val libraries = listOf(
            LibraryInfo(
                "Kotlin",
                "JVM용 정적 타입 프로그래밍 언어",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Android Jetpack",
                "Android 앱 개발을 위한 라이브러리 모음",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Room",
                "SQLite 데이터베이스 추상화 레이어",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Navigation Component",
                "앱 내 화면 전환 관리 라이브러리",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Hilt",
                "의존성 주입 라이브러리",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Coroutines",
                "비동기 프로그래밍 라이브러리",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Material Components",
                "Material Design UI 컴포넌트",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "MPAndroidChart",
                "Android 차트 라이브러리",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "TarsosDSP",
                "음악 신호 처리 라이브러리",
                "GPL-3.0 License"
            ),
            LibraryInfo(
                "Firebase",
                "앱 개발 플랫폼",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "OkHttp",
                "HTTP 클라이언트 라이브러리",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Retrofit",
                "타입 안전 HTTP 클라이언트",
                "Apache License 2.0"
            ),
            LibraryInfo(
                "Gson",
                "JSON 직렬화/역직렬화 라이브러리",
                "Apache License 2.0"
            )
        )

        adapter.submitList(libraries)
    }

    data class LibraryInfo(
        val name: String,
        val description: String,
        val license: String
    )

    inner class LibraryAdapter : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

        private var items = listOf<LibraryInfo>()

        fun submitList(newItems: List<LibraryInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_library, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvLibraryName)
            private val tvDescription: TextView = view.findViewById(R.id.tvLibraryDescription)
            private val tvLicense: TextView = view.findViewById(R.id.tvLibraryLicense)

            fun bind(library: LibraryInfo) {
                tvName.text = library.name
                tvDescription.text = library.description
                tvLicense.text = library.license
            }
        }
    }

    companion object {
        fun newInstance() = LibraryInfoFragment()
    }
}
