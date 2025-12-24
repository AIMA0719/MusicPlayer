package com.example.musicplayer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.data.ChallengeType
import com.example.musicplayer.data.WeeklyChallenge
import java.time.LocalDate

/**
 * 주간 챌린지 진행 상황 저장 엔티티
 */
@Entity(tableName = "weekly_challenges")
data class WeeklyChallengeEntity(
    @PrimaryKey
    val id: String,                    // 챌린지 고유 ID
    val userId: String,                // 사용자 ID
    val type: String,                  // ChallengeType name
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val rewardExp: Int,
    val rewardPoints: Int = 0,
    val startDate: String,             // LocalDate ISO format
    val endDate: String,               // LocalDate ISO format
    val isCompleted: Boolean = false,
    val isRewardClaimed: Boolean = false,
    val completedAt: Long? = null,     // 완료 시간 (epoch millis)
    val claimedAt: Long? = null        // 보상 수령 시간 (epoch millis)
) {
    /**
     * WeeklyChallenge 데이터 클래스로 변환
     */
    fun toWeeklyChallenge(): WeeklyChallenge {
        return WeeklyChallenge(
            id = id,
            type = try { ChallengeType.valueOf(type) } catch (e: Exception) { ChallengeType.SING_COUNT },
            title = title,
            description = description,
            targetValue = targetValue,
            currentValue = currentValue,
            rewardExp = rewardExp,
            rewardPoints = rewardPoints,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            isCompleted = isCompleted,
            isRewardClaimed = isRewardClaimed
        )
    }

    companion object {
        /**
         * WeeklyChallenge에서 Entity 생성
         */
        fun fromWeeklyChallenge(challenge: WeeklyChallenge, userId: String): WeeklyChallengeEntity {
            return WeeklyChallengeEntity(
                id = "${challenge.id}_$userId",
                userId = userId,
                type = challenge.type.name,
                title = challenge.title,
                description = challenge.description,
                targetValue = challenge.targetValue,
                currentValue = challenge.currentValue,
                rewardExp = challenge.rewardExp,
                rewardPoints = challenge.rewardPoints,
                startDate = challenge.startDate.toString(),
                endDate = challenge.endDate.toString(),
                isCompleted = challenge.isCompleted,
                isRewardClaimed = challenge.isRewardClaimed
            )
        }
    }
}
