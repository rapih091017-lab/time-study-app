package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.LapEntity
import com.example.data.model.LapStats
import com.example.data.model.MasterOptions
import com.example.data.model.SessionEntity
import com.example.data.model.StopwatchElementEntity
import com.example.data.repository.TimeStudyRepository
import com.example.data.sync.ExportHelper
import com.example.data.sync.GoogleAppsScriptSyncManager
import com.example.data.sync.SyncResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

enum class HistorySortOrder(val label: String) {
    NEWEST("Terbaru"),
    OLDEST("Terlama"),
    MOST_LAPS("Lap Terbanyak"),
    NAME_AZ("Nama A-Z")
}

sealed class Screen {
    object Home : Screen()
    data class Setup(val mode: String) : Screen() // "TS" or "WLA"
    data class SingleStudy(val sessionId: String) : Screen()
    data class WlaStudy(val sessionId: String) : Screen()
    object Database : Screen()
    data class StudyAnalysis(val studyName: String) : Screen()
    object History : Screen()
    data class SessionDetail(val sessionId: String) : Screen()
    object Settings : Screen()
}

data class SetupFormState(
    val operatorName: String = "",
    val observerName: String = "",
    val studyName: String = "",
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val sessionName: String = "Shift 1",
    val line: String = "",
    val machine: String = "",
    val building: String = "",
    val notes: String = "",
    val isStudyNameError: Boolean = false
)

class TimeStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val syncManager = GoogleAppsScriptSyncManager(application)
    val repository = TimeStudyRepository(db.timeStudyDao(), syncManager)

    // Navigation stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>(Screen.Home)

    // Bottom Navigation Active Tab: 0: Home, 1: Database, 2: History, 3: Settings
    private val _selectedNavTab = MutableStateFlow(0)
    val selectedNavTab: StateFlow<Int> = _selectedNavTab.asStateFlow()

    // Form setup state
    private val _formState = MutableStateFlow(SetupFormState())
    val formState: StateFlow<SetupFormState> = _formState.asStateFlow()

    // Active session details
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _activeSession = MutableStateFlow<SessionEntity?>(null)
    val activeSession: StateFlow<SessionEntity?> = _activeSession.asStateFlow()

    private val _elements = MutableStateFlow<List<StopwatchElementEntity>>(emptyList())
    val elements: StateFlow<List<StopwatchElementEntity>> = _elements.asStateFlow()

    private val _laps = MutableStateFlow<List<LapEntity>>(emptyList())
    val laps: StateFlow<List<LapEntity>> = _laps.asStateFlow()

    // Real-time ticking times: ElementId -> Current Elapsed Millis
    private val _liveElapsedTimes = MutableStateFlow<Map<String, Long>>(emptyMap())
    val liveElapsedTimes: StateFlow<Map<String, Long>> = _liveElapsedTimes.asStateFlow()

    // Timing ticker state
    private data class ElementRunningState(
        var accumulatedMillis: Long,
        var isRunning: Boolean,
        var lastStartRealtime: Long
    )
    private val runningStateMap = mutableMapOf<String, ElementRunningState>()
    private var tickerJob: Job? = null

    // Undo management
    private var recentlyDeletedLap: LapEntity? = null
    private val _undoMessageEvent = MutableSharedFlow<String>()
    val undoMessageEvent: SharedFlow<String> = _undoMessageEvent.asSharedFlow()

    // Sync & Network status
    val pendingSessionsCount = repository.pendingSessionsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val pendingLapsCount = repository.pendingLapsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    private val _isOnline = MutableStateFlow(syncManager.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Master Options & Autocomplete
    val masterOptions = repository.getMasterOptions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MasterOptions()
    )

    val datesWithData = repository.distinctDates.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // All sessions
    val allSessions = repository.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Save to master toggle
    private val _saveToMaster = MutableStateFlow(syncManager.isSaveToMasterEnabled())
    val saveToMaster: StateFlow<Boolean> = _saveToMaster.asStateFlow()

    // Date filter on Home
    private val _homeDateFilter = MutableStateFlow("")
    val homeDateFilter: StateFlow<String> = _homeDateFilter.asStateFlow()

    // History filter & sort (supports browsing previous months & sorting)
    private val initialCal = Calendar.getInstance()
    private val _historyYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    val historyYear: StateFlow<Int> = _historyYear.asStateFlow()

    private val _historyMonth = MutableStateFlow(initialCal.get(Calendar.MONTH)) // 0-based
    val historyMonth: StateFlow<Int> = _historyMonth.asStateFlow()

    private val _historyFilterByMonth = MutableStateFlow(false)
    val historyFilterByMonth: StateFlow<Boolean> = _historyFilterByMonth.asStateFlow()

    private val _historyDateFilter = MutableStateFlow("")
    val historyDateFilter: StateFlow<String> = _historyDateFilter.asStateFlow()

    private val _historySortOrder = MutableStateFlow(HistorySortOrder.NEWEST)
    val historySortOrder: StateFlow<HistorySortOrder> = _historySortOrder.asStateFlow()

    // Database lock & Onboarding Guide state
    private val _isDatabaseLocked = MutableStateFlow(syncManager.isDatabaseLocked())
    val isDatabaseLocked: StateFlow<Boolean> = _isDatabaseLocked.asStateFlow()

    private val _showOnboardingGuide = MutableStateFlow(!syncManager.hasSeenGuide())
    val showOnboardingGuide: StateFlow<Boolean> = _showOnboardingGuide.asStateFlow()

    private var lastAutoPullMillis = 0L

    init {
        startNetworkMonitor()
        restoreActiveSessionIfNeeded()
        // Auto-sync master & history from Google Sheets on start if online
        viewModelScope.launch {
            delay(1200)
            autoPullFromCloudIfReady()
        }
    }

    private suspend fun autoPullFromCloudIfReady() {
        if (!syncManager.isOnline() || syncManager.getGasUrl().isBlank()) return
        val now = System.currentTimeMillis()
        if (now - lastAutoPullMillis < 30_000L) return
        lastAutoPullMillis = now
        repository.refreshMasterFromSheets()
        repository.pullDataFromSheets()
    }

    private fun restoreActiveSessionIfNeeded() {
        viewModelScope.launch {
            val savedRoute = syncManager.getActiveScreenRoute()
            val routeDraftSession = if (savedRoute.startsWith("SINGLE:")) {
                val draftId = savedRoute.removePrefix("SINGLE:")
                repository.getSessionById(draftId)?.takeIf { it.status == "DRAFT" }
            } else {
                null
            }

            if (routeDraftSession != null) {
                val draftElements = repository.getElementsForSessionSync(routeDraftSession.sessionId)
                val hasProgress = routeDraftSession.lapCount > 0 ||
                    draftElements.any { it.isRunning || it.elapsedMillis > 0L }
                if (hasProgress) {
                    repository.cleanupEmptyDraftSessions(keepSessionId = routeDraftSession.sessionId)
                    loadSession(routeDraftSession.sessionId)
                    val targetScreen = Screen.SingleStudy(routeDraftSession.sessionId)
                    if (_currentScreen.value == Screen.Home) {
                        screenStack.clear()
                        screenStack.add(Screen.Home)
                        screenStack.add(targetScreen)
                        _currentScreen.value = targetScreen
                    }
                    return@launch
                }
            }

            repository.cleanupEmptyDraftSessions()

            val active = repository.getLatestActiveSession()
            if (active != null) {
                loadSession(active.sessionId)
                val targetScreen = when {
                    savedRoute == "WLA:${active.sessionId}" -> Screen.WlaStudy(active.sessionId)
                    savedRoute == "SINGLE:${active.sessionId}" -> Screen.SingleStudy(active.sessionId)
                    else -> null
                }
                if (targetScreen != null && _currentScreen.value == Screen.Home) {
                    screenStack.clear()
                    screenStack.add(Screen.Home)
                    screenStack.add(targetScreen)
                    _currentScreen.value = targetScreen
                }
            } else if (savedRoute.isNotEmpty()) {
                syncManager.saveActiveScreenRoute("")
            }
        }
    }

    fun onAppBackgrounded() {
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val updatedElements = _elements.value.map { element ->
            val state = runningStateMap[element.elementId]
            if (state != null && state.isRunning) {
                val additional = (nowRealtime - state.lastStartRealtime).coerceAtLeast(0L)
                val totalNow = state.accumulatedMillis + additional
                state.accumulatedMillis = totalNow
                state.lastStartRealtime = nowRealtime
                element.copy(
                    elapsedMillis = totalNow,
                    isRunning = true,
                    lastStartTimeMillis = nowWall
                )
            } else if (state != null) {
                element.copy(
                    elapsedMillis = state.accumulatedMillis,
                    isRunning = false,
                    lastStartTimeMillis = 0L
                )
            } else {
                element
            }
        }
        if (updatedElements.isNotEmpty()) {
            _elements.value = updatedElements
            viewModelScope.launch {
                updatedElements.forEach { repository.updateElement(it) }
            }
        }
    }

    fun onAppForegrounded() {
        if (_activeSession.value == null) {
            restoreActiveSessionIfNeeded()
        } else {
            val nowRealtime = SystemClock.elapsedRealtime()
            val updatedTimes = _liveElapsedTimes.value.toMutableMap()
            runningStateMap.forEach { (elemId, state) ->
                if (state.isRunning) {
                    updatedTimes[elemId] = state.accumulatedMillis + (nowRealtime - state.lastStartRealtime).coerceAtLeast(0L)
                } else {
                    updatedTimes[elemId] = state.accumulatedMillis
                }
            }
            _liveElapsedTimes.value = updatedTimes
            ensureTickerRunning()
        }
        viewModelScope.launch {
            autoPullFromCloudIfReady()
        }
    }

    private fun startNetworkMonitor() {
        viewModelScope.launch {
            while (isActive) {
                _isOnline.value = syncManager.isOnline()
                delay(4000)
            }
        }
    }

    // Navigation
    fun navigateTo(screen: Screen) {
        if (screenStack.lastOrNull() != screen) {
            screenStack.add(screen)
        }
        _currentScreen.value = screen
        updateNavTabFromScreen(screen)
        persistScreenRoute(screen)
    }

    fun selectNavTab(tabIndex: Int) {
        _selectedNavTab.value = tabIndex
        when (tabIndex) {
            0 -> navigateTo(Screen.Home)
            1 -> navigateTo(Screen.Database)
            2 -> navigateTo(Screen.History)
            3 -> navigateTo(Screen.Settings)
        }
    }

    private fun updateNavTabFromScreen(screen: Screen) {
        when (screen) {
            is Screen.Home -> _selectedNavTab.value = 0
            is Screen.Database, is Screen.StudyAnalysis -> _selectedNavTab.value = 1
            is Screen.History, is Screen.SessionDetail -> _selectedNavTab.value = 2
            is Screen.Settings -> _selectedNavTab.value = 3
            else -> {}
        }
    }

    private fun persistScreenRoute(screen: Screen) {
        when (screen) {
            is Screen.SingleStudy -> syncManager.saveActiveScreenRoute("SINGLE:${screen.sessionId}")
            is Screen.WlaStudy -> syncManager.saveActiveScreenRoute("WLA:${screen.sessionId}")
            is Screen.Home -> syncManager.saveActiveScreenRoute("")
            else -> {}
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            val prev = screenStack.last()
            _currentScreen.value = prev
            updateNavTabFromScreen(prev)
            persistScreenRoute(prev)
            return true
        }
        return false
    }

    private fun navigateHomeClearingStack() {
        screenStack.clear()
        screenStack.add(Screen.Home)
        _currentScreen.value = Screen.Home
        _selectedNavTab.value = 0
        syncManager.saveActiveScreenRoute("")
    }

    fun setHomeDateFilter(date: String) {
        _homeDateFilter.value = date
    }

    fun setHistoryMonth(year: Int, month: Int, filterByMonth: Boolean = true) {
        _historyYear.value = year
        _historyMonth.value = month
        _historyFilterByMonth.value = filterByMonth
        // Clear specific day filter if it doesn't belong to the newly chosen month
        val prefix = String.format(Locale.US, "%04d-%02d-", year, month + 1)
        if (_historyDateFilter.value.isNotBlank() && !_historyDateFilter.value.startsWith(prefix)) {
            _historyDateFilter.value = ""
        }
    }

    fun stepHistoryMonth(deltaMonths: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _historyYear.value)
            set(Calendar.MONTH, _historyMonth.value)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        setHistoryMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), filterByMonth = true)
    }

    fun setHistoryFilterByMonth(enabled: Boolean) {
        _historyFilterByMonth.value = enabled
        if (!enabled) {
            _historyDateFilter.value = ""
        }
    }

    fun setHistoryDateFilter(date: String) {
        _historyDateFilter.value = date
        if (date.length >= 7) {
            val parts = date.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull()
            val m = parts.getOrNull(1)?.toIntOrNull()
            if (y != null && m != null) {
                _historyYear.value = y
                _historyMonth.value = (m - 1).coerceIn(0, 11)
                _historyFilterByMonth.value = true
            }
        }
    }

    fun setHistorySortOrder(order: HistorySortOrder) {
        _historySortOrder.value = order
    }

    fun setDatabaseLocked(locked: Boolean) {
        syncManager.setDatabaseLocked(locked)
        _isDatabaseLocked.value = locked
        if (locked) {
            viewModelScope.launch {
                _toastEvent.emit("Database dikunci ke Spreadsheet utama")
            }
        } else {
            viewModelScope.launch {
                _toastEvent.emit("Mode input manual link Spreadsheet diaktifkan")
            }
        }
    }

    fun resetSpreadsheetToDefaultLocked() {
        syncManager.saveSpreadsheetUrl(GoogleAppsScriptSyncManager.DEFAULT_SPREADSHEET_URL)
        syncManager.setDatabaseLocked(true)
        _isDatabaseLocked.value = true
        viewModelScope.launch {
            _toastEvent.emit("Link dikembalikan & dikunci ke database utama")
        }
    }

    fun openOnboardingGuide() {
        _showOnboardingGuide.value = true
    }

    fun dismissOnboardingGuide() {
        syncManager.setHasSeenGuide(true)
        _showOnboardingGuide.value = false
    }

    fun setSaveToMaster(enabled: Boolean) {
        _saveToMaster.value = enabled
        syncManager.setSaveToMaster(enabled)
    }

    /**
     * Fitur Observasi Cepat / Time Study Cepat:
     * Langsung buka stopwatch sebagai DRAFT sementara tanpa langsung masuk Sesi Aktif / Riwayat / Database.
     * Sebelum keluar, user akan diminta konfirmasi apakah hasil observasi mau disimpan atau dibuang.
     */
    fun handleQuickObservationClick(forceNew: Boolean = false) {
        val current = _activeSession.value
        if (!forceNew && current != null && current.status == "DRAFT") {
            navigateTo(Screen.SingleStudy(current.sessionId))
            return
        }
        startQuickObservation()
    }

    fun startQuickObservation() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.cleanupEmptyDraftSessions()
            val countToday = repository.countSessionsForStudyAndDate("Observasi Cepat", today)
            val sessionNum = countToday + 1
            val studyTitle = if (sessionNum > 1) "Observasi Cepat #$sessionNum" else "Observasi Cepat"

            val newSession = SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                mode = "TS",
                date = today,
                sessionName = "Shift $sessionNum",
                operatorName = "",
                observerName = "",
                studyName = studyTitle,
                line = "",
                machine = "",
                building = "",
                notes = "Dibuat via Observasi Cepat ($timeNow)",
                stopwatchCount = 1,
                status = "DRAFT",
                syncStatus = "DRAFT"
            )

            val (createdSession, _) = repository.createSession(newSession, listOf(studyTitle))
            loadSession(createdSession.sessionId)
            navigateTo(Screen.SingleStudy(createdSession.sessionId))
        }
    }

    fun saveQuickObservationAndExit(customStudyName: String = "") {
        val session = _activeSession.value ?: return
        pauseSingleStopwatch()

        val totalDuration = _elements.value.maxOfOrNull {
            _liveElapsedTimes.value[it.elementId] ?: it.elapsedMillis
        } ?: 0L
        val resolvedName = customStudyName.trim().ifBlank { session.studyName }

        tickerJob?.cancel()
        runningStateMap.clear()
        _activeSessionId.value = null
        _activeSession.value = null
        _elements.value = emptyList()
        _laps.value = emptyList()
        _liveElapsedTimes.value = emptyMap()

        viewModelScope.launch {
            repository.completeSession(session.sessionId, totalDuration, resolvedName)
            _toastEvent.emit("Observasi disimpan ke Riwayat!")

            val otherActive = repository.getLatestActiveSession()
            if (otherActive != null) {
                loadSession(otherActive.sessionId)
            }

            if (syncManager.isOnline() && syncManager.getGasUrl().isNotBlank()) {
                syncData(silent = true)
            }
            navigateHomeClearingStack()
        }
    }

    fun promoteQuickDraftToActiveAndReturnHome(customStudyName: String = "") {
        val session = _activeSession.value ?: return
        pauseSingleStopwatch()

        val totalDuration = _elements.value.maxOfOrNull {
            _liveElapsedTimes.value[it.elementId] ?: it.elapsedMillis
        } ?: 0L
        val resolvedName = customStudyName.trim().ifBlank { session.studyName }

        viewModelScope.launch {
            repository.promoteDraftSessionToActive(session.sessionId, totalDuration, resolvedName)
            _activeSession.value = repository.getSessionById(session.sessionId)
            _elements.value = repository.getElementsForSessionSync(session.sessionId)
            _laps.value = repository.getLapsForSessionSync(session.sessionId)
            _toastEvent.emit("Observasi disimpan ke Sesi Aktif")
            navigateHomeClearingStack()
        }
    }

    fun discardQuickObservationAndExit(silent: Boolean = false) {
        val session = _activeSession.value
        tickerJob?.cancel()
        runningStateMap.clear()
        _activeSessionId.value = null
        _activeSession.value = null
        _elements.value = emptyList()
        _laps.value = emptyList()
        _liveElapsedTimes.value = emptyMap()

        viewModelScope.launch {
            if (session != null) {
                repository.deleteSession(
                    sessionId = session.sessionId,
                    deleteFromSheets = false,
                    markIgnored = false
                )
            }
            val otherActive = repository.getLatestActiveSession()
            if (otherActive != null) {
                loadSession(otherActive.sessionId)
            }
            if (!silent) {
                _toastEvent.emit("Observasi cepat tidak disimpan")
            }
            navigateHomeClearingStack()
        }
    }

    fun updateSessionMetadata(
        studyName: String,
        operatorName: String,
        observerName: String = _activeSession.value?.observerName ?: "",
        line: String,
        machine: String,
        building: String,
        sessionName: String
    ) {
        val session = _activeSession.value ?: return
        val updated = session.copy(
            studyName = studyName.trim().ifBlank { session.studyName },
            operatorName = operatorName.trim(),
            observerName = observerName.trim(),
            line = line.trim(),
            machine = machine.trim(),
            building = building.trim(),
            sessionName = sessionName.trim().ifBlank { session.sessionName },
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateSession(updated)
            _activeSession.value = updated
            _toastEvent.emit("Info sesi diperbarui")
        }
    }

    fun openSetup(mode: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _formState.value = SetupFormState(date = today, sessionName = "Shift 1")
        navigateTo(Screen.Setup(mode))
    }

    fun useLastSessionData() {
        viewModelScope.launch {
            val last = repository.getLastSession()
            if (last != null) {
                // Auto calculate next shift number if studyName and date are same
                val countToday = repository.countSessionsForStudyAndDate(last.studyName, last.date)
                val nextSessionName = if (countToday > 0) "Shift ${countToday + 1}" else last.sessionName

                _formState.value = _formState.value.copy(
                    operatorName = last.operatorName,
                    observerName = last.observerName,
                    studyName = last.studyName,
                    sessionName = nextSessionName,
                    line = last.line,
                    machine = last.machine,
                    building = last.building,
                    notes = last.notes,
                    isStudyNameError = false
                )
                _toastEvent.emit("Data sesi terakhir dimuat")
            } else {
                _toastEvent.emit("Belum ada riwayat sesi sebelumnya")
            }
        }
    }

    fun updateFormField(
        operatorName: String? = null,
        observerName: String? = null,
        studyName: String? = null,
        sessionName: String? = null,
        date: String? = null,
        line: String? = null,
        machine: String? = null,
        building: String? = null,
        notes: String? = null
    ) {
        _formState.value = _formState.value.copy(
            operatorName = operatorName ?: _formState.value.operatorName,
            observerName = observerName ?: _formState.value.observerName,
            studyName = studyName ?: _formState.value.studyName,
            sessionName = sessionName ?: _formState.value.sessionName,
            date = date ?: _formState.value.date,
            line = line ?: _formState.value.line,
            machine = machine ?: _formState.value.machine,
            building = building ?: _formState.value.building,
            notes = notes ?: _formState.value.notes,
            isStudyNameError = if (studyName != null) studyName.isBlank() else _formState.value.isStudyNameError
        )
    }

    fun startSessionFromForm(mode: String) {
        val form = _formState.value
        if (form.studyName.isBlank()) {
            _formState.value = form.copy(isStudyNameError = true)
            return
        }

        viewModelScope.launch {
            val newSession = SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                mode = mode,
                date = form.date,
                sessionName = form.sessionName.ifBlank { "Shift 1" }.trim(),
                operatorName = form.operatorName.trim(),
                observerName = form.observerName.trim(),
                studyName = form.studyName.trim(),
                line = form.line.trim(),
                machine = form.machine.trim(),
                building = form.building.trim(),
                notes = form.notes.trim(),
                stopwatchCount = if (mode == "WLA") 3 else 1,
                status = "ACTIVE"
            )

            val initialElements = if (mode == "WLA") {
                listOf("Elemen 1", "Elemen 2", "Elemen 3")
            } else {
                listOf(form.studyName.trim())
            }

            val (createdSession, _) = repository.createSession(newSession, initialElements)
            loadSession(createdSession.sessionId)

            if (mode == "WLA") {
                navigateTo(Screen.WlaStudy(createdSession.sessionId))
            } else {
                navigateTo(Screen.SingleStudy(createdSession.sessionId))
            }
        }
    }

    fun loadSession(sessionId: String) {
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _activeSession.value = session
            if (session != null) {
                val elementsList = repository.getElementsForSessionSync(sessionId)

                // If same session is already running live in memory, don't reset runningStateMap
                val alreadyLiveInMemory = elementsList.isNotEmpty() &&
                    elementsList.all { runningStateMap.containsKey(it.elementId) } &&
                    elementsList.any { runningStateMap[it.elementId]?.isRunning == true }

                if (alreadyLiveInMemory) {
                    _elements.value = elementsList
                } else {
                    _elements.value = elementsList
                    runningStateMap.clear()
                    val currentLiveMap = mutableMapOf<String, Long>()
                    val nowWall = System.currentTimeMillis()
                    val nowRealtime = SystemClock.elapsedRealtime()
                    elementsList.forEach { el ->
                        val effectiveElapsed = if (el.isRunning && el.lastStartTimeMillis > 0L) {
                            val backgroundDelta = (nowWall - el.lastStartTimeMillis).coerceAtLeast(0L)
                            el.elapsedMillis + backgroundDelta
                        } else {
                            el.elapsedMillis
                        }
                        runningStateMap[el.elementId] = ElementRunningState(
                            accumulatedMillis = effectiveElapsed,
                            isRunning = el.isRunning,
                            lastStartRealtime = if (el.isRunning) nowRealtime else 0L
                        )
                        currentLiveMap[el.elementId] = effectiveElapsed
                    }
                    _liveElapsedTimes.value = currentLiveMap
                }

                val lapsList = repository.getLapsForSessionSync(sessionId)
                _laps.value = lapsList

                ensureTickerRunning()
            }
        }
    }

    private fun ensureTickerRunning() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                var anyRunning = false
                val nowRealtime = SystemClock.elapsedRealtime()
                val updatedTimes = mutableMapOf<String, Long>()

                runningStateMap.forEach { (elemId, state) ->
                    if (state.isRunning) {
                        anyRunning = true
                        val currentElapsed = state.accumulatedMillis + (nowRealtime - state.lastStartRealtime)
                        updatedTimes[elemId] = currentElapsed
                    } else {
                        updatedTimes[elemId] = state.accumulatedMillis
                    }
                }
                _liveElapsedTimes.value = updatedTimes

                if (!anyRunning) {
                    delay(100)
                } else {
                    delay(30)
                }
            }
        }
    }

    // Single TS Controls: START, PAUSE, RESUME
    fun startSingleStopwatch() {
        val element = _elements.value.firstOrNull() ?: return
        val state = runningStateMap.getOrPut(element.elementId) {
            ElementRunningState(element.elapsedMillis, false, 0L)
        }
        val nowRealtime = SystemClock.elapsedRealtime()

        state.isRunning = true
        state.lastStartRealtime = nowRealtime
        runningStateMap[element.elementId] = state

        val updatedElement = element.copy(
            elapsedMillis = state.accumulatedMillis,
            isRunning = true,
            lastStartTimeMillis = System.currentTimeMillis()
        )
        _elements.value = _elements.value.map { if (it.elementId == element.elementId) updatedElement else it }

        viewModelScope.launch {
            repository.updateElement(updatedElement)
        }
        ensureTickerRunning()
    }

    fun pauseSingleStopwatch() {
        val element = _elements.value.firstOrNull() ?: return
        val state = runningStateMap[element.elementId] ?: return
        val nowRealtime = SystemClock.elapsedRealtime()

        if (state.isRunning) {
            val additional = (nowRealtime - state.lastStartRealtime).coerceAtLeast(0L)
            state.accumulatedMillis += additional
            state.isRunning = false
            state.lastStartRealtime = 0L
            runningStateMap[element.elementId] = state

            val updatedElement = element.copy(
                elapsedMillis = state.accumulatedMillis,
                isRunning = false,
                lastStartTimeMillis = 0L
            )
            _elements.value = _elements.value.map { if (it.elementId == element.elementId) updatedElement else it }
            _liveElapsedTimes.value = _liveElapsedTimes.value.toMutableMap().apply {
                put(element.elementId, state.accumulatedMillis)
            }

            viewModelScope.launch {
                repository.updateElement(updatedElement)
            }
        }
    }

    fun resumeSingleStopwatch() {
        val element = _elements.value.firstOrNull() ?: return
        val state = runningStateMap.getOrPut(element.elementId) {
            ElementRunningState(element.elapsedMillis, false, 0L)
        }
        if (!state.isRunning) {
            val nowRealtime = SystemClock.elapsedRealtime()
            state.isRunning = true
            state.lastStartRealtime = nowRealtime
            runningStateMap[element.elementId] = state

            val updatedElement = element.copy(
                elapsedMillis = state.accumulatedMillis,
                isRunning = true,
                lastStartTimeMillis = System.currentTimeMillis()
            )
            _elements.value = _elements.value.map { if (it.elementId == element.elementId) updatedElement else it }

            viewModelScope.launch {
                repository.updateElement(updatedElement)
            }
            ensureTickerRunning()
        }
    }

    fun pauseAndReturnToHome() {
        pauseSingleStopwatch()
        pauseAllWla()
        navigateTo(Screen.Home)
    }

    fun recordSingleLap(label: String = "") {
        val session = _activeSession.value ?: return
        val element = _elements.value.firstOrNull() ?: return
        val state = runningStateMap[element.elementId] ?: return

        val nowRealtime = SystemClock.elapsedRealtime()
        val currentCumulative = if (state.isRunning) {
            state.accumulatedMillis + (nowRealtime - state.lastStartRealtime)
        } else {
            state.accumulatedMillis
        }

        val previousLapCumulative = _laps.value.lastOrNull()?.cumulativeMillis ?: 0L
        val lapDuration = (currentCumulative - previousLapCumulative).coerceAtLeast(0L)

        viewModelScope.launch {
            val newLap = repository.recordLap(
                sessionId = session.sessionId,
                elementId = element.elementId,
                elementName = element.name,
                sessionName = session.sessionName,
                lapDurationMillis = lapDuration,
                cumulativeMillis = currentCumulative,
                label = label
            )
            _laps.value = _laps.value + newLap
        }
    }

    /**
     * Requirement 3: RESET saves old session (if laps exist) and immediately starts a BRAND NEW session
     * with the same setup parameters, incremented session number, timer at 00:00.00, and empty laps!
     */
    fun resetSingleStopwatchChaining() {
        val session = _activeSession.value ?: return
        val elementsList = _elements.value
        val totalDuration = _liveElapsedTimes.value[elementsList.firstOrNull()?.elementId] ?: 0L

        viewModelScope.launch {
            if (_laps.value.isNotEmpty()) {
                repository.completeSession(session.sessionId, totalDuration)
            }

            val (newSession, _) = repository.createNextSession(session, elementsList)
            loadSession(newSession.sessionId)
            _toastEvent.emit("Memulai sesi baru: ${newSession.sessionName}")
        }
    }

    // WLA Controls: Separate START, PAUSE, RESUME, RESET, COMPLETE
    fun startWlaElement(elementId: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val state = runningStateMap[elementId] ?: ElementRunningState(element.elapsedMillis, false, 0L)
        val nowRealtime = SystemClock.elapsedRealtime()

        state.isRunning = true
        state.lastStartRealtime = nowRealtime
        runningStateMap[elementId] = state

        val updatedElement = element.copy(
            elapsedMillis = state.accumulatedMillis,
            isRunning = true,
            lastStartTimeMillis = System.currentTimeMillis(),
            isCompleted = false
        )
        _elements.value = _elements.value.map { if (it.elementId == elementId) updatedElement else it }

        viewModelScope.launch {
            repository.updateElement(updatedElement)
        }
        ensureTickerRunning()
    }

    fun pauseWlaElement(elementId: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val state = runningStateMap[elementId] ?: return
        val nowRealtime = SystemClock.elapsedRealtime()

        if (state.isRunning) {
            val additional = nowRealtime - state.lastStartRealtime
            state.accumulatedMillis += additional
            state.isRunning = false
            runningStateMap[elementId] = state

            val updatedElement = element.copy(
                elapsedMillis = state.accumulatedMillis,
                isRunning = false,
                lastStartTimeMillis = 0L
            )
            _elements.value = _elements.value.map { if (it.elementId == elementId) updatedElement else it }

            viewModelScope.launch {
                repository.updateElement(updatedElement)
            }
        }
    }

    fun resumeWlaElement(elementId: String) {
        // Melanjutkan waktu akumulasi dari titik jeda terakhir tanpa mereset
        startWlaElement(elementId)
    }

    fun resetWlaElement(elementId: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val state = runningStateMap[elementId] ?: ElementRunningState(0L, false, 0L)
        state.accumulatedMillis = 0L
        state.isRunning = false
        state.lastStartRealtime = 0L
        runningStateMap[elementId] = state

        val updatedMap = _liveElapsedTimes.value.toMutableMap()
        updatedMap[elementId] = 0L
        _liveElapsedTimes.value = updatedMap

        val updatedElement = element.copy(
            elapsedMillis = 0L,
            isRunning = false,
            lastStartTimeMillis = 0L,
            isCompleted = false
        )
        _elements.value = _elements.value.map { if (it.elementId == elementId) updatedElement else it }

        viewModelScope.launch {
            repository.updateElement(updatedElement)
            repository.deleteLapsForElement(element.sessionId, elementId)
            _laps.value = repository.getLapsForSessionSync(element.sessionId)
            _toastEvent.emit("Stopwatch '${element.name}' direset")
        }
    }

    fun completeWlaElement(elementId: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val state = runningStateMap[elementId] ?: return
        val session = _activeSession.value ?: return
        val nowRealtime = SystemClock.elapsedRealtime()

        val finalCumulative = if (state.isRunning) {
            val additional = nowRealtime - state.lastStartRealtime
            state.accumulatedMillis += additional
            state.accumulatedMillis
        } else {
            state.accumulatedMillis
        }
        state.isRunning = false
        runningStateMap[elementId] = state

        val updatedMap = _liveElapsedTimes.value.toMutableMap()
        updatedMap[elementId] = finalCumulative
        _liveElapsedTimes.value = updatedMap

        val updatedElement = element.copy(
            elapsedMillis = finalCumulative,
            isRunning = false,
            lastStartTimeMillis = 0L,
            isCompleted = true
        )
        _elements.value = _elements.value.map { if (it.elementId == elementId) updatedElement else it }

        viewModelScope.launch {
            repository.updateElement(updatedElement)

            // Catat lap terakhir secara otomatis jika terdapat durasi sejak lap sebelumnya
            val elementLaps = _laps.value.filter { it.elementId == elementId }
            val prevCumulative = elementLaps.lastOrNull()?.cumulativeMillis ?: 0L
            val lapDuration = finalCumulative - prevCumulative
            if (lapDuration > 0) {
                val newLap = repository.recordLap(
                    sessionId = session.sessionId,
                    elementId = element.elementId,
                    elementName = element.name,
                    sessionName = session.sessionName,
                    lapDurationMillis = lapDuration,
                    cumulativeMillis = finalCumulative,
                    label = "Selesai"
                )
                _laps.value = _laps.value + newLap
            }

            _toastEvent.emit("Elemen '${element.name}' selesai!")
        }
    }

    fun reopenWlaElement(elementId: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val updatedElement = element.copy(isCompleted = false)
        _elements.value = _elements.value.map { if (it.elementId == elementId) updatedElement else it }
        viewModelScope.launch {
            repository.updateElement(updatedElement)
            _toastEvent.emit("Stopwatch '${element.name}' dibuka kembali")
        }
    }

    fun startAllWla() {
        val nowRealtime = SystemClock.elapsedRealtime()
        val updatedList = _elements.value.map { element ->
            if (!element.isCompleted) {
                val state = runningStateMap[element.elementId] ?: ElementRunningState(element.elapsedMillis, false, 0L)
                state.isRunning = true
                state.lastStartRealtime = nowRealtime
                runningStateMap[element.elementId] = state
                element.copy(
                    elapsedMillis = state.accumulatedMillis,
                    isRunning = true,
                    lastStartTimeMillis = System.currentTimeMillis()
                )
            } else {
                element
            }
        }
        _elements.value = updatedList
        viewModelScope.launch {
            updatedList.forEach { repository.updateElement(it) }
        }
        ensureTickerRunning()
    }

    fun pauseAllWla() {
        val nowRealtime = SystemClock.elapsedRealtime()
        val updatedList = _elements.value.map { element ->
            val state = runningStateMap[element.elementId]
            if (state != null && state.isRunning) {
                val additional = nowRealtime - state.lastStartRealtime
                state.accumulatedMillis += additional
                state.isRunning = false
                runningStateMap[element.elementId] = state
                element.copy(
                    elapsedMillis = state.accumulatedMillis,
                    isRunning = false,
                    lastStartTimeMillis = 0L
                )
            } else {
                element
            }
        }
        _elements.value = updatedList
        viewModelScope.launch {
            updatedList.forEach { repository.updateElement(it) }
        }
    }

    fun resumeAllWla() {
        startAllWla()
    }

    fun recordWlaLap(elementId: String, label: String = "") {
        val session = _activeSession.value ?: return
        val element = _elements.value.find { it.elementId == elementId } ?: return
        val state = runningStateMap[elementId] ?: return

        val nowRealtime = SystemClock.elapsedRealtime()
        val currentCumulative = if (state.isRunning) {
            state.accumulatedMillis + (nowRealtime - state.lastStartRealtime)
        } else {
            state.accumulatedMillis
        }

        val elementLaps = _laps.value.filter { it.elementId == elementId }
        val prevCumulative = elementLaps.lastOrNull()?.cumulativeMillis ?: 0L
        val lapDuration = (currentCumulative - prevCumulative).coerceAtLeast(0L)

        viewModelScope.launch {
            val newLap = repository.recordLap(
                sessionId = session.sessionId,
                elementId = element.elementId,
                elementName = element.name,
                sessionName = session.sessionName,
                lapDurationMillis = lapDuration,
                cumulativeMillis = currentCumulative,
                label = label
            )
            _laps.value = _laps.value + newLap
        }
    }

    fun resetWlaChaining() {
        val session = _activeSession.value ?: return
        val elementsList = _elements.value
        val totalDuration = elementsList.maxOfOrNull {
            _liveElapsedTimes.value[it.elementId] ?: it.elapsedMillis
        } ?: 0L

        viewModelScope.launch {
            if (_laps.value.isNotEmpty()) {
                repository.completeSession(session.sessionId, totalDuration)
            }
            val (newSession, _) = repository.createNextSession(session, elementsList)
            loadSession(newSession.sessionId)
            _toastEvent.emit("Memulai sesi WLA baru: ${newSession.sessionName}")
        }
    }

    fun addWlaElement(name: String) {
        val session = _activeSession.value ?: return
        val colors = listOf("#F6C445", "#2F7F6F", "#F26A7E", "#CFC4F7", "#A9C8FA", "#F5A94A")
        val colorHex = colors[_elements.value.size % colors.size]

        viewModelScope.launch {
            val added = repository.addElement(session.sessionId, name, colorHex)
            _elements.value = _elements.value + added
            runningStateMap[added.elementId] = ElementRunningState(0L, false, 0L)
            _liveElapsedTimes.value = _liveElapsedTimes.value.toMutableMap().apply {
                put(added.elementId, 0L)
            }
            _toastEvent.emit("Stopwatch '${added.name}' ditambahkan")
        }
    }

    fun renameWlaElement(elementId: String, newName: String) {
        val element = _elements.value.find { it.elementId == elementId } ?: return
        viewModelScope.launch {
            repository.updateElement(element.copy(name = newName.trim()))
            _elements.value = _elements.value.map {
                if (it.elementId == elementId) it.copy(name = newName.trim()) else it
            }
        }
    }

    fun deleteWlaElement(elementId: String) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            repository.deleteElement(elementId, session.sessionId)
            _elements.value = _elements.value.filter { it.elementId != elementId }
            _laps.value = _laps.value.filter { it.elementId != elementId }
            runningStateMap.remove(elementId)
            _toastEvent.emit("Stopwatch berhasil dihapus")
        }
    }

    // Lap Deletion & Undo
    fun deleteLap(lap: LapEntity) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            val deleted = repository.deleteLap(lap.lapId, lap.elementId, session.sessionId)
            if (deleted != null) {
                recentlyDeletedLap = deleted
                _laps.value = repository.getLapsForSessionSync(session.sessionId)
                _undoMessageEvent.emit("Lap #${lap.lapNumber} dihapus")
            }
        }
    }

    fun undoDeleteLap() {
        val lapToRestore = recentlyDeletedLap ?: return
        val session = _activeSession.value ?: return
        recentlyDeletedLap = null
        viewModelScope.launch {
            repository.restoreLap(lapToRestore, session.sessionId)
            _laps.value = repository.getLapsForSessionSync(session.sessionId)
            _toastEvent.emit("Lap #${lapToRestore.lapNumber} dipulihkan")
        }
    }

    fun updateLapLabel(lapId: String, newLabel: String) {
        viewModelScope.launch {
            repository.updateLapLabel(lapId, newLabel.trim())
            val session = _activeSession.value ?: return@launch
            _laps.value = repository.getLapsForSessionSync(session.sessionId)
        }
    }

    /**
     * Requirement 3: SELESAI saves session and AUTOMATICALLY returns to Home with toast "Sesi tersimpan"
     */
    fun finishSessionToHome() {
        val session = _activeSession.value ?: return
        pauseSingleStopwatch()
        pauseAllWla()

        val totalDuration = _elements.value.maxOfOrNull {
            _liveElapsedTimes.value[it.elementId] ?: it.elapsedMillis
        } ?: 0L

        tickerJob?.cancel()
        runningStateMap.clear()
        _activeSessionId.value = null
        _activeSession.value = null
        _elements.value = emptyList()
        _laps.value = emptyList()
        _liveElapsedTimes.value = emptyMap()

        viewModelScope.launch {
            repository.completeSession(session.sessionId, totalDuration)
            syncManager.saveActiveScreenRoute("")
            _toastEvent.emit("Sesi tersimpan!")

            val otherActive = repository.getLatestActiveSession()
            if (otherActive != null) {
                loadSession(otherActive.sessionId)
            }

            if (syncManager.isOnline() && syncManager.getGasUrl().isNotBlank()) {
                syncData(silent = true)
            }
            navigateHomeClearingStack()
        }
    }

    fun deleteSession(
        sessionId: String,
        deleteFromSheets: Boolean = false,
        navigateBackOnComplete: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.deleteSession(sessionId, deleteFromSheets)
            if (_activeSessionId.value == sessionId) {
                tickerJob?.cancel()
                runningStateMap.clear()
                _activeSessionId.value = null
                _activeSession.value = null
                _elements.value = emptyList()
                _laps.value = emptyList()
                _liveElapsedTimes.value = emptyMap()
                syncManager.saveActiveScreenRoute("")
            }
            _toastEvent.emit(result.getOrNull() ?: "Sesi berhasil dihapus")
            if (navigateBackOnComplete) {
                navigateBack()
            }
        }
    }

    // Statistics with Standard Deviation and Unit/Min calculation
    fun calculateStats(lapsList: List<LapEntity>): LapStats {
        return LapStats.calculate(lapsList.map { it.lapDurationMillis })
    }

    // Pull & Sync
    fun refreshMasterData() {
        viewModelScope.launch {
            _toastEvent.emit("Menyegarkan master data...")
            val res = repository.refreshMasterFromSheets()
            if (res.isSuccess) {
                _toastEvent.emit("Master data berhasil diperbarui dari Sheets!")
            } else {
                _toastEvent.emit("Gagal refresh: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun pullSessionsFromSheets() {
        if (_syncing.value) return
        _syncing.value = true
        viewModelScope.launch {
            _toastEvent.emit("Menarik riwayat terbaru dari Google Sheets...")
            repository.refreshMasterFromSheets()
            val res = repository.pullDataFromSheets()
            _syncing.value = false
            if (res.isSuccess) {
                lastAutoPullMillis = System.currentTimeMillis()
                _toastEvent.emit(res.getOrNull() ?: "Data berhasil diunduh!")
            } else {
                _toastEvent.emit("Gagal: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun syncData(silent: Boolean = false) {
        if (_syncing.value) return
        _syncing.value = true

        viewModelScope.launch {
            val pushResult = repository.syncPendingData()
            val pullResult = repository.pullDataFromSheets()
            repository.refreshMasterFromSheets()
            _syncing.value = false
            if (pushResult.isSuccess && pullResult.isSuccess) {
                lastAutoPullMillis = System.currentTimeMillis()
                val pushRes = pushResult.getOrNull()
                val pullMsg = pullResult.getOrNull() ?: ""
                if (!silent) {
                    val pushMsg = if ((pushRes?.syncedSessions ?: 0) > 0 || (pushRes?.syncedLaps ?: 0) > 0) {
                        "Upload ${pushRes?.syncedSessions} sesi & ${pushRes?.syncedLaps} lap. "
                    } else ""
                    _toastEvent.emit((pushMsg + pullMsg).ifBlank { "Sinkronisasi 2 arah berhasil!" })
                }
            } else if (pushResult.isSuccess) {
                if (!silent) {
                    _toastEvent.emit(pushResult.getOrNull()?.message ?: "Upload berhasil")
                }
            } else {
                if (!silent) {
                    _toastEvent.emit(
                        pushResult.exceptionOrNull()?.message
                            ?: pullResult.exceptionOrNull()?.message
                            ?: "Sinkronisasi gagal"
                    )
                }
            }
        }
    }

    fun openDatabaseSpreadsheet(context: Context) {
        val url = syncManager.getSpreadsheetUrl()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Tidak dapat membuka link Google Sheets: ${e.localizedMessage ?: "Browser tidak ditemukan"}")
            }
        }
    }

    fun testGasConnection(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.testConnection(url)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: "Koneksi Berhasil!")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Gagal")
            }
        }
    }

    // Export Helpers
    fun exportAllData(isExcel: Boolean) {
        viewModelScope.launch {
            val (laps, sessionsMap) = repository.getAllLapsAndSessionsForExport()
            val sessionsList = repository.getAllSessionsForExport()
            if (laps.isEmpty() && sessionsList.isEmpty()) {
                _toastEvent.emit("Tidak ada data untuk diekspor")
                return@launch
            }

            if (isExcel) {
                val bytes = ExportHelper.createXlsxFile(laps, sessionsMap, sessionsList)
                val res = ExportHelper.shareExportFile(getApplication(), bytes, "TimeStudy_All", "xlsx")
                if (res.isSuccess) {
                    _toastEvent.emit("Export Excel berhasil!")
                } else {
                    // Fallback to TSV clipboard
                    val tsv = ExportHelper.generateTabSeparatedLaps(laps, sessionsMap)
                    ExportHelper.copyToClipboard(getApplication(), tsv)
                    _toastEvent.emit("Gagal buka file. Data disalin ke clipboard!")
                }
            } else {
                val csv = ExportHelper.generateLapsCsv(laps, sessionsMap)
                val res = ExportHelper.shareExportFile(getApplication(), csv.toByteArray(Charsets.UTF_8), "TimeStudy_All", "csv")
                if (res.isSuccess) {
                    _toastEvent.emit("Export CSV berhasil!")
                } else {
                    ExportHelper.copyToClipboard(getApplication(), csv)
                    _toastEvent.emit("Gagal buka file. CSV disalin ke clipboard!")
                }
            }
        }
    }

    fun exportSessionData(sessionId: String, isExcel: Boolean) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            val laps = repository.getLapsForSessionSync(sessionId)
            val map = mapOf(session.sessionId to session)
            val cleanName = session.studyName.replace("\\s+".toRegex(), "_")

            if (isExcel) {
                val bytes = ExportHelper.createXlsxFile(laps, map, listOf(session))
                val res = ExportHelper.shareExportFile(getApplication(), bytes, "TimeStudy_${cleanName}", "xlsx")
                if (res.isSuccess) {
                    _toastEvent.emit("Export Excel berhasil!")
                } else {
                    val tsv = ExportHelper.generateTabSeparatedLaps(laps, map)
                    ExportHelper.copyToClipboard(getApplication(), tsv)
                    _toastEvent.emit("Data sesi disalin ke clipboard (TSV)")
                }
            } else {
                val csv = ExportHelper.generateLapsCsv(laps, map)
                val res = ExportHelper.shareExportFile(getApplication(), csv.toByteArray(Charsets.UTF_8), "TimeStudy_${cleanName}", "csv")
                if (res.isSuccess) {
                    _toastEvent.emit("Export CSV berhasil!")
                } else {
                    ExportHelper.copyToClipboard(getApplication(), csv)
                    _toastEvent.emit("CSV disalin ke clipboard")
                }
            }
        }
    }

    fun copySessionTsvToClipboard(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            val laps = repository.getLapsForSessionSync(sessionId)
            val tsv = ExportHelper.generateTabSeparatedLaps(laps, mapOf(session.sessionId to session))
            ExportHelper.copyToClipboard(getApplication(), tsv)
            _toastEvent.emit("Tabel data disalin ke clipboard (bisa di-paste ke Excel/Sheets)")
        }
    }

    fun exportSessionsSummary(isExcel: Boolean) {
        viewModelScope.launch {
            val sessionsList = repository.getAllSessionsForExport()
            val laps = emptyList<LapEntity>()
            val sessionsMap = sessionsList.associateBy { it.sessionId }
            if (isExcel) {
                val bytes = ExportHelper.createXlsxFile(laps, sessionsMap, sessionsList)
                val res = ExportHelper.shareExportFile(getApplication(), bytes, "TimeStudy_Summary", "xlsx")
                if (res.isSuccess) {
                    _toastEvent.emit("Export Ringkasan Excel berhasil!")
                } else {
                    val csv = ExportHelper.generateSessionsCsv(sessionsList)
                    ExportHelper.copyToClipboard(getApplication(), csv)
                    _toastEvent.emit("Gagal buka file. Data disalin ke clipboard!")
                }
            } else {
                val csv = ExportHelper.generateSessionsCsv(sessionsList)
                val res = ExportHelper.shareExportFile(getApplication(), csv.toByteArray(Charsets.UTF_8), "TimeStudy_Summary", "csv")
                if (res.isSuccess) {
                    _toastEvent.emit("Export Ringkasan CSV berhasil!")
                } else {
                    ExportHelper.copyToClipboard(getApplication(), csv)
                    _toastEvent.emit("CSV disalin ke clipboard!")
                }
            }
        }
    }
}
