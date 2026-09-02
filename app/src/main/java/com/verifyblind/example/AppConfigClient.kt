package com.verifyblind.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches GET /api/public/app-config and decides whether the local EXAMPLE preview is enabled
 * for this build. Best-effort: any failure / empty value / version mismatch → false (real flow).
 * No dependency added — uses HttpURLConnection on Dispatchers.IO.
 */
object AppConfigClient {
    suspend fun isPreviewEnabled(apiBaseUrl: String, appVersionName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(apiBaseUrl.trimEnd('/') + "/api/public/app-config")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                try {
                    if (conn.responseCode != 200) return@withContext false
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val previewVersion = JSONObject(body).optString("preview_version_example_android", "")
                    previewVersion.isNotEmpty() && previewVersion == appVersionName
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                false
            }
        }
}
