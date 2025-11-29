package com.example.musicplayer.manager

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleAuthManager {
    private const val RC_SIGN_IN = 9001
    // Scope for YouTube Data API (Read Only)
    private const val SCOPE_YOUTUBE_READONLY = "https://www.googleapis.com/auth/youtube.readonly"

    private lateinit var googleSignInClient: GoogleSignInClient

    fun init(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_YOUTUBE_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>): GoogleSignInAccount? {
        return try {
            completedTask.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            LogManager.e("Google Sign-In failed with status code: ${e.statusCode}")
            when (e.statusCode) {
                10 -> LogManager.e("Sign-in error: Developer error. Check SHA-1/OAuth client configuration.")
                7 -> LogManager.e("Sign-in error: Network error.")
                8 -> LogManager.e("Sign-in error: Internal error.")
                else -> LogManager.e("Sign-in error: ${e.message}")
            }
            e.printStackTrace()
            null
        }
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun signOut(onComplete: () -> Unit) {
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut().addOnCompleteListener {
                onComplete()
            }
        }
    }

    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                val scope = "oauth2:$SCOPE_YOUTUBE_READONLY"
                GoogleAuthUtil.getToken(context, account.account!!, scope)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
