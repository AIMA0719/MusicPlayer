package com.example.musicplayer.domain.model

/**
 * 사용자 레벨 도메인 모델
 */
data class UserLevel(
    val id: Int = 0,
    val userId: String,
    val level: Int = 1,
    val currentExp: Int = 0,
    val totalExp: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * 다음 레벨까지 필요한 경험치
     */
    val expForNextLevel: Int
        get() = calculateExpForLevel(level + 1)

    /**
     * 현재 레벨 진행률 (0.0 ~ 1.0)
     */
    val levelProgress: Float
        get() {
            val currentLevelExp = calculateExpForLevel(level)
            val nextLevelExp = calculateExpForLevel(level + 1)
            val expInCurrentLevel = totalExp - currentLevelExp
            val expNeeded = nextLevelExp - currentLevelExp
            return if (expNeeded > 0) expInCurrentLevel.toFloat() / expNeeded else 0f
        }

    companion object {
        /**
         * 특정 레벨에 도달하기 위해 필요한 총 경험치
         */
        fun calculateExpForLevel(level: Int): Int {
            return when {
                level <= 1 -> 0
                level <= 10 -> (level - 1) * 100
                level <= 20 -> 900 + (level - 10) * 150
                level <= 30 -> 2400 + (level - 20) * 200
                else -> 4400 + (level - 30) * 300
            }
        }

        /**
         * 레벨 이름 반환
         */
        fun getLevelTitle(level: Int): String {
            return when {
                level < 5 -> "노래방 새싹"
                level < 10 -> "노래방 초보"
                level < 20 -> "노래방 중수"
                level < 30 -> "노래방 고수"
                level < 40 -> "노래방 달인"
                level < 50 -> "노래방 마스터"
                else -> "노래방 전설"
            }
        }
    }
}
