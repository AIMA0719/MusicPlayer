package com.example.musicplayer.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 주간 챌린지 타입
 */
enum class ChallengeType(val displayName: String, val description: String) {
    SCORE_ABOVE_90("고득점 챌린지", "90점 이상 달성"),
    SING_COUNT("노래 부르기", "노래 N곡 부르기"),
    PERFECT_STREAK("연속 도전", "연속 N일 노래방 이용"),
    GENRE_MASTER("장르 마스터", "특정 장르 노래 N곡"),
    NEW_SONG("신곡 도전", "처음 부르는 노래 N곡"),
    HIGH_DIFFICULTY("고난이도 도전", "어려운 난이도로 N곡");
}

/**
 * 주간 챌린지 데이터
 */
data class WeeklyChallenge(
    val id: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,          // 목표 값 (90점, 10곡 등)
    val currentValue: Int = 0,     // 현재 진행 값
    val rewardExp: Int,            // 보상 경험치
    val rewardPoints: Int = 0,     // 보상 포인트
    val startDate: LocalDate,      // 시작일
    val endDate: LocalDate,        // 종료일 (일요일)
    val isCompleted: Boolean = false,
    val isRewardClaimed: Boolean = false
) {
    val progress: Float
        get() = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isActive: Boolean
        get() {
            val today = LocalDate.now()
            return today in startDate..endDate
        }

    val remainingDays: Int
        get() {
            val today = LocalDate.now()
            return if (today.isAfter(endDate)) 0
            else endDate.toEpochDay().toInt() - today.toEpochDay().toInt()
        }

    companion object {
        /**
         * 이번 주 시작일 (월요일) 가져오기
         */
        fun getWeekStartDate(): LocalDate {
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

        /**
         * 이번 주 종료일 (일요일) 가져오기
         */
        fun getWeekEndDate(): LocalDate {
            return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        }

        /**
         * 주간 챌린지 ID 생성 (주 단위로 고유)
         */
        fun generateChallengeId(type: ChallengeType, weekStart: LocalDate): String {
            return "${type.name}_${weekStart.year}_W${weekStart.get(java.time.temporal.WeekFields.ISO.weekOfYear())}"
        }
    }
}

/**
 * 주간 챌린지 생성 팩토리
 */
object WeeklyChallengeFactory {

    /**
     * 이번 주 챌린지 목록 생성
     */
    fun generateWeeklyChallenges(): List<WeeklyChallenge> {
        val startDate = WeeklyChallenge.getWeekStartDate()
        val endDate = WeeklyChallenge.getWeekEndDate()

        return listOf(
            // 고득점 챌린지
            WeeklyChallenge(
                id = WeeklyChallenge.generateChallengeId(ChallengeType.SCORE_ABOVE_90, startDate),
                type = ChallengeType.SCORE_ABOVE_90,
                title = "90점 이상 3회 달성",
                description = "90점 이상의 점수를 3회 달성하세요",
                targetValue = 3,
                rewardExp = 150,
                rewardPoints = 50,
                startDate = startDate,
                endDate = endDate
            ),
            // 노래 부르기 챌린지
            WeeklyChallenge(
                id = WeeklyChallenge.generateChallengeId(ChallengeType.SING_COUNT, startDate),
                type = ChallengeType.SING_COUNT,
                title = "이번 주 10곡 부르기",
                description = "이번 주에 총 10곡을 불러보세요",
                targetValue = 10,
                rewardExp = 200,
                rewardPoints = 100,
                startDate = startDate,
                endDate = endDate
            ),
            // 연속 도전 챌린지
            WeeklyChallenge(
                id = WeeklyChallenge.generateChallengeId(ChallengeType.PERFECT_STREAK, startDate),
                type = ChallengeType.PERFECT_STREAK,
                title = "5일 연속 이용",
                description = "5일 연속으로 노래방을 이용하세요",
                targetValue = 5,
                rewardExp = 300,
                rewardPoints = 150,
                startDate = startDate,
                endDate = endDate
            ),
            // 신곡 도전 챌린지
            WeeklyChallenge(
                id = WeeklyChallenge.generateChallengeId(ChallengeType.NEW_SONG, startDate),
                type = ChallengeType.NEW_SONG,
                title = "새로운 노래 5곡 도전",
                description = "처음 불러보는 노래 5곡에 도전하세요",
                targetValue = 5,
                rewardExp = 250,
                rewardPoints = 80,
                startDate = startDate,
                endDate = endDate
            )
        )
    }
}
