package com.example.data.repository

import com.example.data.local.TimeStudyDao
import com.example.data.model.LapEntity
import com.example.data.model.MasterItemEntity
import com.example.data.model.MasterOptions
import com.example.data.model.SessionEntity
import com.example.data.model.StopwatchElementEntity
import com.example.data.sync.GoogleAppsScriptSyncManager
import com.example.data.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class TimeStudyRepository(
    private val dao: TimeStudyDao,
    private val syncManager: GoogleAppsScriptSyncManager
) {

    val allSessions: Flow<List<SessionEntity>> = dao.getAllSessions()
    val pendingSessionsCount: Flow<Int> = dao.getPendingSessionsCountFlow()
    val pendingLapsCount: Flow<Int> = dao.getPendingLapsCountFlow()
    val distinctDates: Flow<List<String>> = dao.getDistinctSessionDates()

    val recentOperators: Flow<List<String>> = dao.getRecentOperators()
    val recentObservers: Flow<List<String>> = dao.getRecentObservers()
    val recentStudyNames: Flow<List<String>> = dao.getRecentStudyNames()
    val recentLines: Flow<List<String>> = dao.getRecentLines()
    val recentMachines: Flow<List<String>> = dao.getRecentMachines()
    val recentBuildings: Flow<List<String>> = dao.getRecentBuildings()
    val recentSessionNames: Flow<List<String>> = dao.getRecentSessionNames()

    // Master options combining Room cached items with historical entries
    fun getMasterOptions(): Flow<MasterOptions> {
        val masterFlow = combine(
            dao.getMasterValuesByCategory("operator"),
            dao.getMasterValuesByCategory("observer"),
            dao.getMasterValuesByCategory("studyName"),
            dao.getMasterValuesByCategory("line"),
            dao.getMasterValuesByCategory("machine")
        ) { mOp, mObs, mStudy, mLine, mMac ->
            listOf(mOp, mObs, mStudy, mLine, mMac)
        }

        val masterExtraFlow = combine(
            dao.getMasterValuesByCategory("building"),
            dao.getMasterValuesByCategory("session"),
            recentOperators,
            recentObservers,
            recentStudyNames
        ) { mBld, mSes, rOp, rObs, rStudy ->
            listOf(mBld, mSes, rOp, rObs, rStudy)
        }

        val recentFlow = combine(
            recentLines,
            recentMachines,
            recentBuildings,
            recentSessionNames
        ) { rLine, rMac, rBld, rSes ->
            listOf(rLine, rMac, rBld, rSes)
        }

        return combine(masterFlow, masterExtraFlow, recentFlow) { m, m2, r ->
            val mOp = m[0]
            val mObs = m[1]
            val mStudy = m[2]
            val mLine = m[3]
            val mMac = m[4]

            val mBld = m2[0]
            val mSes = m2[1]
            val rOp = m2[2]
            val rObs = m2[3]
            val rStudy = m2[4]

            val rLine = r[0]
            val rMac = r[1]
            val rBld = r[2]
            val rSes = r[3]

            MasterOptions(
                operators = (mOp + rOp).distinct().filter { it.isNotBlank() },
                observers = mObs.ifEmpty { rObs }.distinct().filter { it.isNotBlank() },
                studyNames = (mStudy + rStudy).distinct().filter { it.isNotBlank() },
                lines = (mLine + rLine).distinct().filter { it.isNotBlank() },
                machines = (mMac + rMac).distinct().filter { it.isNotBlank() },
                buildings = (mBld + rBld).distinct().filter { it.isNotBlank() },
                sessions = (mSes + rSes + listOf("Shift 1", "Shift 2", "Shift 3", "Pagi", "Siang", "Malam")).distinct().filter { it.isNotBlank() }
            )
        }
    }

    suspend fun refreshMasterFromSheets(): Result<MasterOptions> {
        val result = syncManager.fetchMasterData()
        if (result.isSuccess) {
            val options = result.getOrNull() ?: return result
            val entities = mutableListOf<MasterItemEntity>()
            options.operators.forEach { entities.add(MasterItemEntity("operator", it)) }
            options.observers.forEach { entities.add(MasterItemEntity("observer", it)) }
            options.studyNames.forEach { entities.add(MasterItemEntity("studyName", it)) }
            options.lines.forEach { entities.add(MasterItemEntity("line", it)) }
            options.machines.forEach { entities.add(MasterItemEntity("machine", it)) }
            options.buildings.forEach { entities.add(MasterItemEntity("building", it)) }
            options.sessions.forEach { entities.add(MasterItemEntity("session", it)) }

            dao.deleteMasterItemsByCategory("operator")
            dao.deleteMasterItemsByCategory("observer")
            dao.deleteMasterItemsByCategory("studyName")
            dao.deleteMasterItemsByCategory("line")
            dao.deleteMasterItemsByCategory("machine")
            dao.deleteMasterItemsByCategory("building")
            dao.deleteMasterItemsByCategory("session")
            dao.insertMasterItems(entities)
        }
        return result
    }

    suspend fun pullDataFromSheets(): Result<String> {
        val res = syncManager.pullRemoteSessionsAndLaps()
        if (res.isFailure) return Result.failure(res.exceptionOrNull() ?: Exception("Gagal menarik data"))
        val (remoteSessions, remoteLaps) = res.getOrNull() ?: return Result.failure(Exception("Data kosong"))

        val localSessionsMap = dao.getAllSessionsSync().associateBy { it.sessionId }
        val localLapsMap = dao.getAllLapsSync().associateBy { it.lapId }
        val excludedIds = syncManager.getPendingDeleteSessionIds() + syncManager.getIgnoredRemoteSessionIds()

        val protectedSessionIds = localSessionsMap.values
            .filter { it.syncStatus == "PENDING" || it.status == "ACTIVE" }
            .map { it.sessionId }
            .toSet()

        val sessionsToInsert = remoteSessions.filter { remote ->
            remote.sessionId !in protectedSessionIds && remote.sessionId !in excludedIds
        }

        val lapsToInsert = remoteLaps.filter { remoteLap ->
            val localLap = localLapsMap[remoteLap.lapId]
            remoteLap.sessionId !in protectedSessionIds &&
                remoteLap.sessionId !in excludedIds &&
                (localLap == null || localLap.syncStatus != "PENDING")
        }

        if (sessionsToInsert.isNotEmpty()) {
            dao.insertSessions(sessionsToInsert)
        }
        if (lapsToInsert.isNotEmpty()) {
            dao.insertLaps(lapsToInsert)
        }

        // Reconstruct StopwatchElementEntity rows for pulled sessions that have none locally
        val lapsBySession = remoteLaps.groupBy { it.sessionId }
        val colors = listOf("#F6C445", "#2F7F6F", "#F26A7E", "#CFC4F7", "#A9C8FA", "#F5A94A")
        sessionsToInsert.forEach { session ->
            val existingElements = dao.getElementsForSessionSync(session.sessionId)
            if (existingElements.isEmpty()) {
                val sessionLaps = lapsBySession[session.sessionId].orEmpty()
                val elementsToCreate = if (session.mode == "WLA" && sessionLaps.isNotEmpty()) {
                    val distinctPairs = sessionLaps
                        .groupBy { it.elementId.ifBlank { session.sessionId } }
                    distinctPairs.entries.mapIndexed { idx, (elemId, elemLaps) ->
                        val elemName = elemLaps.firstOrNull()?.elementName?.ifBlank { "Elemen ${idx + 1}" }
                            ?: "Elemen ${idx + 1}"
                        StopwatchElementEntity(
                            elementId = elemId,
                            sessionId = session.sessionId,
                            name = elemName,
                            colorHex = colors[idx % colors.size],
                            elapsedMillis = elemLaps.sumOf { it.lapDurationMillis }.coerceAtLeast(session.totalDurationMillis),
                            isRunning = false,
                            orderIndex = idx
                        )
                    }
                } else {
                    listOf(
                        StopwatchElementEntity(
                            elementId = sessionLaps.firstOrNull()?.elementId?.ifBlank { session.sessionId } ?: session.sessionId,
                            sessionId = session.sessionId,
                            name = session.studyName.ifBlank { "Time Study" },
                            colorHex = colors[0],
                            elapsedMillis = session.totalDurationMillis,
                            isRunning = false,
                            orderIndex = 0
                        )
                    )
                }
                dao.insertElements(elementsToCreate)
            }
        }

        return Result.success("Berhasil menyinkronkan ${sessionsToInsert.size} sesi dan ${lapsToInsert.size} lap dari Sheets")
    }

    suspend fun getLastSession(): SessionEntity? = dao.getLastSession()

    suspend fun getLatestActiveSession(): SessionEntity? = dao.getLatestActiveSession()

    suspend fun cleanupEmptyDraftSessions(keepSessionId: String = "") {
        dao.deleteEmptyDraftSessionsExcept(keepSessionId)
    }

    fun getSessionFlow(sessionId: String): Flow<SessionEntity?> = dao.getSessionFlow(sessionId)

    suspend fun getSessionById(sessionId: String): SessionEntity? = dao.getSessionById(sessionId)

    suspend fun getSessionsForStudyName(studyName: String): List<SessionEntity> =
        dao.getSessionsForStudyNameSync(studyName)

    suspend fun getLapsForStudyName(studyName: String): List<LapEntity> =
        dao.getLapsForStudyNameSync(studyName)

    suspend fun countSessionsForStudyAndDate(studyName: String, date: String): Int =
        dao.countSessionsForStudyAndDate(studyName, date)

    fun getElementsForSession(sessionId: String): Flow<List<StopwatchElementEntity>> =
        dao.getElementsForSession(sessionId)

    suspend fun getElementsForSessionSync(sessionId: String): List<StopwatchElementEntity> =
        dao.getElementsForSessionSync(sessionId)

    fun getLapsForSession(sessionId: String): Flow<List<LapEntity>> =
        dao.getLapsForSession(sessionId)

    suspend fun getLapsForSessionSync(sessionId: String): List<LapEntity> =
        dao.getLapsForSessionSync(sessionId)

    fun getLapsForElement(elementId: String): Flow<List<LapEntity>> =
        dao.getLapsForElement(elementId)

    suspend fun getLapsForElementSync(elementId: String): List<LapEntity> =
        dao.getLapsForElementSync(elementId)

    suspend fun createSession(
        session: SessionEntity,
        initialElementNames: List<String>
    ): Pair<SessionEntity, List<StopwatchElementEntity>> {
        dao.insertSession(session)

        val colors = listOf(
            "#F6C445", // Mustard
            "#2F7F6F", // Teal
            "#F26A7E", // Coral
            "#CFC4F7", // Lavender
            "#A9C8FA", // Sky Blue
            "#F5A94A"  // Orange
        )

        val elements = if (initialElementNames.isEmpty()) {
            listOf(
                StopwatchElementEntity(
                    sessionId = session.sessionId,
                    name = session.studyName.ifBlank { "Elemen 1" },
                    colorHex = colors[0],
                    orderIndex = 0
                )
            )
        } else {
            initialElementNames.mapIndexed { idx, name ->
                StopwatchElementEntity(
                    sessionId = session.sessionId,
                    name = name.ifBlank { "Elemen ${idx + 1}" },
                    colorHex = colors[idx % colors.size],
                    orderIndex = idx
                )
            }
        }

        dao.insertElements(elements)
        val updatedSession = session.copy(stopwatchCount = elements.size)
        dao.updateSession(updatedSession)
        return Pair(updatedSession, elements)
    }

    suspend fun createNextSession(
        oldSession: SessionEntity,
        elements: List<StopwatchElementEntity>
    ): Pair<SessionEntity, List<StopwatchElementEntity>> {
        // Compute next shift/session name e.g. "Shift 1" -> "Shift 2"
        val oldName = oldSession.sessionName.trim()
        val regex = Regex("^(.*?)(?:\\s+(\\d+))?$")
        val match = regex.find(oldName)
        val nextSessionName = if (match != null && match.groupValues[2].isNotEmpty()) {
            val prefix = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull() ?: 1
            "$prefix ${num + 1}"
        } else if (oldName.startsWith("Shift", ignoreCase = true) || oldName.startsWith("Sesi", ignoreCase = true)) {
            "Shift 2"
        } else {
            "$oldName (Lanjutan)"
        }

        val newSessionId = UUID.randomUUID().toString()
        val isDraft = oldSession.status == "DRAFT"
        val newSession = SessionEntity(
            sessionId = newSessionId,
            mode = oldSession.mode,
            date = oldSession.date,
            sessionName = nextSessionName,
            operatorName = oldSession.operatorName,
            observerName = oldSession.observerName,
            studyName = oldSession.studyName,
            line = oldSession.line,
            machine = oldSession.machine,
            building = oldSession.building,
            notes = oldSession.notes,
            stopwatchCount = elements.size,
            lapCount = 0,
            totalDurationMillis = 0L,
            status = if (isDraft) "DRAFT" else "ACTIVE",
            syncStatus = if (isDraft) "DRAFT" else "PENDING",
            createdAt = System.currentTimeMillis()
        )
        dao.insertSession(newSession)

        val newElements = elements.mapIndexed { idx, el ->
            StopwatchElementEntity(
                elementId = UUID.randomUUID().toString(),
                sessionId = newSessionId,
                name = el.name,
                colorHex = el.colorHex,
                elapsedMillis = 0L,
                isRunning = false,
                lastStartTimeMillis = 0L,
                orderIndex = idx
            )
        }
        dao.insertElements(newElements)
        return Pair(newSession, newElements)
    }

    suspend fun addElement(sessionId: String, name: String, colorHex: String): StopwatchElementEntity {
        val currentElements = dao.getElementsForSessionSync(sessionId)
        val newElement = StopwatchElementEntity(
            elementId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            name = name.ifBlank { "Elemen ${currentElements.size + 1}" },
            colorHex = colorHex,
            orderIndex = currentElements.size
        )
        dao.insertElement(newElement)

        val session = dao.getSessionById(sessionId)
        if (session != null) {
            dao.updateSession(
                session.copy(
                    stopwatchCount = currentElements.size + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return newElement
    }

    suspend fun updateElement(element: StopwatchElementEntity) {
        dao.updateElement(element)
    }

    suspend fun deleteElement(elementId: String, sessionId: String) {
        dao.deleteElement(elementId)
        val remaining = dao.getElementsForSessionSync(sessionId)
        val session = dao.getSessionById(sessionId)
        if (session != null) {
            val totalLaps = dao.getLapsForSessionSync(sessionId).size
            dao.updateSession(
                session.copy(
                    stopwatchCount = remaining.size,
                    lapCount = totalLaps,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteLapsForElement(sessionId: String, elementId: String) {
        dao.deleteLapsForElement(sessionId, elementId)
        val remainingLaps = dao.getLapsForSessionSync(sessionId).size
        val session = dao.getSessionById(sessionId)
        if (session != null) {
            dao.updateSession(
                session.copy(
                    lapCount = remainingLaps,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordLap(
        sessionId: String,
        elementId: String,
        elementName: String,
        sessionName: String,
        lapDurationMillis: Long,
        cumulativeMillis: Long,
        label: String = ""
    ): LapEntity {
        val existingLaps = dao.getLapsForElementSync(elementId)
        val lapNumber = existingLaps.size + 1
        val session = dao.getSessionById(sessionId)
        val isDraft = session?.status == "DRAFT"
        val lap = LapEntity(
            lapId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            elementId = elementId,
            elementName = elementName,
            sessionName = sessionName,
            lapNumber = lapNumber,
            lapDurationMillis = lapDurationMillis,
            cumulativeMillis = cumulativeMillis,
            label = label,
            timestampMillis = System.currentTimeMillis(),
            syncStatus = if (isDraft) "DRAFT" else "PENDING"
        )
        dao.insertLap(lap)

        val allSessionLaps = dao.getLapsForSessionSync(sessionId)
        if (session != null) {
            dao.updateSession(
                session.copy(
                    lapCount = allSessionLaps.size,
                    syncStatus = if (isDraft) "DRAFT" else "PENDING",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return lap
    }

    suspend fun deleteLap(lapId: String, elementId: String, sessionId: String): LapEntity? {
        val lap = dao.getLapById(lapId) ?: return null
        dao.deleteLap(lapId)
        dao.recalculateLapsForElement(elementId)

        val allSessionLaps = dao.getLapsForSessionSync(sessionId)
        val session = dao.getSessionById(sessionId)
        if (session != null) {
            dao.updateSession(
                session.copy(
                    lapCount = allSessionLaps.size,
                    syncStatus = if (session.status == "DRAFT") "DRAFT" else "PENDING",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return lap
    }

    suspend fun restoreLap(lap: LapEntity, sessionId: String) {
        val session = dao.getSessionById(sessionId)
        val isDraft = session?.status == "DRAFT"
        dao.insertLap(lap.copy(syncStatus = if (isDraft) "DRAFT" else "PENDING"))
        dao.recalculateLapsForElement(lap.elementId)

        val allSessionLaps = dao.getLapsForSessionSync(sessionId)
        if (session != null) {
            dao.updateSession(
                session.copy(
                    lapCount = allSessionLaps.size,
                    syncStatus = if (isDraft) "DRAFT" else "PENDING",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateLapLabel(lapId: String, newLabel: String) {
        val lap = dao.getLapById(lapId) ?: return
        val session = dao.getSessionById(lap.sessionId)
        val targetSync = if (session?.status == "DRAFT") "DRAFT" else "PENDING"
        dao.insertLap(lap.copy(label = newLabel, syncStatus = targetSync))
    }

    suspend fun completeSession(
        sessionId: String,
        totalDurationMillis: Long,
        customStudyName: String? = null
    ) {
        val session = dao.getSessionById(sessionId) ?: return
        val resolvedStudyName = customStudyName?.trim()?.takeIf { it.isNotBlank() } ?: session.studyName
        val studyNameChanged = resolvedStudyName != session.studyName

        val elements = dao.getElementsForSessionSync(sessionId)
        elements.forEach { el ->
            val updatedName = if (studyNameChanged && session.mode != "WLA") resolvedStudyName else el.name
            if (el.isRunning || updatedName != el.name) {
                dao.updateElement(
                    el.copy(
                        name = updatedName,
                        isRunning = false,
                        lastStartTimeMillis = 0L
                    )
                )
            }
        }
        val allLaps = dao.getLapsForSessionSync(sessionId)
        if (studyNameChanged && session.mode != "WLA" && allLaps.isNotEmpty()) {
            dao.insertLaps(allLaps.map { it.copy(elementName = resolvedStudyName, syncStatus = "PENDING") })
        } else {
            dao.markLapsPendingForSession(sessionId)
        }
        dao.updateSession(
            session.copy(
                studyName = resolvedStudyName,
                status = "COMPLETED",
                totalDurationMillis = totalDurationMillis,
                lapCount = allLaps.size,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
        )
    }

    suspend fun promoteDraftSessionToActive(
        sessionId: String,
        totalDurationMillis: Long,
        customStudyName: String? = null
    ) {
        val session = dao.getSessionById(sessionId) ?: return
        val resolvedStudyName = customStudyName?.trim()?.takeIf { it.isNotBlank() } ?: session.studyName
        val studyNameChanged = resolvedStudyName != session.studyName

        val elements = dao.getElementsForSessionSync(sessionId)
        elements.forEach { el ->
            val updatedName = if (studyNameChanged && session.mode != "WLA") resolvedStudyName else el.name
            dao.updateElement(
                el.copy(
                    name = updatedName,
                    elapsedMillis = if (session.mode != "WLA") totalDurationMillis else el.elapsedMillis,
                    isRunning = false,
                    lastStartTimeMillis = 0L
                )
            )
        }
        val allLaps = dao.getLapsForSessionSync(sessionId)
        if (studyNameChanged && session.mode != "WLA" && allLaps.isNotEmpty()) {
            dao.insertLaps(allLaps.map { it.copy(elementName = resolvedStudyName, syncStatus = "PENDING") })
        } else {
            dao.markLapsPendingForSession(sessionId)
        }
        dao.updateSession(
            session.copy(
                studyName = resolvedStudyName,
                status = "ACTIVE",
                totalDurationMillis = totalDurationMillis,
                lapCount = allLaps.size,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
        )
    }

    suspend fun updateSession(session: SessionEntity) {
        val isDraft = session.status == "DRAFT"
        dao.updateSession(session.copy(syncStatus = if (isDraft) "DRAFT" else "PENDING"))
        if (!isDraft) {
            dao.markLapsPendingForSession(session.sessionId)
        }
    }

    suspend fun deleteSession(
        sessionId: String,
        deleteFromSheets: Boolean = false,
        markIgnored: Boolean = true
    ): Result<String> {
        dao.deleteSession(sessionId)
        if (deleteFromSheets) {
            syncManager.addPendingDeleteSessionId(sessionId)
            syncManager.addIgnoredRemoteSessionId(sessionId)
            if (syncManager.isOnline() && syncManager.getGasUrl().isNotBlank()) {
                val remoteRes = syncManager.deleteSessionsFromSheets(listOf(sessionId))
                if (remoteRes.isSuccess) {
                    syncManager.removePendingDeleteSessionIds(listOf(sessionId))
                    syncManager.removeIgnoredRemoteSessionIds(listOf(sessionId))
                    return Result.success("Sesi berhasil dihapus dari perangkat & Google Sheets")
                }
            }
            return Result.success("Sesi dihapus di perangkat (akan dihapus dari Sheets saat sync online)")
        } else {
            if (markIgnored) {
                syncManager.addIgnoredRemoteSessionId(sessionId)
            }
            return Result.success("Sesi berhasil dihapus dari perangkat")
        }
    }

    suspend fun syncPendingData(): Result<SyncResult> {
        val pendingDeletes = syncManager.getPendingDeleteSessionIds().toList()
        if (pendingDeletes.isNotEmpty() && syncManager.isOnline() && syncManager.getGasUrl().isNotBlank()) {
            val delRes = syncManager.deleteSessionsFromSheets(pendingDeletes)
            if (delRes.isSuccess) {
                syncManager.removePendingDeleteSessionIds(pendingDeletes)
                syncManager.removeIgnoredRemoteSessionIds(pendingDeletes)
            }
        }

        val pendingSessions = dao.getPendingSessions()
        val pendingLaps = dao.getPendingLaps()

        // Gunakan seluruh sesi agar setiap lap dijamin memiliki data induk (operator, shift/sesi, line, mesin, gedung)
        val allSessionsMap = dao.getAllSessionsSync().associateBy { it.sessionId }

        val result = syncManager.syncData(pendingSessions, pendingLaps, allSessionsMap)
        if (result.isSuccess) {
            val sessionIds = pendingSessions.map { it.sessionId }
            val lapIds = pendingLaps.map { it.lapId }
            if (sessionIds.isNotEmpty()) dao.markSessionsSynced(sessionIds)
            if (lapIds.isNotEmpty()) dao.markLapsSynced(lapIds)
        }
        return result
    }

    suspend fun getAllLapsAndSessionsForExport(): Pair<List<LapEntity>, Map<String, SessionEntity>> {
        val laps = dao.getAllLapsSync()
        val sessionsList = dao.getAllSessionsSync()
        val map = sessionsList.associateBy { it.sessionId }
        return Pair(laps, map)
    }

    suspend fun getAllSessionsForExport(): List<SessionEntity> {
        return dao.getAllSessionsSync()
    }
}
