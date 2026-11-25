package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.LapEntity
import com.example.data.model.MasterItemEntity
import com.example.data.model.SessionEntity
import com.example.data.model.StopwatchElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeStudyDao {

    @Query("SELECT * FROM sessions WHERE status != 'DRAFT' ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status != 'DRAFT'")
    suspend fun getAllSessionsSync(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE status != 'DRAFT' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    fun getSessionFlow(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE studyName = :studyName AND status != 'DRAFT' ORDER BY createdAt ASC")
    fun getSessionsForStudyName(studyName: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE studyName = :studyName AND status != 'DRAFT' ORDER BY createdAt ASC")
    suspend fun getSessionsForStudyNameSync(studyName: String): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE studyName = :studyName AND date = :date AND status != 'DRAFT'")
    suspend fun countSessionsForStudyAndDate(studyName: String, date: String): Int

    @Query("SELECT DISTINCT date FROM sessions WHERE status != 'DRAFT'")
    fun getDistinctSessionDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM sessions WHERE status = 'DRAFT' AND lapCount = 0 AND sessionId != :keepSessionId")
    suspend fun deleteEmptyDraftSessionsExcept(keepSessionId: String = "")

    // Elements
    @Query("SELECT * FROM stopwatch_elements WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getElementsForSession(sessionId: String): Flow<List<StopwatchElementEntity>>

    @Query("SELECT * FROM stopwatch_elements WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getElementsForSessionSync(sessionId: String): List<StopwatchElementEntity>

    @Query("SELECT * FROM stopwatch_elements WHERE elementId = :elementId")
    suspend fun getElementById(elementId: String): StopwatchElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: StopwatchElementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElements(elements: List<StopwatchElementEntity>)

    @Update
    suspend fun updateElement(element: StopwatchElementEntity)

    @Query("DELETE FROM stopwatch_elements WHERE elementId = :elementId")
    suspend fun deleteElement(elementId: String)

    // Laps
    @Query("SELECT laps.* FROM laps INNER JOIN sessions ON laps.sessionId = sessions.sessionId WHERE sessions.status != 'DRAFT' ORDER BY laps.timestampMillis ASC")
    suspend fun getAllLapsSync(): List<LapEntity>

    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun getLapsForSession(sessionId: String): Flow<List<LapEntity>>

    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getLapsForSessionSync(sessionId: String): List<LapEntity>

    @Query("SELECT * FROM laps WHERE elementId = :elementId ORDER BY lapNumber ASC")
    fun getLapsForElement(elementId: String): Flow<List<LapEntity>>

    @Query("SELECT * FROM laps WHERE elementId = :elementId ORDER BY lapNumber ASC")
    suspend fun getLapsForElementSync(elementId: String): List<LapEntity>

    @Query("SELECT laps.* FROM laps INNER JOIN sessions ON laps.sessionId = sessions.sessionId WHERE sessions.studyName = :studyName AND sessions.status != 'DRAFT' ORDER BY laps.timestampMillis ASC")
    suspend fun getLapsForStudyNameSync(studyName: String): List<LapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapEntity>)

    @Query("DELETE FROM laps WHERE lapId = :lapId")
    suspend fun deleteLap(lapId: String)

    @Query("DELETE FROM laps WHERE sessionId = :sessionId AND elementId = :elementId")
    suspend fun deleteLapsForElement(sessionId: String, elementId: String)

    @Query("SELECT * FROM laps WHERE lapId = :lapId")
    suspend fun getLapById(lapId: String): LapEntity?

    // Sync helpers
    @Query("SELECT * FROM sessions WHERE status != 'DRAFT' AND syncStatus = 'PENDING'")
    suspend fun getPendingSessions(): List<SessionEntity>

    @Query("SELECT laps.* FROM laps INNER JOIN sessions ON laps.sessionId = sessions.sessionId WHERE sessions.status != 'DRAFT' AND laps.syncStatus = 'PENDING'")
    suspend fun getPendingLaps(): List<LapEntity>

    @Query("SELECT COUNT(*) FROM laps INNER JOIN sessions ON laps.sessionId = sessions.sessionId WHERE sessions.status != 'DRAFT' AND laps.syncStatus = 'PENDING'")
    fun getPendingLapsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions WHERE status != 'DRAFT' AND syncStatus = 'PENDING'")
    fun getPendingSessionsCountFlow(): Flow<Int>

    @Query("UPDATE sessions SET syncStatus = 'SYNCED' WHERE sessionId IN (:sessionIds)")
    suspend fun markSessionsSynced(sessionIds: List<String>)

    @Query("UPDATE laps SET syncStatus = 'SYNCED' WHERE lapId IN (:lapIds)")
    suspend fun markLapsSynced(lapIds: List<String>)

    @Query("UPDATE laps SET syncStatus = 'PENDING' WHERE sessionId = :sessionId")
    suspend fun markLapsPendingForSession(sessionId: String)

    // Master items caching
    @Query("SELECT value FROM master_items WHERE category = :category ORDER BY value ASC")
    fun getMasterValuesByCategory(category: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterItems(items: List<MasterItemEntity>)

    @Query("DELETE FROM master_items WHERE category = :category")
    suspend fun deleteMasterItemsByCategory(category: String)

    // Autocomplete queries from historical entries
    @Query("SELECT DISTINCT operatorName FROM sessions WHERE operatorName != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentOperators(): Flow<List<String>>

    @Query("SELECT DISTINCT observerName FROM sessions WHERE observerName != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentObservers(): Flow<List<String>>

    @Query("SELECT DISTINCT studyName FROM sessions WHERE studyName != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentStudyNames(): Flow<List<String>>

    @Query("SELECT DISTINCT line FROM sessions WHERE line != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentLines(): Flow<List<String>>

    @Query("SELECT DISTINCT machine FROM sessions WHERE machine != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentMachines(): Flow<List<String>>

    @Query("SELECT DISTINCT building FROM sessions WHERE building != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentBuildings(): Flow<List<String>>

    @Query("SELECT DISTINCT sessionName FROM sessions WHERE sessionName != '' AND status != 'DRAFT' ORDER BY createdAt DESC LIMIT 15")
    fun getRecentSessionNames(): Flow<List<String>>

    @Transaction
    suspend fun recalculateLapsForElement(elementId: String) {
        val remainingLaps = getLapsForElementSync(elementId)
        var cumulative = 0L
        val updated = remainingLaps.mapIndexed { index, lap ->
            cumulative += lap.lapDurationMillis
            lap.copy(
                lapNumber = index + 1,
                cumulativeMillis = cumulative,
                syncStatus = if (lap.syncStatus == "DRAFT") "DRAFT" else "PENDING"
            )
        }
        insertLaps(updated)
    }
}
