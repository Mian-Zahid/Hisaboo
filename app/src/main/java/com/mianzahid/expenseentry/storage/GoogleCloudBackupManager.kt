package com.mianzahid.expenseentry.storage

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles Google Sign-In + automatic hidden Google Drive AppData backup & auto-restore
 * (same mechanism used by WhatsApp / Google Contacts backups).
 */
class GoogleCloudBackupManager(private val context: Context) {

    companion object {
        private const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        private const val BACKUP_FILE_NAME = "hisaboo_cloud_backup_v3.json"
    }

    fun getSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = getSignInClient().signInIntent

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun signOut() {
        try {
            getSignInClient().signOut().await()
        } catch (_: Exception) {
        }
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount): String? =
        withContext(Dispatchers.IO) {
            try {
                val androidAccount = account.account ?: return@withContext null
                val scope = "oauth2:$SCOPE_DRIVE_APPDATA"
                GoogleAuthUtil.getToken(context, androidAccount, scope)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun uploadBackupToCloud(account: GoogleSignInAccount, jsonContent: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(account)
                    ?: return@withContext Result.failure(Exception("Google OAuth token unavailable. Check SHA-1 / OAuth Client ID in Google Cloud Console."))

                val existingFileId = findExistingBackupFileId(token)

                if (existingFileId != null) {
                    // Update existing AppData backup file
                    val url = URL("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "PATCH"
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                    }
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonContent) }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        Result.success("Synced to Google Cloud (${account.email})")
                    } else {
                        Result.failure(Exception("Cloud upload HTTP $code"))
                    }
                } else {
                    // Create new multipart file inside hidden appDataFolder
                    val boundary = "hisaboo_boundary_${System.currentTimeMillis()}"
                    val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                        doOutput = true
                    }

                    val metadata = JSONObject().apply {
                        put("name", BACKUP_FILE_NAME)
                        put("parents", org.json.JSONArray().put("appDataFolder"))
                    }.toString()

                    val body = buildString {
                        append("--$boundary
")
                        append("Content-Type: application/json; charset=UTF-8

")
                        append(metadata)
                        append("
--$boundary
")
                        append("Content-Type: application/json; charset=UTF-8

")
                        append(jsonContent)
                        append("
--$boundary--
")
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        Result.success("Backed up to Google Cloud (${account.email})")
                    } else {
                        Result.failure(Exception("Cloud create HTTP $code"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadBackupFromCloud(account: GoogleSignInAccount): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(account)
                    ?: return@withContext Result.failure(Exception("Google OAuth token unavailable."))

                val fileId = findExistingBackupFileId(token)
                    ?: return@withContext Result.failure(Exception("No cloud backup found for ${account.email}"))

                val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (conn.responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    Result.success(JSONObject(responseText))
                } else {
                    Result.failure(Exception("Cloud download HTTP ${conn.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun findExistingBackupFileId(token: String): String? {
        return try {
            val queryUrl = URL(
                "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name%3D'$BACKUP_FILE_NAME'&fields=files(id%2Cname)"
            )
            val conn = (queryUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    files.getJSONObject(0).getString("id")
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
