package com.hugoer.mobile.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.hugoer.mobile.data.model.SiteProfile
import org.json.JSONArray
import org.json.JSONObject

/** Plain SharedPreferences for non-secret connected site metadata. */
class SiteProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("site_profiles", Context.MODE_PRIVATE)

    fun load(): List<SiteProfile> {
        val raw = preferences.getString("sites", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(SiteProfile(
                        id = obj.optString("id"),
                        owner = obj.optString("owner"),
                        repository = obj.optString("repository"),
                        displayName = obj.optString("displayName"),
                        defaultBranch = obj.optString("defaultBranch", "main"),
                        pagesUrl = obj.optString("pagesUrl"),
                        isUserOrOrganizationSite = obj.optBoolean("isUserOrOrganizationSite", false),
                        addedAtUtc = obj.optLong("addedAtUtc", System.currentTimeMillis())
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(profiles: List<SiteProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(JSONObject().apply {
                put("id", profile.id)
                put("owner", profile.owner)
                put("repository", profile.repository)
                put("displayName", profile.displayName)
                put("defaultBranch", profile.defaultBranch)
                put("pagesUrl", profile.pagesUrl)
                put("isUserOrOrganizationSite", profile.isUserOrOrganizationSite)
                put("addedAtUtc", profile.addedAtUtc)
            })
        }
        preferences.edit().putString("sites", array.toString()).apply()
    }
}

/** Secure Keystore-backed storage for GitHub PATs. Tokens never enter the plain profile JSON. */
class SecureTokenStore(context: Context) {
    private val preferences by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_tokens",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(key: String, token: String) {
        preferences.edit().putString(key, token).apply()
    }

    fun get(key: String): String? = preferences.getString(key, null)

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
