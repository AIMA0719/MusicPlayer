package com.example.musicplayer.manager

import com.example.musicplayer.data.ChallengeType
import com.example.musicplayer.data.WeeklyChallenge
import com.example.musicplayer.data.WeeklyChallengeFactory
import com.example.musicplayer.data.local.database.dao.WeeklyChallengeDao
import com.example.musicplayer.data.local.database.entity.WeeklyChallengeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 주간 챌린지 관리 매니저
 */
@Singleton
class WeeklyChallengeManager @Inject constructor(
    private val challengeDao: WeeklyChallengeDao
) {
    private var currentUserId: String = "guest"

    /**
     * 사용자 ID 설정
     */
    fun setUserId(userId: String) {
        currentUserId = userId
    }

    /**
     * 현재 주간 챌린지 목록 조회 (Flow)
     */
    fun getCurrentChallenges(): Flow<List<WeeklyChallenge>> {
        val today = LocalDate.now().toString()
        return challengeDao.getCurrentChallenges(currentUserId, today).map { entities ->
            entities.map { it.toWeeklyChallenge() }
        }
    }

    /**
     * 이번 주 챌린지 초기화 (앱 시작 시 호출)
     */
    suspend fun initializeWeeklyChallenges() {
        val weekStart = WeeklyChallenge.getWeekStartDate()

        // 이미 이번 주 챌린지가 있는지 확인
        val existingChallenge = challengeDao.getChallengeByType(
            currentUserId,
            ChallengeType.SING_COUNT.name,
            weekStart.toString()
        )

        if (existingChallenge == null) {
            // 이번 주 챌린지 생성
            val newChallenges = WeeklyChallengeFactory.generateWeeklyChallenges()
            val entities = newChallenges.map {
                WeeklyChallengeEntity.fromWeeklyChallenge(it, currentUserId)
            }
            challengeDao.insertOrUpdateAll(entities)
            Timber.d("Weekly challenges initialized for user: $currentUserId")
        }

        // 오래된 챌린지 정리 (4주 이전)
        val cutoffDate = weekStart.minusWeeks(4).toString()
        challengeDao.deleteOldChallenges(cutoffDate)
    }

    /**
     * 녹음 완료 시 챌린지 업데이트
     */
    suspend fun onRecordingCompleted(
        songName: String,
        score: Int,
        difficulty: String,
        isNewSong: Boolean
    ): List<ChallengeUpdateResult> {
        val results = mutableListOf<ChallengeUpdateResult>()
        val weekStart = WeeklyChallenge.getWeekStartDate()

        // 1. 노래 부르기 챌린지 (SING_COUNT)
        updateChallengeProgress(
            ChallengeType.SING_COUNT,
            weekStart,
            incrementBy = 1
        )?.let { results.add(it) }

        // 2. 90점 이상 챌린지 (SCORE_ABOVE_90)
        if (score >= 90) {
            updateChallengeProgress(
                ChallengeType.SCORE_ABOVE_90,
                weekStart,
                incrementBy = 1
            )?.let { results.add(it) }
        }

        // 3. 신곡 도전 챌린지 (NEW_SONG)
        if (isNewSong) {
            updateChallengeProgress(
                ChallengeType.NEW_SONG,
                weekStart,
                incrementBy = 1
            )?.let { results.add(it) }
        }

        // 4. 고난이도 챌린지 (HIGH_DIFFICULTY)
        if (difficulty == "HARD" || difficulty == "VERY_HARD") {
            updateChallengeProgress(
                ChallengeType.HIGH_DIFFICULTY,
                weekStart,
                incrementBy = 1
            )?.let { results.add(it) }
        }

        return results
    }

    /**
     * 연속 이용 챌린지 업데이트 (매일 호출)
     */
    suspend fun onDailyLogin() {
        val weekStart = WeeklyChallenge.getWeekStartDate()
        updateChallengeProgress(
            ChallengeType.PERFECT_STREAK,
            weekStart,
            incrementBy = 1
        )
    }

    /**
     * 챌린지 진행도 업데이트
     */
    private suspend fun updateChallengeProgress(
        type: ChallengeType,
        weekStart: LocalDate,
        incrementBy: Int
    ): ChallengeUpdateResult? {
        val challengeId = "${WeeklyChallenge.generateChallengeId(type, weekStart)}_$currentUserId"
        val entity = challengeDao.getChallengeById(challengeId) ?: return null

        if (entity.isCompleted) return null // 이미 완료됨

        val newValue = (entity.currentValue + incrementBy).coerceAtMost(entity.targetValue)
        val isCompleted = newValue >= entity.targetValue
        val completedAt = if (isCompleted && !entity.isCompleted) System.currentTimeMillis() else entity.completedAt

        challengeDao.updateProgress(challengeId, newValue, isCompleted, completedAt)

        return ChallengeUpdateResult(
            challengeId = challengeId,
            type = type,
            title = entity.title,
            previousValue = entity.currentValue,
            newValue = newValue,
            targetValue = entity.targetValue,
            justCompleted = isCompleted && !entity.isCompleted,
            rewardExp = if (isCompleted && !entity.isCompleted) entity.rewardExp else 0,
            rewardPoints = if (isCompleted && !entity.isCompleted) entity.rewardPoints else 0
        )
    }

    /**
     * 보상 수령
     */
    suspend fun claimReward(challengeId: String): Boolean {
        val entity = challengeDao.getChallengeById(challengeId) ?: return false
        if (!entity.isCompleted || entity.isRewardClaimed) return false

        challengeDao.claimReward(challengeId, System.currentTimeMillis())
        Timber.d("Reward claimed for challenge: $challengeId")
        return true
    }

    /**
     * 수령하지 않은 보상 조회
     */
    suspend fun getUnclaimedChallenges(): List<WeeklyChallenge> {
        return challengeDao.getUnclaimedChallenges(currentUserId).map { it.toWeeklyChallenge() }
    }

    /**
     * 완료된 챌린지 개수 조회
     */
    suspend fun getCompletedChallengeCount(): Int {
        return challengeDao.getCompletedChallengeCount(currentUserId)
    }

    /**
     * 챌린지 히스토리 조회
     */
    suspend fun getChallengeHistory(limit: Int = 50): List<WeeklyChallenge> {
        return challengeDao.getChallengeHistory(currentUserId, limit).map { it.toWeeklyChallenge() }
    }
}

/**
 * 챌린지 업데이트 결과
 */
data class ChallengeUpdateResult(
    val challengeId: String,
    val type: ChallengeType,
    val title: String,
    val previousValue: Int,
    val newValue: Int,
    val targetValue: Int,
    val justCompleted: Boolean,
    val rewardExp: Int,
    val rewardPoints: Int
) {
    val progress: Float
        get() = if (targetValue > 0) (newValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f
}
