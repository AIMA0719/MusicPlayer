package com.example.musicplayer.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicplayer.activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * ExoPlayer 기반 음악 재생 서비스
 *
 * - 백그라운드 재생 지원
 * - MediaSession을 통한 미디어 컨트롤
 * - 알림 표시 (재생/일시정지/다음/이전)
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null
    private var playerListener: Player.Listener? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("MusicPlaybackService created")

        // MediaSession 초기화
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(createPendingIntent())
            .build()

        // Player 리스너 추가 (참조 저장하여 나중에 제거)
        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> Timber.d("Player State: IDLE")
                    Player.STATE_BUFFERING -> Timber.d("Player State: BUFFERING")
                    Player.STATE_READY -> Timber.d("Player State: READY")
                    Player.STATE_ENDED -> Timber.d("Player State: ENDED")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Timber.d("Is Playing: $isPlaying")
            }
        }
        playerListener?.let { exoPlayer.addListener(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // Player 리스너 제거
        playerListener?.let { exoPlayer.removeListener(it) }
        playerListener = null

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        Timber.d("MusicPlaybackService destroyed")
        super.onDestroy()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val ACTION_PLAY = "com.example.musicplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.musicplayer.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.musicplayer.ACTION_PREVIOUS"
    }
}
