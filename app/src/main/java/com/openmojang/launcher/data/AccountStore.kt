package com.openmojang.launcher.data

import android.content.Context
import android.content.SharedPreferences

data class MsAccount(
    val gamertag: String,
    val uuid: String,
    val accessToken: String = "",
    val refreshToken: String = "",
    val skinUrl: String = "",
    val gamerscore: Int = -1
)

data class XboxAchievement(
    val id: String,
    val name: String,
    val description: String,
    val progressState: String,
    val percentComplete: Int
)

data class LiveProfile(
    val gamertag: String,
    val xuid: String,
    val gamerscore: Int,
    val achievements: List<XboxAchievement>,
    val unlockedCount: Int,
    val totalCount: Int
)

/**
 * Lightweight local store for the signed-in Microsoft / Xbox profile.
 * Tokens stay on device only.
 */
class AccountStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("open_mojang_account", Context.MODE_PRIVATE)

    fun isSignedIn(): Boolean = prefs.getString(KEY_GAMERTAG, null) != null

    fun getAccount(): MsAccount? {
        val tag = prefs.getString(KEY_GAMERTAG, null) ?: return null
        return MsAccount(
            gamertag = tag,
            uuid = prefs.getString(KEY_UUID, "") ?: "",
            accessToken = prefs.getString(KEY_ACCESS, "") ?: "",
            refreshToken = prefs.getString(KEY_REFRESH, "") ?: "",
            skinUrl = prefs.getString(KEY_SKIN, "") ?: "",
            gamerscore = prefs.getInt(KEY_GS, -1)
        )
    }

    fun save(account: MsAccount) {
        prefs.edit()
            .putString(KEY_GAMERTAG, account.gamertag)
            .putString(KEY_UUID, account.uuid)
            .putString(KEY_ACCESS, account.accessToken)
            .putString(KEY_REFRESH, account.refreshToken)
            .putString(KEY_SKIN, account.skinUrl)
            .putInt(KEY_GS, account.gamerscore)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_GAMERTAG = "gamertag"
        private const val KEY_UUID = "uuid"
        private const val KEY_ACCESS = "access"
        private const val KEY_REFRESH = "refresh"
        private const val KEY_SKIN = "skin"
        private const val KEY_GS = "gamerscore"
    }
}
