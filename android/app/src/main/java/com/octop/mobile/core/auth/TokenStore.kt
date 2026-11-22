package com.octop.mobile.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.octop.mobile.core.sync.OctopClient

/**
 * token 落盘。
 *
 * 正常走 Android Keystore 支持的 EncryptedSharedPreferences；
 * 少数 ROM 上 Keystore 会抽风（抛异常），此时降级成普通 Prefs 并打日志——
 * 宁可弱一点也不要让用户卡在登录页。
 */
object TokenStore {

    private const val TAG = "TokenStore"
    private const val FILE_SECURE = "octop_secure"
    private const val FILE_FALLBACK = "octop_plain"

    private const val K_TOKEN = "access_token"
    private const val K_ENDPOINT = "endpoint"
    private const val K_USERNAME = "username"
    private const val K_BRIDGE_TOKEN = "bridge_token"
    private const val K_BRIDGE_ON = "bridge_enabled"

    private var prefs: SharedPreferences? = null
    private var encrypted: Boolean = false

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        prefs = runCatching {
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            encrypted = true
            EncryptedSharedPreferences.create(
                app,
                FILE_SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as SharedPreferences
        }.getOrElse { e ->
            Log.w(TAG, "Keystore 不可用，降级为普通存储: ${e.message}")
            encrypted = false
            app.getSharedPreferences(FILE_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    val isEncrypted: Boolean get() = encrypted

    fun save(token: String, endpoint: String, username: String = "") {
        prefs?.edit()
            ?.putString(K_TOKEN, token)
            ?.putString(K_ENDPOINT, com.octop.mobile.core.sync.Endpoints.normalize(endpoint))
            ?.putString(K_USERNAME, username)
            ?.apply()
    }

    fun setEndpoint(endpoint: String) {
        prefs?.edit()?.putString(K_ENDPOINT, endpoint)?.apply()
    }

    fun token(): String? = prefs?.getString(K_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun endpoint(): String = prefs?.getString(K_ENDPOINT, null)?.takeIf { it.isNotBlank() }
        ?: OctopClient.DEFAULT_BASE_URL

    fun username(): String = prefs?.getString(K_USERNAME, "").orEmpty()

    /** 手机能力桥的访问令牌：首次调用时生成，之后固定不变 */
    fun bridgeToken(): String {
        prefs?.getString(K_BRIDGE_TOKEN, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val fresh = java.util.UUID.randomUUID().toString().replace("-", "").take(24)
        prefs?.edit()?.putString(K_BRIDGE_TOKEN, fresh)?.apply()
        return fresh
    }

    fun bridgeEnabled(): Boolean = prefs?.getBoolean(K_BRIDGE_ON, true) ?: true

    fun setBridgeEnabled(on: Boolean) {
        prefs?.edit()?.putBoolean(K_BRIDGE_ON, on)?.apply()
    }

    fun clearToken() {
        prefs?.edit()?.remove(K_TOKEN)?.apply()
    }

    fun clearAll() {
        prefs?.edit()?.clear()?.apply()
    }
}
