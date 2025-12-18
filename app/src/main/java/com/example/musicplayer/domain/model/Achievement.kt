package com.example.musicplayer.domain.model

/**
 * 도전과제 도메인 모델
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val maxProgress: Int,
    val icon: String,
    val isHidden: Boolean = false,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val unlockedAt: Long? = null
)

/**
 * 도전과제 정의
 */
enum class AchievementType(
    val id: String,
    val title: String,
    val description: String,
    val maxProgress: Int,
    val icon: String,
    val isHidden: Boolean = false
) {
    // === 기본 도전과제 ===
    FIRST_RECORDING("FIRST_RECORDING", "첫 녹음", "첫 번째 점수 내기를 완료하세요", 1, "🎤"),
    FIRST_90_SCORE("FIRST_90_SCORE", "90점 돌파", "90점 이상을 달성하세요", 1, "🌟"),

    // === 연속 도전과제 ===
    CONSECUTIVE_3_DAYS("CONSECUTIVE_3_DAYS", "3일 연속", "3일 연속으로 점수 내기를 하세요", 3, "🔥"),
    CONSECUTIVE_7_DAYS("CONSECUTIVE_7_DAYS", "일주일 챌린지", "7일 연속으로 점수 내기를 하세요", 7, "💪"),
    CONSECUTIVE_30_DAYS("CONSECUTIVE_30_DAYS", "한 달 챌린저", "30일 연속으로 점수 내기를 하세요", 30, "🎖️"),

    // === 점수 도전과제 ===
    SCORE_90_5_SONGS("SCORE_90_5_SONGS", "고득점 마스터", "90점 이상을 5회 달성하세요", 5, "⭐"),
    SCORE_95_3_SONGS("SCORE_95_3_SONGS", "완벽주의자", "95점 이상을 3회 달성하세요", 3, "💯"),
    PERFECT_SCORE("PERFECT_SCORE", "완벽한 점수", "100점을 달성하세요", 1, "👑"),
    CONSISTENCY_MASTER("CONSISTENCY_MASTER", "안정적인 실력", "10회 연속 80점 이상 달성", 10, "📈"),

    // === 난이도 도전과제 ===
    TRY_ALL_DIFFICULTY("TRY_ALL_DIFFICULTY", "난이도 마스터", "모든 난이도로 점수 내기를 시도해보세요", 5, "🎯"),
    HARD_MODE_MASTER("HARD_MODE_MASTER", "고수의 실력", "고수 모드로 80점 이상 달성", 1, "💪"),
    VERY_HARD_CLEAR("VERY_HARD_CLEAR", "초고수 달성", "초고수 모드로 70점 이상 달성", 1, "🔥"),

    // === 특수 도전과제 ===
    VIBRATO_MASTER("VIBRATO_MASTER", "비브라토 마스터", "점수 내기에서 비브라토를 10회 성공하세요", 10, "🎵"),
    SONG_MASTER("SONG_MASTER", "곡 정복자", "같은 곡으로 점수 내기를 10회 하세요", 10, "🎼"),
    MORNING_BIRD("MORNING_BIRD", "아침 새", "오전 6시~9시에 점수 내기", 1, "🌅"),
    NIGHT_OWL("NIGHT_OWL", "올빼미", "자정~새벽 3시에 점수 내기", 1, "🦉"),

    // === 횟수 도전과제 ===
    RECORDING_10("RECORDING_10", "노래방 단골", "점수 내기를 총 10회 하세요", 10, "🎙️"),
    RECORDING_50("RECORDING_50", "열정 가수", "점수 내기를 총 50회 하세요", 50, "🎶"),
    RECORDING_100("RECORDING_100", "프로 가수", "점수 내기를 총 100회 하세요", 100, "🏆"),
    RECORDING_500("RECORDING_500", "전설의 가수", "점수 내기를 총 500회 하세요", 500, "🌟"),

    // === 히든 도전과제 ===
    LUCKY_7("LUCKY_7", "럭키 세븐", "정확히 77점을 달성하세요", 1, "🍀", true),
    LUCKY_8("LUCKY_8", "럭키 에이트", "정확히 88점을 달성하세요", 1, "🎰", true),
    ALMOST_PERFECT("ALMOST_PERFECT", "아깝다!", "정확히 99점을 달성하세요", 1, "😭", true),
    BRAVE_ATTEMPT("BRAVE_ATTEMPT", "용감한 도전", "50점 미만을 받으세요", 1, "🦁", true),
    MIDNIGHT_SINGER("MIDNIGHT_SINGER", "자정의 가수", "정확히 자정(00:00)에 점수 내기 시작", 1, "🌙", true),
    EARLY_BIRD("EARLY_BIRD", "새벽 기상", "새벽 5시~6시에 점수 내기", 1, "🌄", true),
    LUNCH_TIME_SINGER("LUNCH_TIME_SINGER", "점심시간 가수", "점심시간(12~13시)에 점수 내기 5회", 5, "🍱", true),
    TIME_TRAVELER("TIME_TRAVELER", "시간 여행자", "새벽/아침/점심/저녁/밤 모두 점수 내기", 5, "⏰", true),
    TRIPLE_CROWN("TRIPLE_CROWN", "트리플 크라운", "3회 연속 95점 이상", 3, "🎩", true),
    SPEED_DEMON("SPEED_DEMON", "스피드 러너", "1시간 내 점수 내기 5회", 5, "⚡", true),
    MARATHON_SINGER("MARATHON_SINGER", "마라톤 가수", "하루에 점수 내기 20회", 20, "🏃", true),
    DIVERSITY_MASTER("DIVERSITY_MASTER", "다양성의 달인", "서로 다른 10곡으로 점수 내기", 10, "🌈", true),
    COMEBACK("COMEBACK", "재기의 달인", "40점대 이후 바로 90점 이상 달성", 1, "💪", true),
    WEEKEND_WARRIOR("WEEKEND_WARRIOR", "주말 전사", "주말에 점수 내기 10회", 10, "⚔️", true);

    fun toAchievement(isUnlocked: Boolean = false, progress: Int = 0, unlockedAt: Long? = null) = Achievement(
        id = id,
        title = title,
        description = description,
        maxProgress = maxProgress,
        icon = icon,
        isHidden = isHidden,
        isUnlocked = isUnlocked,
        progress = progress,
        unlockedAt = unlockedAt
    )
}
