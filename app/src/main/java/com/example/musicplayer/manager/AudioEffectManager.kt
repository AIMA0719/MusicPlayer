package com.example.musicplayer.manager

import android.content.Context
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.PresetReverb
import timber.log.Timber

/**
 * 오디오 이펙트 관리자
 *
 * - 리버브 (잔향) 효과
 * - 에코 효과
 * - 프리셋 기반 설정
 */
class AudioEffectManager(private val context: Context) {

    companion object {
        // 리버브 프리셋
        enum class ReverbPreset(val value: Short, val displayName: String) {
            NONE(PresetReverb.PRESET_NONE, "없음"),
            SMALL_ROOM(PresetReverb.PRESET_SMALLROOM, "작은 방"),
            MEDIUM_ROOM(PresetReverb.PRESET_MEDIUMROOM, "중간 방"),
            LARGE_ROOM(PresetReverb.PRESET_LARGEROOM, "큰 방"),
            MEDIUM_HALL(PresetReverb.PRESET_MEDIUMHALL, "중간 홀"),
            LARGE_HALL(PresetReverb.PRESET_LARGEHALL, "큰 홀"),
            PLATE(PresetReverb.PRESET_PLATE, "플레이트");

            companion object {
                fun fromValue(value: Short): ReverbPreset {
                    return entries.find { it.value == value } ?: NONE
                }

                fun fromIndex(index: Int): ReverbPreset {
                    return entries.getOrElse(index) { NONE }
                }
            }
        }

        // 커스텀 리버브 설정
        data class CustomReverbSettings(
            val roomLevel: Short = -1000,       // -9600 ~ 0 mB
            val roomHFLevel: Short = -100,      // -9600 ~ 0 mB
            val decayTime: Int = 1000,          // 100 ~ 20000 ms
            val decayHFRatio: Short = 500,      // 100 ~ 2000 (1/1000)
            val reflectionsLevel: Short = -200, // -9600 ~ 1000 mB
            val reflectionsDelay: Int = 20,     // 0 ~ 300 ms
            val reverbLevel: Short = -1000,     // -9600 ~ 2000 mB
            val reverbDelay: Int = 40,          // 0 ~ 100 ms
            val diffusion: Short = 1000,        // 0 ~ 1000 (1/10 %)
            val density: Short = 1000           // 0 ~ 1000 (1/10 %)
        ) {
            companion object {
                // 프리셋 설정들
                val KARAOKE_SMALL = CustomReverbSettings(
                    roomLevel = -500,
                    roomHFLevel = -200,
                    decayTime = 800,
                    decayHFRatio = 600,
                    reflectionsLevel = -300,
                    reflectionsDelay = 10,
                    reverbLevel = -800,
                    reverbDelay = 20,
                    diffusion = 800,
                    density = 900
                )

                val KARAOKE_MEDIUM = CustomReverbSettings(
                    roomLevel = -300,
                    roomHFLevel = -100,
                    decayTime = 1200,
                    decayHFRatio = 700,
                    reflectionsLevel = -200,
                    reflectionsDelay = 15,
                    reverbLevel = -500,
                    reverbDelay = 30,
                    diffusion = 900,
                    density = 950
                )

                val KARAOKE_LARGE = CustomReverbSettings(
                    roomLevel = -200,
                    roomHFLevel = 0,
                    decayTime = 2000,
                    decayHFRatio = 800,
                    reflectionsLevel = -100,
                    reflectionsDelay = 25,
                    reverbLevel = -300,
                    reverbDelay = 50,
                    diffusion = 1000,
                    density = 1000
                )

                val CONCERT_HALL = CustomReverbSettings(
                    roomLevel = -100,
                    roomHFLevel = 0,
                    decayTime = 3000,
                    decayHFRatio = 900,
                    reflectionsLevel = 0,
                    reflectionsDelay = 30,
                    reverbLevel = -200,
                    reverbDelay = 60,
                    diffusion = 1000,
                    density = 1000
                )
            }
        }

        // SharedPreferences 키
        private const val PREFS_NAME = "audio_effect_settings"
        private const val KEY_REVERB_ENABLED = "reverb_enabled"
        private const val KEY_REVERB_PRESET = "reverb_preset"
        private const val KEY_REVERB_LEVEL = "reverb_level"
        private const val KEY_ECHO_ENABLED = "echo_enabled"
        private const val KEY_ECHO_DELAY = "echo_delay"
        private const val KEY_ECHO_DECAY = "echo_decay"
    }

