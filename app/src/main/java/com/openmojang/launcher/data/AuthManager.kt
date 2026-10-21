package com.openmojang.launcher.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DeviceCodeSession(
    val userCode: String,
    val deviceCode: String,
    val verificationUri: String,
    val intervalSec: Int,
    val expiresIn: Int
)

/**
 * Microsoft login via login.live.com device code (Xbox client) → Xbox user token → XSTS.
 */
class AuthManager(private val context: Context) {

    private val store = AccountStore(context)
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun current(): MsAccount? = store.getAccount()
    fun isSignedIn(): Boolean = store.isSignedIn()
    fun signOut() { store.clear() }

    suspend fun startDeviceCode(): Result<DeviceCodeSession> = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .add("response_type", "device_code")
                .build()
            val req = Request.Builder()
                .url("https://login.live.com/oauth20_connect.srf")
                .post(body)
                .header("Accept", "application/json")
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Device code HTTP ${resp.code}: ${text.take(280)}")
                    )
                }
                val json = JSONObject(text)
                if (json.has("error")) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("error_description", json.getString("error")))
                    )
                }
                Result.success(
                    DeviceCodeSession(
                        userCode = json.getString("user_code"),
                        deviceCode = json.getString("device_code"),
                        verificationUri = json.optString("verification_uri", "https://www.microsoft.com/link"),
                        intervalSec = json.optInt("interval", 5),
                        expiresIn = json.optInt("expires_in", 900)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollDeviceCode(session: DeviceCodeSession): Result<MsAccount> =
        withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + session.expiresIn * 1000L
            var interval = session.intervalSec.coerceAtLeast(3)
            while (System.currentTimeMillis() < deadline) {
                delay(interval * 1000L)
                val token = tryPollToken(session.deviceCode) ?: continue
                if (token.has("error")) {
                    val err = token.getString("error")
                    when (err) {
                        "authorization_pending" -> continue
                        "slow_down" -> { interval += 2; continue }
                        else -> return@withContext Result.failure(
                            IllegalStateException(token.optString("error_description", err))
                        )
                    }
                }
                val access = token.optString("access_token", "")
                if (access.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Microsoft returned no access token"))
                }
                return@withContext finishXbox(access, token.optString("refresh_token", ""))
            }
            Result.failure(IllegalStateException("Timed out. Enter the code at microsoft.com/link and try again."))
        }

    fun openVerification(activity: Activity, uri: String) {
        val target = if (uri.isBlank()) "https://www.microsoft.com/link" else uri
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun tryPollToken(deviceCode: String): JSONObject? {
        return try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("device_code", deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()
            val req = Request.Builder()
                .url("https://login.live.com/oauth20_token.srf")
                .post(body)
                .header("Accept", "application/json")
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) null else JSONObject(text)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun finishXbox(msAccess: String, refresh: String): Result<MsAccount> {
        val first = xboxChain(msAccess, prefixD = false)
        if (first.isSuccess) {
            val account = first.getOrThrow().copy(refreshToken = refresh)
            store.save(account)
            return Result.success(account)
        }
        val second = xboxChain(msAccess, prefixD = true)
        if (second.isSuccess) {
            val account = second.getOrThrow().copy(refreshToken = refresh)
            store.save(account)
            return Result.success(account)
        }
        return Result.failure(
            second.exceptionOrNull() ?: first.exceptionOrNull()
                ?: IllegalStateException("Xbox login failed")
        )
    }

    private fun xboxChain(msAccess: String, prefixD: Boolean): Result<MsAccount> {
        return try {
            val ticket = if (prefixD) "d=$msAccess" else msAccess
            val rpsBody = JSONObject()
                .put(
                    "Properties",
                    JSONObject()
                        .put("AuthMethod", "RPS")
                        .put("SiteName", "user.auth.xboxlive.com")
                        .put("RpsTicket", ticket)
                )
                .put("RelyingParty", "http://auth.xboxlive.com")
                .put("TokenType", "JWT")
            val userTokenJson = postJson(
                "https://user.auth.xboxlive.com/user/authenticate",
                rpsBody.toString()
            )
            val userToken = userTokenJson.getString("Token")
            val uhs = userTokenJson
                .getJSONObject("DisplayClaims")
                .getJSONArray("xui")
                .getJSONObject(0)
                .getString("uhs")

            val xstsBody = JSONObject()
                .put(
                    "Properties",
                    JSONObject()
                        .put("SandboxId", "RETAIL")
                        .put("UserTokens", JSONArray().put(userToken))
                )
                .put("RelyingParty", "http://xboxlive.com")
                .put("TokenType", "JWT")
            val xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody.toString())
            if (xsts.has("XErr") && !xsts.has("Token")) {
                val code = xsts.optLong("XErr")
                val why = when (code) {
                    2148916233L -> "This Microsoft account has no Xbox profile. Create one at xbox.com, then sign in again."
                    2148916238L -> "Child account needs an adult to allow Xbox."
                    else -> "Xbox rejected the account (XErr $code)"
                }
                return Result.failure(IllegalStateException(why))
            }
            val xstsToken = xsts.getString("Token")
            val xui = xsts.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0)
            val gamertag = xui.optString("gtg", "Player").ifBlank { "Player" }
            val xid = xui.optString("xid", uhs)
            Result.success(
                MsAccount(
                    gamertag = gamertag,
                    uuid = xid,
                    accessToken = "XBL3.0 x=$uhs;$xstsToken",
                    refreshToken = "",
                    skinUrl = ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postJson(url: String, json: String): JSONObject {
        val media = "application/json; charset=utf-8".toMediaType()
        val req = Request.Builder()
            .url(url)
            .post(json.toRequestBody(media))
            .header("Accept", "application/json")
            .header("x-xbl-contract-version", "1")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) throw IllegalStateException("Empty response from $url (${resp.code})")
            val jsonOut = JSONObject(text)
            if (!resp.isSuccessful && !jsonOut.has("XErr")) {
                throw IllegalStateException("HTTP ${resp.code}: ${text.take(300)}")
            }
            return jsonOut
        }
    }

    suspend fun loadLiveProfile(account: MsAccount): Result<LiveProfile> = withContext(Dispatchers.IO) {
        try {
            val token = account.accessToken
            if (token.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }
            var gamerscore = account.gamerscore
            var gamertag = account.gamertag
            runCatching {
                val req = Request.Builder()
                    .url("https://profile.xboxlive.com/users/me/profile/settings?settings=Gamerscore,Gamertag")
                    .get()
                    .header("Authorization", token)
                    .header("x-xbl-contract-version", "2")
                    .header("Accept", "application/json")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val profileUsers = JSONObject(text).optJSONArray("profileUsers")
                        val settings = profileUsers?.optJSONObject(0)?.optJSONArray("settings")
                        if (settings != null) {
                            for (i in 0 until settings.length()) {
                                val s = settings.getJSONObject(i)
                                when (s.optString("id")) {
                                    "Gamerscore" -> gamerscore = s.optString("value", "0").toIntOrNull() ?: gamerscore
                                    "Gamertag" -> {
                                        val g = s.optString("value")
                                        if (g.isNotBlank()) gamertag = g
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val achievements = mutableListOf<XboxAchievement>()
            val titleId = "1739947436"
            runCatching {
                val url =
                    "https://achievements.xboxlive.com/users/xuid(${account.uuid})/achievements?titleId=$titleId&maxItems=50&orderBy=UnlockTime"
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", token)
                    .header("x-xbl-contract-version", "2")
                    .header("Accept", "application/json")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        val arr = JSONObject(text).optJSONArray("achievements") ?: return@use
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            val pct = if (a.optString("progressState") == "Achieved") 100 else 0
                            achievements += XboxAchievement(
                                id = a.optString("id"),
                                name = a.optString("name"),
                                description = a.optString("description").ifBlank {
                                    a.optString("lockedDescription")
                                },
                                progressState = a.optString("progressState"),
                                percentComplete = pct
                            )
                        }
                    }
                }
            }
            if (gamerscore >= 0) {
                store.save(account.copy(gamertag = gamertag, gamerscore = gamerscore))
            }
            val unlocked = achievements.count { it.progressState == "Achieved" || it.percentComplete >= 100 }
            Result.success(
                LiveProfile(
                    gamertag = gamertag,
                    xuid = account.uuid,
                    gamerscore = gamerscore,
                    achievements = achievements,
                    unlockedCount = unlocked,
                    totalCount = achievements.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CLIENT_ID = "00000000402b5328"
        const val SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"
    }
}
