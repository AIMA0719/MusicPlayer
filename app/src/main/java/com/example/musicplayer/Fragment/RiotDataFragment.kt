package com.example.musicplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musicplayer.Manager.LogManager
import com.example.musicplayer.PostResult
import com.example.musicplayer.Server.RetrofitClient
import com.example.musicplayer.Server.RetrofitService
import com.example.musicplayer.databinding.FragmentRiotDataBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiotDataFragment : Fragment() {
    private lateinit var binding: FragmentRiotDataBinding
    private lateinit var retrofitService: RetrofitService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRiotDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {// RetrofitService 인터페이스 초기화
            retrofitService = RetrofitClient.retrofit.create(RetrofitService::class.java)

            // Retrofit을 사용하여 데이터 가져오기
            val call: Call<PostResult> = retrofitService.getPosts("1")
            call.enqueue(object : Callback<PostResult> {
                override fun onResponse(call: Call<PostResult>, response: Response<PostResult>) {
                    if (response.isSuccessful) {
                        // 요청이 성공한 경우 처리
                        val postResult: PostResult? = response.body()
                        LogManager.e(postResult.toString())
                        // TODO: 받아온 데이터 처리
                    } else {
                        // 요청이 실패한 경우 처리
                        // TODO: 실패 상황에 대한 처리
                    }
                }

                override fun onFailure(call: Call<PostResult>, t: Throwable) {
                    // 통신 실패 시 처리
                    // TODO: 실패 상황에 대한 처리
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RiotDataFragment().apply {}
    }
}