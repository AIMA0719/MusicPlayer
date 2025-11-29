package com.example.musicplayer.repository

import android.content.Context
import com.example.musicplayer.database.AppDatabase
import com.example.musicplayer.database.entity.LoginType
import com.example.musicplayer.database.entity.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(context: Context) {
    private val userDao = AppDatabase.getDatabase(context).userDao()

    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()
    }

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun updateLastLogin(userId: String) {
        userDao.updateLastLogin(userId)
    }

    suspend fun createGuestUser(): User {
        val guestUser = User(
            userId = "guest_${UUID.randomUUID()}",
            email = null,
            displayName = "게스트",
            profileImageUrl = null,
            loginType = LoginType.GUEST
        )
        insertUser(guestUser)
        return guestUser
    }

    suspend fun createGoogleUser(
        userId: String,
        email: String?,
        displayName: String,
        profileImageUrl: String?
    ): User {
        val googleUser = User(
            userId = userId,
            email = email,
            displayName = displayName,
            profileImageUrl = profileImageUrl,
            loginType = LoginType.GOOGLE
        )
        insertUser(googleUser)
        return googleUser
    }

    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteUserById(userId: String) {
        userDao.deleteUserById(userId)
    }
}
