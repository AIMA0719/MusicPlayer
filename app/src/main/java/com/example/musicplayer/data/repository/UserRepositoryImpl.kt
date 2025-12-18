package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.database.dao.UserDao
import com.example.musicplayer.data.local.database.entity.UserEntity
import com.example.musicplayer.data.local.preference.PreferenceManager
import com.example.musicplayer.domain.model.LoginType
import com.example.musicplayer.domain.model.User
import com.example.musicplayer.domain.repository.UserRepository
import com.example.musicplayer.data.local.database.LoginType as DbLoginType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val preferenceManager: PreferenceManager
) : UserRepository {

    override suspend fun getUser(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    override fun getCurrentUserId(): String? {
        return preferenceManager.userId
    }

    override fun isLoggedIn(): Boolean {
        return preferenceManager.isLoggedIn
    }

    override suspend fun login(
        userId: String,
        email: String?,
        displayName: String,
        profileImageUrl: String?,
        loginType: LoginType
    ) {
        val user = User(
            userId = userId,
            email = email,
            displayName = displayName,
            profileImageUrl = profileImageUrl,
            loginType = loginType
        )
        saveUser(user)
        preferenceManager.saveLoginInfo(userId)
    }

    override suspend fun logout() {
        preferenceManager.logout()
    }

    private fun UserEntity.toDomain(): User {
        return User(
            userId = userId,
            email = email,
            displayName = displayName,
            profileImageUrl = profileImageUrl,
            loginType = loginType.toDomainLoginType(),
            createdAt = createdAt,
            lastLoginAt = lastLoginAt
        )
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            userId = userId,
            email = email,
            displayName = displayName,
            profileImageUrl = profileImageUrl,
            loginType = loginType.toDbLoginType(),
            createdAt = createdAt,
            lastLoginAt = lastLoginAt
        )
    }

    private fun DbLoginType.toDomainLoginType(): LoginType {
        return when (this) {
            DbLoginType.GUEST -> LoginType.GUEST
            DbLoginType.GOOGLE -> LoginType.GOOGLE
            DbLoginType.KAKAO -> LoginType.KAKAO
            DbLoginType.NAVER -> LoginType.NAVER
        }
    }

    private fun LoginType.toDbLoginType(): DbLoginType {
        return when (this) {
            LoginType.GUEST -> DbLoginType.GUEST
            LoginType.GOOGLE -> DbLoginType.GOOGLE
            LoginType.KAKAO -> DbLoginType.KAKAO
            LoginType.NAVER -> DbLoginType.NAVER
        }
    }
}
