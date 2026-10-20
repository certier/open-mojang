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
 * Microsoft account via device code → Xbox Live.
 */
class AuthManager(private val context: Context) {

    private val store = AccountStore(context)
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun current(): MsAccount? = store.getAccount()

    fun isSignedIn(): Boolean = store.isSignedIn()

    fun signOut() {
        store.clear()
    }

    suspend fun startDeviceCode(): Result<DeviceCodeSession> = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build()
            val req = Request.Builder()
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode")
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Device code HTTP ${resp.code}: $text")
                    )
                }
                val json = JSONObject(text)
                Result.success(
                    DeviceCodeSession(
                        userCode = json.getString("user_code"),
                        deviceCode = json.getString("device_code"),
                        verificationUri = json.optString(
                            "verification_uri",
                            "https://www.microsoft.com/link"
                        ),
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
                val token = tryPollToken(session.deviceCode)
                when {
                    token == null -> continue
                    token.has("error") -> {
                        val err = token.getString("error")
                        when (err) {
                            "authorization_pending" -> continue
                            "slow_down" -> {
                                interval += 2
                                continue
                            }
                            else -> return@withContext Result.failure(
                                IllegalStateException(token.optString("error_description", err))
                            )
                        }
                    }
                    else -> {
                        val access = token.getString("access_token")
                        val refresh = token.optString("refresh_token", "")
                        return@withContext finishXbox(access, refresh)
                    }
                }
            }
            Result.failure(IllegalStateException("Device login timed out"))
        }

    fun openVerification(activity: Activity, uri: String) {
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun signIn(activity: Activity): Result<MsAccount> = withContext(Dispatchers.Main) {
        val session = startDeviceCode().getOrElse {
            return@withContext Result.failure(it)
        }
        openVerification(activity, session.verificationUri)
        pollDeviceCode(session)
    }

    private fun tryPollToken(deviceCode: String): JSONObject? {
        return try {
            val body = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("client_id", CLIENT_ID)
                .add("device_code", deviceCode)
                .build()
            val req = Request.Builder()
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
                .post(body)
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
        return try {
            val rpsBody = JSONObject()
                .put(
                    "Properties",
                    JSONObject()
                        .put("AuthMethod", "RPS")
                        .put("SiteName", "user.auth.xboxlive.com")
                        .put("RpsTicket", "d=$msAccess")
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
            val xstsToken = xsts.getString("Token")
            val xui = xsts.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0)
            val gamertag = xui.optString("gtg", "Player")
            val xid = xui.optString("xid", "")

            val account = MsAccount(
                gamertag = gamertag.ifBlank { "Player" },
                uuid = xid.ifBlank { uhs },
                accessToken = "XBL3.0 x=$uhs;$xstsToken",
                refreshToken = refresh,
                skinUrl = ""
            )
            store.save(account)
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postJson(url: String, json: String): JSONObject {
        val media = "application/json".toMediaType()
        val req = Request.Builder()
            .url(url)
            .post(json.toRequestBody(media))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("x-xbl-contract-version", "1")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: $text")
            }
            return JSONObject(text)
        }
    }

    companion object {
        const val CLIENT_ID = "00000000402b5328"
        const val SCOPE = "XboxLive.signin offline_access"
    }
}