    // 이펙트 인스턴스
    private var presetReverb: PresetReverb? = null
    private var environmentalReverb: EnvironmentalReverb? = null

    // 현재 설정
    private var isReverbEnabled: Boolean = false
    private var currentReverbPreset: ReverbPreset = ReverbPreset.NONE
    private var reverbLevel: Int = 50 // 0-100

    private var isEchoEnabled: Boolean = false
    private var echoDelay: Int = 100 // ms
    private var echoDecay: Float = 0.5f // 0.0-1.0

    // 연결된 오디오 세션
    private var audioSessionId: Int = 0

    init {
        loadSettings()
    }

    /**
     * 오디오 세션에 이펙트 연결
     */
    fun attachToSession(sessionId: Int) {
        if (sessionId == 0) {
            Timber.w("Invalid audio session ID: 0")
            return
        }

        // 기존 이펙트 해제
        release()

        audioSessionId = sessionId

        try {
            // PresetReverb 생성
            presetReverb = PresetReverb(0, sessionId).apply {
                preset = currentReverbPreset.value
                enabled = isReverbEnabled
            }
            Timber.d("PresetReverb attached to session $sessionId")

            // EnvironmentalReverb 생성 (더 세밀한 조정용)
            environmentalReverb = EnvironmentalReverb(0, sessionId).apply {
                enabled = false // 기본적으로 비활성화
            }
            Timber.d("EnvironmentalReverb attached to session $sessionId")

        } catch (e: Exception) {
            Timber.e(e, "Failed to create audio effects")
        }
    }

    /**
     * 리버브 활성화/비활성화
     */
    fun setReverbEnabled(enabled: Boolean) {
        isReverbEnabled = enabled
        presetReverb?.enabled = enabled
        saveSettings()
        Timber.d("Reverb enabled: $enabled")
    }

    /**
     * 리버브 프리셋 설정
     */
    fun setReverbPreset(preset: ReverbPreset) {
        currentReverbPreset = preset
        presetReverb?.preset = preset.value
        saveSettings()
        Timber.d("Reverb preset: ${preset.displayName}")
    }

    /**
     * 리버브 레벨 설정 (0-100)
     */
    fun setReverbLevel(level: Int) {
        reverbLevel = level.coerceIn(0, 100)
        // PresetReverb는 레벨 조정이 제한적이므로 EnvironmentalReverb 사용
        environmentalReverb?.let { reverb ->
            val roomLevel = (-9600 + (reverbLevel * 96)).toShort()
            reverb.roomLevel = roomLevel
        }
        saveSettings()
        Timber.d("Reverb level: $reverbLevel")
    }

    /**
     * 커스텀 리버브 설정 적용
     */
    fun applyCustomReverb(settings: CustomReverbSettings) {
        environmentalReverb?.let { reverb ->
            try {
                reverb.roomLevel = settings.roomLevel
                reverb.roomHFLevel = settings.roomHFLevel
                reverb.decayTime = settings.decayTime
                reverb.decayHFRatio = settings.decayHFRatio
                reverb.reflectionsLevel = settings.reflectionsLevel
                reverb.reflectionsDelay = settings.reflectionsDelay
                reverb.reverbLevel = settings.reverbLevel
                reverb.reverbDelay = settings.reverbDelay
                reverb.diffusion = settings.diffusion
                reverb.density = settings.density

                // EnvironmentalReverb 활성화
                presetReverb?.enabled = false
                reverb.enabled = true

                Timber.d("Custom reverb applied")
            } catch (e: Exception) {
                Timber.e(e, "Failed to apply custom reverb")
            }
        }
    }

