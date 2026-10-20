package com.openmojang.launcher.data

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Microsoft / Xbox auth layer.
 *
 * Production path: MSAL PublicClientApplication + Xbox Live token exchange.
 * Until an Azure AD client ID is configured, demoSignIn() stores a local profile
 * so the rest of the launcher UI can be exercised end-to-end.
 */
class AuthManager(private val context: Context) {

    private val store = AccountStore(context)

    fun current(): MsAccount? = store.getAccount()

    fun isSignedIn(): Boolean = store.isSignedIn()

    /**
     * Placeholder interactive login.
     * Replace body with MSAL acquireToken once CLIENT_ID is set in BuildConfig.
     */
    suspend fun signIn(activity: Activity): Result<MsAccount> = withContext(Dispatchers.Main) {
        // Simulate network + consent UI delay
        withContext(Dispatchers.IO) { delay(600) }

        val account = MsAccount(
            gamertag = "OpenMojangPlayer",
            uuid = "00000000-0000-0000-0000-000000000001",
            accessToken = "demo-token",
            skinUrl = ""
        )
        store.save(account)
        Result.success(account)
    }

    fun signOut() {
        store.clear()
    }

    companion object {
        /** Set this in local.properties / BuildConfig when you register an Azure app. */
        const val MSAL_CLIENT_ID = "" // empty = demo mode
    }
}
