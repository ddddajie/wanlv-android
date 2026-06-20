package com.wanlv.app.network

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wanlv.app.config.AppConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.json.JSONObject

object AuthSession {
    private const val PreferencesName = "secure_auth_session"
    private const val EncryptedPayloadKey = "encrypted_payload"
    private const val InitializationVectorKey = "initialization_vector"
    private const val KeyAlias = "wanlv_auth_session_key"
    private const val AndroidKeyStoreProvider = "AndroidKeyStore"
    private const val CipherTransformation = "AES/GCM/NoPadding"

    private var preferences: SharedPreferences? = null
    private var userDataJson: String? = null

    var token by mutableStateOf<String?>(null)
        private set
    var refreshToken: String? = null
        private set
    var userId by mutableStateOf<Long?>(null)
        private set
    var loginExpirationVersion by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(
            PreferencesName,
            Context.MODE_PRIVATE
        )
        if (!restoreEncryptedSession()) {
            token = AppConfig.debugToken
            userId = AppConfig.debugUserId
        }
    }

    fun setLogin(
        userId: Long,
        token: String,
        refreshToken: String,
        userData: JSONObject? = null
    ) {
        this.userId = userId
        this.token = token
        this.refreshToken = refreshToken
        userDataJson = userData?.toString()
        persistEncryptedSession()
    }

    fun updateTokens(token: String, refreshToken: String) {
        this.token = token
        this.refreshToken = refreshToken
        userDataJson = userDataJson?.let {
            runCatching {
                JSONObject(it)
                    .put("token", token)
                    .put("refreshToken", refreshToken)
                    .toString()
            }.getOrDefault(it)
        }
        persistEncryptedSession()
    }

    fun updateUser(userData: JSONObject) {
        userDataJson = userData.toString()
        persistEncryptedSession()
    }

    fun cachedUserData(): JSONObject? = userDataJson
        ?.let { runCatching { JSONObject(it) }.getOrNull() }

    fun clear() {
        userId = null
        token = null
        refreshToken = null
        userDataJson = null
        preferences?.edit()
            ?.remove(EncryptedPayloadKey)
            ?.remove(InitializationVectorKey)
            ?.apply()
    }

    fun expireLogin() {
        val hadLogin = userId != null || !token.isNullOrBlank() || !refreshToken.isNullOrBlank()
        clear()
        // 重点：仅鉴权过期时发送一次全局事件，导航层据此进入登录区；用户主动退出不重复跳转。
        if (hadLogin) loginExpirationVersion++
    }

    private fun persistEncryptedSession() {
        val currentPreferences = preferences ?: return
        val currentToken = token?.takeIf { it.isNotBlank() } ?: return
        val currentUserId = userId ?: return

        runCatching {
            val payload = JSONObject()
                .put("userId", currentUserId)
                .put("token", currentToken)
                .apply {
                    refreshToken?.takeIf { it.isNotBlank() }?.let { put("refreshToken", it) }
                    userDataJson?.let { put("userData", JSONObject(it)) }
                }
                .toString()
                .toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance(CipherTransformation).apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            }
            val encryptedPayload = cipher.doFinal(payload)
            // 重点：SharedPreferences 只保存 Keystore 加密后的密文和随机 IV，不落盘明文 token。
            currentPreferences.edit()
                .putString(EncryptedPayloadKey, encryptedPayload.toBase64())
                .putString(InitializationVectorKey, cipher.iv.toBase64())
                .apply()
        }
    }

    private fun restoreEncryptedSession(): Boolean {
        val currentPreferences = preferences ?: return false
        val encryptedPayload = currentPreferences.getString(EncryptedPayloadKey, null)
            ?.fromBase64() ?: return false
        val initializationVector = currentPreferences.getString(InitializationVectorKey, null)
            ?.fromBase64() ?: return false

        return runCatching {
            val cipher = Cipher.getInstance(CipherTransformation).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    javax.crypto.spec.GCMParameterSpec(128, initializationVector)
                )
            }
            val payload = JSONObject(String(cipher.doFinal(encryptedPayload), Charsets.UTF_8))
            val restoredToken = payload.optString("token").takeIf { it.isNotBlank() }
                ?: return@runCatching false
            val restoredUserId = payload.optLong("userId").takeIf { it > 0L }
                ?: return@runCatching false
            token = restoredToken
            refreshToken = payload.optString("refreshToken").takeIf { it.isNotBlank() }
            userId = restoredUserId
            userDataJson = payload.optJSONObject("userData")?.toString()
            true
        }.getOrElse {
            // 密钥失效或密文损坏时直接清理会话，避免应用持续卡在不可恢复状态。
            currentPreferences.edit().clear().apply()
            false
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStoreProvider).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            AndroidKeyStoreProvider
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    KeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