    /**
     * 노래방 리버브 프리셋 적용
     */
    fun applyKaraokeReverb(size: KaraokeReverbSize) {
        val settings = when (size) {
            KaraokeReverbSize.SMALL -> CustomReverbSettings.KARAOKE_SMALL
            KaraokeReverbSize.MEDIUM -> CustomReverbSettings.KARAOKE_MEDIUM
            KaraokeReverbSize.LARGE -> CustomReverbSettings.KARAOKE_LARGE
            KaraokeReverbSize.CONCERT -> CustomReverbSettings.CONCERT_HALL
        }
        applyCustomReverb(settings)
    }

    /**
     * 에코 활성화/비활성화
     * 주의: Android 기본 API는 에코를 직접 지원하지 않으므로
     * 실제 구현에서는 OpenSL ES나 별도 라이브러리 필요
     */
    fun setEchoEnabled(enabled: Boolean) {
        isEchoEnabled = enabled
        saveSettings()
        Timber.d("Echo enabled: $enabled (Note: requires custom implementation)")
    }

    /**
     * 에코 딜레이 설정 (ms)
     */
    fun setEchoDelay(delayMs: Int) {
        echoDelay = delayMs.coerceIn(50, 500)
        saveSettings()
        Timber.d("Echo delay: $echoDelay ms")
    }

    /**
     * 에코 감쇠율 설정 (0.0-1.0)
     */
    fun setEchoDecay(decay: Float) {
        echoDecay = decay.coerceIn(0.1f, 0.9f)
        saveSettings()
        Timber.d("Echo decay: $echoDecay")
    }

    /**
     * 모든 이펙트 비활성화
     */
    fun disableAllEffects() {
        isReverbEnabled = false
        isEchoEnabled = false

        presetReverb?.enabled = false
        environmentalReverb?.enabled = false

        saveSettings()
        Timber.d("All effects disabled")
    }

    /**
     * 현재 설정 가져오기
     */
    fun isReverbEnabled(): Boolean = isReverbEnabled
    fun getCurrentReverbPreset(): ReverbPreset = currentReverbPreset
    fun getReverbLevel(): Int = reverbLevel
    fun isEchoEnabled(): Boolean = isEchoEnabled
    fun getEchoDelay(): Int = echoDelay
    fun getEchoDecay(): Float = echoDecay

    /**
     * 설정 저장
     */
    private fun saveSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_REVERB_ENABLED, isReverbEnabled)
            putInt(KEY_REVERB_PRESET, currentReverbPreset.ordinal)
            putInt(KEY_REVERB_LEVEL, reverbLevel)
            putBoolean(KEY_ECHO_ENABLED, isEchoEnabled)
            putInt(KEY_ECHO_DELAY, echoDelay)
            putFloat(KEY_ECHO_DECAY, echoDecay)
            apply()
        }
    }

    /**
     * 설정 로드
     */
    private fun loadSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isReverbEnabled = prefs.getBoolean(KEY_REVERB_ENABLED, false)
        currentReverbPreset = ReverbPreset.fromIndex(prefs.getInt(KEY_REVERB_PRESET, 0))
        reverbLevel = prefs.getInt(KEY_REVERB_LEVEL, 50)
        isEchoEnabled = prefs.getBoolean(KEY_ECHO_ENABLED, false)
        echoDelay = prefs.getInt(KEY_ECHO_DELAY, 100)
        echoDecay = prefs.getFloat(KEY_ECHO_DECAY, 0.5f)
    }

    /**
     * 리소스 해제
     */
    fun release() {
        try {
            presetReverb?.release()
            presetReverb = null
        } catch (e: Exception) {
            Timber.e(e, "Error releasing PresetReverb")
        }

        try {
            environmentalReverb?.release()
            environmentalReverb = null
        } catch (e: Exception) {
            Timber.e(e, "Error releasing EnvironmentalReverb")
        }

        audioSessionId = 0
        Timber.d("AudioEffectManager released")
    }

    /**
     * 노래방 리버브 크기
     */
    enum class KaraokeReverbSize(val displayName: String) {
        SMALL("작은 노래방"),
        MEDIUM("중간 노래방"),
        LARGE("큰 노래방"),
        CONCERT("콘서트 홀")
    }
}
