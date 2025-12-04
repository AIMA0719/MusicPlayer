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
class GameManager(
    private val context: Context,
    private val userId: String = "guest"
) {

    private val database = AppDatabase.getDatabase(context)
    private val userLevelDao = database.userLevelDao()
    private val achievementDao = database.achievementDao()
    private val recordingHistoryDao = database.recordingHistoryDao()

    private val scope = CoroutineScope(Dispatchers.IO)

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

        // 도전과제 초기화 - 새로운 도전과제 추가
        val existingAchievements = achievementDao.getAllByUser(userId).first()
        val existingIds = existingAchievements.map { it.achievementId }.toSet()

        // 새로운 도전과제만 추가 (기존 것은 유지)
        val newAchievements = Achievement.values()
            .filter { it.id !in existingIds }
            .map { achievement ->
                AchievementEntity(
                    achievementId = achievement.id,
                    userId = userId,
                    maxProgress = achievement.maxProgress
                )
            }

        if (newAchievements.isNotEmpty()) {
            achievementDao.insertAll(newAchievements)
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
        LogManager.i("GameManager.updateUserStats called - userId: $userId, score: $score")

        val beforeUpdate = userLevelDao.getByUserIdSync(userId)
        LogManager.i("Before update - totalRecordings: ${beforeUpdate?.totalRecordings}")

        userLevelDao.incrementTotalRecordings(userId)

        val afterUpdate = userLevelDao.getByUserIdSync(userId)
        LogManager.i("After update - totalRecordings: ${afterUpdate?.totalRecordings}")

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

        // === 기본 도전과제 ===
        // 첫 녹음
        checkAndUnlock(Achievement.FIRST_RECORDING, 1)?.let { unlocked.add(it) }

        // 90점 돌파
        if (score >= 90) {
            checkAndUnlock(Achievement.FIRST_90_SCORE, 1)?.let { unlocked.add(it) }
        }

        // === 연속 도전과제 ===
        val userLevel = userLevelDao.getByUserIdSync(userId)
        if (userLevel != null) {
            if (userLevel.consecutiveDays >= 3) {
                checkAndUnlock(Achievement.CONSECUTIVE_3_DAYS, 3)?.let { unlocked.add(it) }
            }
            if (userLevel.consecutiveDays >= 7) {
                checkAndUnlock(Achievement.CONSECUTIVE_7_DAYS, 7)?.let { unlocked.add(it) }
            }
            if (userLevel.consecutiveDays >= 30) {
                checkAndUnlock(Achievement.CONSECUTIVE_30_DAYS, 30)?.let { unlocked.add(it) }
            }
        }

        // === 점수 도전과제 ===
        // 90점 이상 5곡
        val score90Count = recordingHistoryDao.getCountByMinScore(userId, 90)
        checkAndUnlock(Achievement.SCORE_90_5_SONGS, score90Count)?.let { unlocked.add(it) }

        // 95점 이상 3곡
        val score95Count = recordingHistoryDao.getCountByMinScore(userId, 95)
        checkAndUnlock(Achievement.SCORE_95_3_SONGS, score95Count)?.let { unlocked.add(it) }

        // 100점 달성
        if (score >= 100) {
            checkAndUnlock(Achievement.PERFECT_SCORE, 1)?.let { unlocked.add(it) }
        }

        // 10곡 연속 80점 이상 (최근 10곡 체크)
        val recent10 = recordingHistoryDao.getRecentRecordings(userId, 10)
        if (recent10.size >= 10 && recent10.all { it.totalScore >= 80 }) {
            checkAndUnlock(Achievement.CONSISTENCY_MASTER, 10)?.let { unlocked.add(it) }
        }

        // === 난이도 도전과제 ===
        // 모든 난이도 시도
        val triedDifficultyCount = recordingHistoryDao.getTriedDifficultyCount(userId)
        checkAndUnlock(Achievement.TRY_ALL_DIFFICULTY, triedDifficultyCount)?.let { unlocked.add(it) }

        // 고수 모드로 80점 이상
        if (recordingHistory.difficulty == "HARD" && score >= 80) {
            checkAndUnlock(Achievement.HARD_MODE_MASTER, 1)?.let { unlocked.add(it) }
        }

        // 초고수 모드로 70점 이상
        if (recordingHistory.difficulty == "VERY_HARD" && score >= 70) {
            checkAndUnlock(Achievement.VERY_HARD_CLEAR, 1)?.let { unlocked.add(it) }
        }

        // === 특수 도전과제 ===
        // 비브라토
        if (recordingHistory.hasVibrato) {
            checkAndIncrement(Achievement.VIBRATO_MASTER)?.let { unlocked.add(it) }
        }

        // 같은 곡 10번
        val songHistory = recordingHistoryDao.getHistoryBySong(userId, songName).first()
        checkAndUnlock(Achievement.SONG_MASTER, songHistory.size)?.let { unlocked.add(it) }

        // 시간대 체크
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = recordingHistory.timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // 아침 새 (오전 6시~9시)
        if (hour in 6..9) {
            checkAndUnlock(Achievement.MORNING_BIRD, 1)?.let { unlocked.add(it) }
        }

        // 올빼미 (자정~새벽 3시)
        if (hour in 0..3) {
            checkAndUnlock(Achievement.NIGHT_OWL, 1)?.let { unlocked.add(it) }
        }

        // === 횟수 도전과제 ===
        val totalCount = recordingHistoryDao.getTotalRecordingCount(userId)
        checkAndUnlock(Achievement.RECORDING_10, totalCount)?.let { unlocked.add(it) }
        checkAndUnlock(Achievement.RECORDING_50, totalCount)?.let { unlocked.add(it) }
        checkAndUnlock(Achievement.RECORDING_100, totalCount)?.let { unlocked.add(it) }
        checkAndUnlock(Achievement.RECORDING_500, totalCount)?.let { unlocked.add(it) }

        // === 히든 도전과제 ===
        // 럭키 세븐 (정확히 77점)
        if (score == 77) {
            checkAndUnlock(Achievement.LUCKY_7, 1)?.let { unlocked.add(it) }
        }

        // 럭키 에이트 (정확히 88점)
        if (score == 88) {
            checkAndUnlock(Achievement.LUCKY_8, 1)?.let { unlocked.add(it) }
        }

        // 아깝다! (정확히 99점)
        if (score == 99) {
            checkAndUnlock(Achievement.ALMOST_PERFECT, 1)?.let { unlocked.add(it) }
        }

        // 용감한 도전 (50점 미만)
        if (score < 50) {
            checkAndUnlock(Achievement.BRAVE_ATTEMPT, 1)?.let { unlocked.add(it) }
        }

        // 자정의 가수 (정확히 자정)
        if (hour == 0 && minute == 0) {
            checkAndUnlock(Achievement.MIDNIGHT_SINGER, 1)?.let { unlocked.add(it) }
        }

        // 새벽 기상 (새벽 5시~6시)
        if (hour in 5..6) {
            checkAndUnlock(Achievement.EARLY_BIRD, 1)?.let { unlocked.add(it) }
        }

        // 점심시간 가수 (12~13시에 5곡) - 점심시간에 녹음한 경우 체크
        if (hour in 12..13) {
            // 오늘의 점심시간 범위
            val todayLunchStart = Calendar.getInstance().apply {
                timeInMillis = recordingHistory.timestamp
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayLunchEnd = todayLunchStart + 2 * 60 * 60 * 1000 // 2시간 (12시~14시)
            val lunchCount = recordingHistoryDao.getRecordingCountInTimeRange(userId, todayLunchStart, todayLunchEnd)
            checkAndUnlock(Achievement.LUNCH_TIME_SINGER, lunchCount)?.let { unlocked.add(it) }
        }

        // 시간 여행자 (새벽/아침/점심/저녁/밤 모두 녹음) - 진행도 체크
        checkTimeOfDayProgress()?.let { unlocked.add(it) }

        // 트리플 크라운 (3곡 연속 95점 이상)
        val recent3 = recordingHistoryDao.getRecentRecordings(userId, 3)
        if (recent3.size >= 3 && recent3.all { it.totalScore >= 95 }) {
            checkAndUnlock(Achievement.TRIPLE_CROWN, 3)?.let { unlocked.add(it) }
        }

        // 스피드 러너 (1시간 내 5곡)
        val oneHourAgo = recordingHistory.timestamp - 60 * 60 * 1000
        val countLastHour = recordingHistoryDao.getRecordingCountInTimeRange(userId, oneHourAgo, recordingHistory.timestamp)
        checkAndUnlock(Achievement.SPEED_DEMON, countLastHour)?.let { unlocked.add(it) }

        // 마라톤 가수 (하루에 20곡)
        val todayStart = Calendar.getInstance().apply {
            timeInMillis = recordingHistory.timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val todayCount = recordingHistoryDao.getRecordingCountInTimeRange(userId, todayStart, todayEnd)
        checkAndUnlock(Achievement.MARATHON_SINGER, todayCount)?.let { unlocked.add(it) }

        // 다양성의 달인 (서로 다른 10곡)
        val uniqueSongCount = recordingHistoryDao.getUniqueSongCount(userId)
        checkAndUnlock(Achievement.DIVERSITY_MASTER, uniqueSongCount)?.let { unlocked.add(it) }

        // 재기의 달인 (40점대 이후 90점 이상)
        val recent2 = recordingHistoryDao.getRecentRecordings(userId, 2)
        if (recent2.size >= 2) {
            val previousScore = recent2[1].totalScore // 이전 녹음
            if (previousScore in 40..49 && score >= 90) {
                checkAndUnlock(Achievement.COMEBACK, 1)?.let { unlocked.add(it) }
            }
        }

        // 주말 전사 (주말에만 10곡)
        checkWeekendWarrior()?.let { unlocked.add(it) }

        return unlocked
    }

    /**
     * 시간 여행자 도전과제 체크
     * 새벽(0-5), 아침(6-9), 점심(12-13), 저녁(18-21), 밤(21-24) 모두 녹음
     */
    private suspend fun checkTimeOfDayProgress(): AchievementEntity? {
        val allRecordings = recordingHistoryDao.getAllByUser(userId).first()
        val timeSlots = mutableSetOf<com.example.musicplayer.entity.TimeOfDay>()

        allRecordings.forEach { recording ->
            val cal = Calendar.getInstance().apply { timeInMillis = recording.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            when (hour) {
                in 0..5 -> timeSlots.add(com.example.musicplayer.entity.TimeOfDay.DAWN)
                in 6..9 -> timeSlots.add(com.example.musicplayer.entity.TimeOfDay.MORNING)
                in 12..13 -> timeSlots.add(com.example.musicplayer.entity.TimeOfDay.LUNCH)
                in 18..21 -> timeSlots.add(com.example.musicplayer.entity.TimeOfDay.EVENING)
                in 21..23 -> timeSlots.add(com.example.musicplayer.entity.TimeOfDay.NIGHT)
            }
        }

        return checkAndUnlock(Achievement.TIME_TRAVELER, timeSlots.size)
    }

    /**
     * 주말 전사 도전과제 체크
     */
    private suspend fun checkWeekendWarrior(): AchievementEntity? {
        val allRecordings = recordingHistoryDao.getAllByUser(userId).first()
        val weekendCount = allRecordings.count { recording ->
            val cal = Calendar.getInstance().apply { timeInMillis = recording.timestamp }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }

        return checkAndUnlock(Achievement.WEEKEND_WARRIOR, weekendCount)
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
