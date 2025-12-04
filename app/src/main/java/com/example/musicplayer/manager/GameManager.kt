package com.example.musicplayer.manager

import android.content.Context
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.entity.Achievement
import com.example.musicplayer.entity.AchievementEntity
import com.example.musicplayer.entity.LevelSystem
import com.example.musicplayer.entity.RecordingHistoryEntity
import com.example.musicplayer.entity.UserLevelEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 게임 요소 매니저
 * - 레벨 시스템
 * - 도전과제
 * - 경험치 관리
 */
class GameManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val userLevelDao = database.userLevelDao()
    private val achievementDao = database.achievementDao()
    private val recordingHistoryDao = database.recordingHistoryDao()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val userId = "guest" // 추후 로그인 시스템 연동

    /**
     * 초기화 - 사용자 레벨 및 도전과제 생성
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // 사용자 레벨이 없으면 생성
        var userLevel = userLevelDao.getByUserIdSync(userId)
        if (userLevel == null) {
            userLevel = UserLevelEntity(userId = userId)
            userLevelDao.insert(userLevel)
        }

        // 도전과제 초기화
        val existingAchievements = achievementDao.getAllByUser(userId).first()
        if (existingAchievements.isEmpty()) {
            val achievements = Achievement.values().map { achievement ->
                AchievementEntity(
                    achievementId = achievement.id,
                    userId = userId,
                    maxProgress = achievement.maxProgress
                )
            }
            achievementDao.insertAll(achievements)
        }
    }

    /**
     * 녹음 완료 시 호출 - 경험치 부여 및 도전과제 체크
     */
    suspend fun onRecordingCompleted(
        songName: String,
        score: Int,
        difficulty: String,
        recordingHistory: RecordingHistoryEntity
    ): GameReward = withContext(Dispatchers.IO) {
        val reward = GameReward()

        // RecordingHistory 저장
        recordingHistoryDao.insert(recordingHistory)

        // 경험치 계산 및 부여
        val exp = LevelSystem.calculateExpGain(score, difficulty)
        val levelUp = addExperience(exp)

        reward.exp = exp
        reward.leveledUp = levelUp.leveledUp
        reward.newLevel = levelUp.newLevel

        // 통계 업데이트
        updateUserStats(score)

        // 도전과제 체크
        reward.unlockedAchievements = checkAchievements(songName, score, recordingHistory)

        reward
    }

    /**
     * 경험치 추가 및 레벨업 처리
     */
    private suspend fun addExperience(exp: Int): LevelUpResult {
        val userLevel = userLevelDao.getByUserIdSync(userId) ?: return LevelUpResult()
        val newExp = userLevel.experience + exp
        var newLevel = userLevel.level
        var remainingExp = newExp

        // 레벨업 체크
        while (remainingExp >= LevelSystem.getRequiredExp(newLevel)) {
            remainingExp -= LevelSystem.getRequiredExp(newLevel)
            newLevel++
        }

        userLevelDao.updateLevel(userId, newLevel, remainingExp)

        return LevelUpResult(
            leveledUp = newLevel > userLevel.level,
            newLevel = newLevel,
            oldLevel = userLevel.level
        )
    }

    /**
     * 사용자 통계 업데이트
     */
    private suspend fun updateUserStats(score: Int) {
        userLevelDao.incrementTotalRecordings(userId)
        userLevelDao.updateHighestScore(userId, score)

        // 연속 녹음 일수 체크
        updateConsecutiveDays()
    }

    /**
     * 연속 녹음 일수 업데이트
     */
    private suspend fun updateConsecutiveDays() {
        val userLevel = userLevelDao.getByUserIdSync(userId) ?: return
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = userLevel.lastRecordingDate
        val yesterday = today - 24 * 60 * 60 * 1000

        val newDays = when {
            lastDate == 0L -> 1 // 첫 녹음
            lastDate >= today -> userLevel.consecutiveDays // 오늘 이미 녹음함
            lastDate >= yesterday -> userLevel.consecutiveDays + 1 // 어제 녹음함 (연속)
            else -> 1 // 끊김
        }

        userLevelDao.updateConsecutiveDays(userId, newDays, today)
    }

    /**
     * 도전과제 체크
     */
    private suspend fun checkAchievements(
        songName: String,
        score: Int,
        recordingHistory: RecordingHistoryEntity
    ): List<AchievementEntity> {
        val unlocked = mutableListOf<AchievementEntity>()

        // 첫 녹음
        checkAndUnlock(Achievement.FIRST_RECORDING, 1)?.let { unlocked.add(it) }

        // 90점 돌파
        if (score >= 90) {
            checkAndUnlock(Achievement.FIRST_90_SCORE, 1)?.let { unlocked.add(it) }
        }

        // 100점 달성
        if (score >= 100) {
            checkAndUnlock(Achievement.PERFECT_SCORE, 1)?.let { unlocked.add(it) }
        }

        // 비브라토
        if (recordingHistory.hasVibrato) {
            checkAndIncrement(Achievement.VIBRATO_MASTER)?.let { unlocked.add(it) }
        }

        // 연속 녹음 일수
        val userLevel = userLevelDao.getByUserIdSync(userId)
        if (userLevel != null) {
            if (userLevel.consecutiveDays >= 3) {
                checkAndUnlock(Achievement.CONSECUTIVE_3_DAYS, 3)?.let { unlocked.add(it) }
            }
            if (userLevel.consecutiveDays >= 7) {
                checkAndUnlock(Achievement.CONSECUTIVE_7_DAYS, 7)?.let { unlocked.add(it) }
            }
        }

        // 90점 이상 5곡
        val highScoreCount = recordingHistoryDao.getCountByMinScore(userId, 90)
        checkAndUnlock(Achievement.SCORE_90_5_SONGS, highScoreCount)?.let { unlocked.add(it) }

        // 총 녹음 수
        val totalCount = recordingHistoryDao.getTotalRecordingCount(userId)
        checkAndUnlock(Achievement.RECORDING_10, totalCount)?.let { unlocked.add(it) }
        checkAndUnlock(Achievement.RECORDING_50, totalCount)?.let { unlocked.add(it) }
        checkAndUnlock(Achievement.RECORDING_100, totalCount)?.let { unlocked.add(it) }

        // 같은 곡 10번
        val songHistory = recordingHistoryDao.getHistoryBySong(userId, songName).first()
        checkAndUnlock(Achievement.SONG_MASTER, songHistory.size)?.let { unlocked.add(it) }

        return unlocked
    }

    /**
     * 도전과제 진행도 증가 및 달성 체크
     */
    private suspend fun checkAndIncrement(achievement: Achievement): AchievementEntity? {
        val entity = achievementDao.getById(userId, achievement.id)
        if (entity != null && !entity.isUnlocked) {
            val newProgress = entity.progress + 1
            achievementDao.updateProgress(userId, achievement.id, newProgress)

            if (newProgress >= entity.maxProgress) {
                achievementDao.unlock(userId, achievement.id)
                return entity.copy(isUnlocked = true, progress = newProgress)
            }
        }
        return null
    }

    /**
     * 도전과제 달성 체크 (현재 진행도로)
     */
    private suspend fun checkAndUnlock(achievement: Achievement, currentProgress: Int): AchievementEntity? {
        val entity = achievementDao.getById(userId, achievement.id)
        if (entity != null && !entity.isUnlocked && currentProgress >= entity.maxProgress) {
            achievementDao.updateProgress(userId, achievement.id, currentProgress)
            achievementDao.unlock(userId, achievement.id)
            return entity.copy(isUnlocked = true, progress = currentProgress)
        }
        return null
    }

    /**
     * 테마 변경
     */
    suspend fun changeTheme(theme: String) = withContext(Dispatchers.IO) {
        userLevelDao.updateTheme(userId, theme)
    }
}

/**
 * 게임 보상
 */
data class GameReward(
    var exp: Int = 0,
    var leveledUp: Boolean = false,
    var newLevel: Int = 1,
    var unlockedAchievements: List<AchievementEntity> = emptyList()
)

/**
 * 레벨업 결과
 */
data class LevelUpResult(
    val leveledUp: Boolean = false,
    val newLevel: Int = 1,
    val oldLevel: Int = 1
)
