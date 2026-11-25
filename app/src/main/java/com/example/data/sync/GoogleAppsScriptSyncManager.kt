package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.LapEntity
import com.example.data.model.MasterOptions
import com.example.data.model.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleAppsScriptSyncManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val prefs = context.getSharedPreferences("time_study_settings", Context.MODE_PRIVATE)

    companion object {
        const val PREF_GAS_URL = "gas_web_app_url"
        const val PREF_SPREADSHEET_URL = "spreadsheet_database_url"
        const val DEFAULT_SPREADSHEET_URL =
            "https://docs.google.com/spreadsheets/d/1GTs6VgbRK_5cxcHJl7yQMzCMehnl_Qbpfyy89gY-flg/edit?gid=1092414501#gid=1092414501"
        const val PREF_LAST_SYNC_TIME = "last_sync_timestamp"
        const val PREF_LAST_SYNC_STATUS = "last_sync_status"
        const val PREF_SAVE_TO_MASTER = "save_to_master"
        const val PREF_ACTIVE_SCREEN_ROUTE = "active_screen_route"
        const val PREF_PENDING_DELETE_SESSION_IDS = "pending_delete_session_ids"
        const val PREF_IGNORED_REMOTE_SESSION_IDS = "ignored_remote_session_ids"
        const val PREF_DATABASE_LOCKED = "is_database_locked"
        const val PREF_HAS_SEEN_GUIDE = "has_seen_onboarding_guide"
    }

    fun getGasUrl(): String {
        return prefs.getString(PREF_GAS_URL, "") ?: ""
    }

    fun saveGasUrl(url: String) {
        prefs.edit().putString(PREF_GAS_URL, url.trim()).apply()
    }

    fun isDatabaseLocked(): Boolean {
        return prefs.getBoolean(PREF_DATABASE_LOCKED, true)
    }

    fun setDatabaseLocked(locked: Boolean) {
        prefs.edit().putBoolean(PREF_DATABASE_LOCKED, locked).apply()
    }

    fun hasSeenGuide(): Boolean {
        return prefs.getBoolean(PREF_HAS_SEEN_GUIDE, false)
    }

    fun setHasSeenGuide(seen: Boolean) {
        prefs.edit().putBoolean(PREF_HAS_SEEN_GUIDE, seen).apply()
    }

    fun getSpreadsheetUrl(): String {
        if (isDatabaseLocked()) return DEFAULT_SPREADSHEET_URL
        val saved = prefs.getString(PREF_SPREADSHEET_URL, "")?.trim().orEmpty()
        return saved.ifBlank { DEFAULT_SPREADSHEET_URL }
    }

    fun saveSpreadsheetUrl(url: String) {
        prefs.edit().putString(PREF_SPREADSHEET_URL, url.trim()).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(PREF_LAST_SYNC_TIME, 0L)
    }

    fun getLastSyncStatus(): String {
        return prefs.getString(PREF_LAST_SYNC_STATUS, "") ?: ""
    }

    fun isSaveToMasterEnabled(): Boolean {
        return prefs.getBoolean(PREF_SAVE_TO_MASTER, true)
    }

    fun setSaveToMaster(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SAVE_TO_MASTER, enabled).apply()
    }

    fun saveActiveScreenRoute(route: String) {
        prefs.edit().putString(PREF_ACTIVE_SCREEN_ROUTE, route).apply()
    }

    fun getActiveScreenRoute(): String {
        return prefs.getString(PREF_ACTIVE_SCREEN_ROUTE, "") ?: ""
    }

    fun getPendingDeleteSessionIds(): Set<String> {
        return prefs.getStringSet(PREF_PENDING_DELETE_SESSION_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun addPendingDeleteSessionId(sessionId: String) {
        val updated = getPendingDeleteSessionIds().toMutableSet()
        updated.add(sessionId)
        prefs.edit().putStringSet(PREF_PENDING_DELETE_SESSION_IDS, updated).apply()
    }

    fun removePendingDeleteSessionIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val updated = getPendingDeleteSessionIds().toMutableSet()
        updated.removeAll(ids.toSet())
        prefs.edit().putStringSet(PREF_PENDING_DELETE_SESSION_IDS, updated).apply()
    }

    fun getIgnoredRemoteSessionIds(): Set<String> {
        return prefs.getStringSet(PREF_IGNORED_REMOTE_SESSION_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun addIgnoredRemoteSessionId(sessionId: String) {
        val updated = getIgnoredRemoteSessionIds().toMutableSet()
        updated.add(sessionId)
        prefs.edit().putStringSet(PREF_IGNORED_REMOTE_SESSION_IDS, updated).apply()
    }

    fun removeIgnoredRemoteSessionIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val updated = getIgnoredRemoteSessionIds().toMutableSet()
        updated.removeAll(ids.toSet())
        prefs.edit().putStringSet(PREF_IGNORED_REMOTE_SESSION_IDS, updated).apply()
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun testConnection(targetUrl: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val url = targetUrl?.trim() ?: getGasUrl()
        if (url.isEmpty()) {
            return@withContext Result.failure(Exception("URL Google Apps Script belum diisi"))
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext Result.failure(Exception("URL tidak valid. Harus diawali https://"))
        }

        try {
            val fullUrl = if (url.contains("?")) "$url&action=ping" else "$url?action=ping"
            val request = Request.Builder().url(fullUrl).get().build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Koneksi Berhasil! Apps Script merespons dengan normal.")
                } else {
                    Result.failure(Exception("Gagal (HTTP ${response.code}): ${bodyString.take(120)}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghubungi server: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchMasterData(): Result<MasterOptions> = withContext(Dispatchers.IO) {
        val url = getGasUrl()
        if (url.isEmpty() || !isOnline()) {
            return@withContext Result.failure(Exception("Offline atau URL belum diisi"))
        }

        try {
            val fullUrl = if (url.contains("?")) "$url&action=master" else "$url?action=master"
            val request = Request.Builder().url(fullUrl).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                if (json.optString("status") != "success") {
                    return@withContext Result.failure(Exception(json.optString("message", "Error respon")))
                }
                val dataObj = json.optJSONObject("data") ?: JSONObject()

                val options = MasterOptions(
                    operators = jsonArrayToList(dataObj.optJSONArray("operators")),
                    observers = jsonArrayToList(dataObj.optJSONArray("observers")),
                    studyNames = jsonArrayToList(dataObj.optJSONArray("studyNames")),
                    lines = jsonArrayToList(dataObj.optJSONArray("lines")),
                    machines = jsonArrayToList(dataObj.optJSONArray("machines")),
                    buildings = jsonArrayToList(dataObj.optJSONArray("buildings")),
                    sessions = jsonArrayToList(dataObj.optJSONArray("sessions")).ifEmpty {
                        listOf("Shift 1", "Shift 2", "Shift 3", "Pagi", "Siang", "Malam")
                    }
                )
                Result.success(options)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullRemoteSessionsAndLaps(): Result<Pair<List<SessionEntity>, List<LapEntity>>> = withContext(Dispatchers.IO) {
        val url = getGasUrl()
        if (url.isEmpty() || !isOnline()) {
            return@withContext Result.failure(Exception("Perangkat offline atau URL belum diatur"))
        }

        try {
            // 1. Fetch Sessions
            val sUrl = if (url.contains("?")) "$url&action=sessions" else "$url?action=sessions"
            val sReq = Request.Builder().url(sUrl).get().build()
            val sessionsList = mutableListOf<SessionEntity>()

            client.newCall(sReq).execute().use { res ->
                if (!res.isSuccessful) return@withContext Result.failure(Exception("Gagal tarik sesi HTTP ${res.code}"))
                val sBody = res.body?.string().orEmpty()
                val json = JSONObject(sBody)
                val arr = json.optJSONArray("sessions") ?: JSONArray()
                val now = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val created = obj.optLong("createdAt", now).takeIf { it > 0L } ?: now
                    val updated = obj.optLong("updatedAt", created).takeIf { it > 0L } ?: created
                    sessionsList.add(
                        SessionEntity(
                            sessionId = obj.getString("sessionId"),
                            mode = obj.optString("mode", "TS"),
                            date = obj.optString("date", ""),
                            sessionName = obj.optString("sessionName", "Shift 1"),
                            operatorName = obj.optString("operatorName", ""),
                            observerName = obj.optString("observerName", ""),
                            studyName = obj.optString("studyName", ""),
                            line = obj.optString("line", ""),
                            machine = obj.optString("machine", ""),
                            building = obj.optString("building", ""),
                            notes = obj.optString("notes", ""),
                            stopwatchCount = obj.optInt("stopwatchCount", 1),
                            lapCount = obj.optInt("lapCount", 0),
                            totalDurationMillis = obj.optLong("totalDurationMillis", 0L),
                            status = obj.optString("status", "COMPLETED"),
                            syncStatus = "SYNCED",
                            createdAt = created,
                            updatedAt = updated
                        )
                    )
                }
            }

            // 2. Fetch Laps
            val lUrl = if (url.contains("?")) "$url&action=laps" else "$url?action=laps"
            val lReq = Request.Builder().url(lUrl).get().build()
            val lapsList = mutableListOf<LapEntity>()

            client.newCall(lReq).execute().use { res ->
                if (!res.isSuccessful) return@withContext Result.failure(Exception("Gagal tarik laps HTTP ${res.code}"))
                val lBody = res.body?.string().orEmpty()
                val json = JSONObject(lBody)
                val arr = json.optJSONArray("laps") ?: JSONArray()
                val now = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ts = obj.optLong("timestampMillis", now).takeIf { it > 0L } ?: now
                    lapsList.add(
                        LapEntity(
                            lapId = obj.getString("lapId"),
                            sessionId = obj.getString("sessionId"),
                            elementId = obj.optString("elementId", obj.getString("sessionId")),
                            elementName = obj.optString("elementName", ""),
                            sessionName = obj.optString("sessionName", "Shift 1"),
                            lapNumber = obj.optInt("lapNumber", 1),
                            lapDurationMillis = obj.optLong("lapDurationMillis", 0L),
                            cumulativeMillis = obj.optLong("cumulativeMillis", 0L),
                            label = obj.optString("label", ""),
                            timestampMillis = ts,
                            syncStatus = "SYNCED"
                        )
                    )
                }
            }

            Result.success(Pair(sessionsList, lapsList))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val str = arr.optString(i, "").trim()
            if (str.isNotBlank() && !list.contains(str)) {
                list.add(str)
            }
        }
        return list
    }

    suspend fun syncData(
        sessions: List<SessionEntity>,
        laps: List<LapEntity>,
        allSessionsMap: Map<String, SessionEntity>
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        val url = getGasUrl()
        if (url.isEmpty()) {
            return@withContext Result.failure(Exception("URL Google Apps Script belum dikonfigurasi di Pengaturan"))
        }
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Perangkat sedang offline"))
        }

        if (sessions.isEmpty() && laps.isEmpty()) {
            return@withContext Result.success(SyncResult(0, 0, "Semua data sudah tersinkron"))
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sessionsJsonArray = JSONArray()
        sessions.forEach { s ->
            val obj = JSONObject().apply {
                put("sessionId", s.sessionId)
                put("mode", s.mode)
                put("date", s.date)
                put("sessionName", s.sessionName)
                put("shift", s.sessionName)
                put("operatorName", s.operatorName)
                put("operator", s.operatorName)
                put("observerName", s.observerName)
                put("observer", s.observerName)
                put("studyName", s.studyName)
                put("line", s.line)
                put("machine", s.machine)
                put("building", s.building)
                put("notes", s.notes)
                put("stopwatchCount", s.stopwatchCount)
                put("lapCount", s.lapCount)
                put("totalDurationSeconds", s.totalDurationMillis / 1000.0)
                put("status", s.status)
                put("createdAt", dateFormat.format(Date(s.createdAt)))
            }
            sessionsJsonArray.put(obj)
        }

        val lapsJsonArray = JSONArray()
        laps.forEach { lap ->
            val parentSession = allSessionsMap[lap.sessionId]
            val effectiveSessionName = lap.sessionName.ifBlank { parentSession?.sessionName ?: "Shift 1" }
            val obj = JSONObject().apply {
                put("lapId", lap.lapId)
                put("sessionId", lap.sessionId)
                put("mode", parentSession?.mode ?: "TS")
                put("date", parentSession?.date ?: "")
                put("sessionName", effectiveSessionName)
                put("shift", effectiveSessionName)
                put("operatorName", parentSession?.operatorName ?: "")
                put("operator", parentSession?.operatorName ?: "")
                put("observerName", parentSession?.observerName ?: "")
                put("observer", parentSession?.observerName ?: "")
                put("studyName", parentSession?.studyName ?: "")
                put("line", parentSession?.line ?: "")
                put("machine", parentSession?.machine ?: "")
                put("building", parentSession?.building ?: "")
                put("elementName", lap.elementName)
                put("lapNumber", lap.lapNumber)
                put("lapDurationSeconds", lap.lapDurationMillis / 1000.0)
                put("cumulativeSeconds", lap.cumulativeMillis / 1000.0)
                put("label", lap.label)
                put("timestampFormatted", dateFormat.format(Date(lap.timestampMillis)))
            }
            lapsJsonArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("action", "sync")
            put("saveToMaster", isSaveToMasterEnabled())
            put("sessions", sessionsJsonArray)
            put("laps", lapsJsonArray)
        }.toString()

        try {
            val requestBody = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    prefs.edit()
                        .putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                        .putString(PREF_LAST_SYNC_STATUS, "Berhasil: ${sessions.size} sesi, ${laps.size} lap")
                        .apply()
                    Result.success(SyncResult(sessions.size, laps.size, "Sukses tersinkron ke Google Sheets"))
                } else {
                    prefs.edit()
                        .putString(PREF_LAST_SYNC_STATUS, "Gagal (HTTP ${response.code})")
                        .apply()
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            prefs.edit()
                .putString(PREF_LAST_SYNC_STATUS, "Error: ${e.localizedMessage}")
                .apply()
            Result.failure(Exception("Gagal sync: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteSessionsFromSheets(sessionIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        val ids = sessionIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (ids.isEmpty()) return@withContext Result.success(0)

        val url = getGasUrl()
        if (url.isEmpty()) {
            return@withContext Result.failure(Exception("URL Google Apps Script belum diatur"))
        }
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Perangkat offline"))
        }

        val payload = JSONObject().apply {
            put("action", "deleteSessions")
            put("sessionIds", JSONArray(ids))
        }.toString()

        try {
            val requestBody = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (json != null && json.optString("status") == "error") {
                    return@withContext Result.failure(Exception(json.optString("message", "Gagal hapus di Sheets")))
                }
                Result.success(ids.size)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal hapus di Sheets: ${e.localizedMessage}"))
        }
    }
}

data class SyncResult(
    val syncedSessions: Int,
    val syncedLaps: Int,
    val message: String
)
