package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.UserLevelDao
import com.example.musicplayer.data.local.database.entity.UserLevelEntity
import com.example.musicplayer.domain.model.UserLevel
import com.example.musicplayer.domain.repository.UserLevelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLevelRepositoryImpl @Inject constructor(
    private val userLevelDao: UserLevelDao
) : UserLevelRepository {

    override fun getUserLevel(userId: String): Flow<UserLevel?> {
        return userLevelDao.getByUserId(userId).map { it?.toDomain() }
    }

    override suspend fun saveUserLevel(userLevel: UserLevel) {
        userLevelDao.insert(userLevel.toEntity())
    }

    override suspend fun addExperience(userId: String, exp: Int): UserLevel {
        userLevelDao.addExperience(userId, exp)
        return userLevelDao.getByUserIdSync(userId)?.toDomain()
            ?: throw IllegalStateException("User level not found")
    }

    override suspend fun checkLevelUp(userId: String): Int? {
        val currentLevel = userLevelDao.getByUserIdSync(userId) ?: return null
        val newLevel = calculateLevelFromExp(currentLevel.experience)
        return if (newLevel > currentLevel.level) {
            userLevelDao.updateLevel(userId, newLevel, currentLevel.experience)
            newLevel
        } else {
            null
        }
    }

    override suspend fun initializeUserLevel(userId: String) {
        val existing = userLevelDao.getByUserIdSync(userId)
        if (existing == null) {
            userLevelDao.insert(
                UserLevelEntity(
                    userId = userId,
                    level = 1,
                    experience = 0
                )
            )
        }
    }

    private fun calculateLevelFromExp(totalExp: Int): Int {
        var level = 1
        while (UserLevel.calculateExpForLevel(level + 1) <= totalExp) {
            level++
        }
        return level
    }

    private fun UserLevelEntity.toDomain(): UserLevel {
        return UserLevel(
            id = 0,
            userId = userId,
            level = level,
            currentExp = experience,
            totalExp = experience
        )
    }

    private fun UserLevel.toEntity(): UserLevelEntity {
        return UserLevelEntity(
            userId = userId,
            level = level,
            experience = currentExp
        )
    }
}
