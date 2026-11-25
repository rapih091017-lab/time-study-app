package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.TimeStudyDao
import com.example.data.model.SessionEntity
import com.example.data.repository.TimeStudyRepository
import com.example.data.sync.GoogleAppsScriptSyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class QuickObservationDraftTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TimeStudyDao
    private lateinit var syncManager: GoogleAppsScriptSyncManager
    private lateinit var repository: TimeStudyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("time_study_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.timeStudyDao()
        syncManager = GoogleAppsScriptSyncManager(context)
        repository = TimeStudyRepository(dao, syncManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun draftSessionAndLapsAreExcludedFromActiveHistoryAndSyncUntilSaved() = runBlocking {
        val draft = SessionEntity(
            sessionId = "draft-1",
            mode = "TS",
            date = "2026-11-24",
            sessionName = "Shift 1",
            studyName = "Observasi Cepat",
            status = "DRAFT",
            syncStatus = "DRAFT"
        )
        val (_, elements) = repository.createSession(draft, listOf("Observasi Cepat"))
        val element = elements.first()

        val lap = repository.recordLap(
            sessionId = draft.sessionId,
            elementId = element.elementId,
            elementName = element.name,
            sessionName = draft.sessionName,
            lapDurationMillis = 2500L,
            cumulativeMillis = 2500L
        )

        assertEquals("DRAFT", lap.syncStatus)
        assertNull(repository.getLatestActiveSession())
        assertTrue(dao.getAllSessionsSync().isEmpty())
        assertTrue(dao.getAllLapsSync().isEmpty())
        assertTrue(dao.getPendingSessions().isEmpty())
        assertTrue(dao.getPendingLaps().isEmpty())
        assertEquals(0, repository.pendingSessionsCount.first())
        assertEquals(0, repository.pendingLapsCount.first())

        // Complete & save to History with custom name
        repository.completeSession(draft.sessionId, 3000L, "Pasang Baut Cepat")

        val completed = repository.getSessionById(draft.sessionId)
        assertNotNull(completed)
        assertEquals("COMPLETED", completed?.status)
        assertEquals("PENDING", completed?.syncStatus)
        assertEquals("Pasang Baut Cepat", completed?.studyName)
        assertEquals(1, dao.getAllSessionsSync().size)
        assertEquals(1, dao.getPendingSessions().size)
        assertEquals(1, dao.getPendingLaps().size)
        assertEquals("Pasang Baut Cepat", dao.getPendingLaps().first().elementName)
    }

    @Test
    fun promoteDraftToActiveMovesDraftIntoActiveBannerAndPendingSync() = runBlocking {
        val draft = SessionEntity(
            sessionId = "draft-2",
            mode = "TS",
            date = "2026-11-24",
            sessionName = "Shift 1",
            studyName = "Observasi Cepat",
            status = "DRAFT",
            syncStatus = "DRAFT"
        )
        val (_, elements) = repository.createSession(draft, listOf("Observasi Cepat"))
        repository.recordLap(
            sessionId = draft.sessionId,
            elementId = elements.first().elementId,
            elementName = elements.first().name,
            sessionName = draft.sessionName,
            lapDurationMillis = 1500L,
            cumulativeMillis = 1500L
        )

        assertNull(repository.getLatestActiveSession())

        repository.promoteDraftSessionToActive(draft.sessionId, 1800L, "Cek Dimensi")

        val active = repository.getLatestActiveSession()
        assertNotNull(active)
        assertEquals("ACTIVE", active?.status)
        assertEquals("PENDING", active?.syncStatus)
        assertEquals("Cek Dimensi", active?.studyName)
        assertEquals(1, dao.getPendingLaps().size)
    }

    @Test
    fun discardDraftDeletesSessionAndLapsCleanlyWithoutAffectingHistory() = runBlocking {
        val draft = SessionEntity(
            sessionId = "draft-3",
            mode = "TS",
            date = "2026-11-24",
            sessionName = "Shift 1",
            studyName = "Observasi Cepat",
            status = "DRAFT",
            syncStatus = "DRAFT"
        )
        val (_, elements) = repository.createSession(draft, listOf("Observasi Cepat"))
        repository.recordLap(
            sessionId = draft.sessionId,
            elementId = elements.first().elementId,
            elementName = elements.first().name,
            sessionName = draft.sessionName,
            lapDurationMillis = 1200L,
            cumulativeMillis = 1200L
        )

        repository.deleteSession(draft.sessionId, deleteFromSheets = false, markIgnored = false)

        assertNull(repository.getSessionById(draft.sessionId))
        assertTrue(repository.getLapsForSessionSync(draft.sessionId).isEmpty())
        assertTrue(dao.getAllSessionsSync().isEmpty())
        assertTrue(syncManager.getIgnoredRemoteSessionIds().isEmpty())
    }

    @Test
    fun cleanupEmptyDraftSessionsRemovesOnlyEmptyOrUnkeptDrafts() = runBlocking {
        val emptyDraft = SessionEntity(
            sessionId = "draft-empty",
            mode = "TS",
            date = "2026-11-24",
            sessionName = "Shift 1",
            studyName = "Observasi Cepat",
            status = "DRAFT",
            syncStatus = "DRAFT"
        )
        val keptEmptyDraft = SessionEntity(
            sessionId = "draft-keep",
            mode = "TS",
            date = "2026-11-24",
            sessionName = "Shift 2",
            studyName = "Observasi Cepat #2",
            status = "DRAFT",
            syncStatus = "DRAFT"
        )
        repository.createSession(emptyDraft, listOf("Observasi Cepat"))
        repository.createSession(keptEmptyDraft, listOf("Observasi Cepat #2"))

        repository.cleanupEmptyDraftSessions(keepSessionId = keptEmptyDraft.sessionId)

        assertNull(repository.getSessionById(emptyDraft.sessionId))
        assertNotNull(repository.getSessionById(keptEmptyDraft.sessionId))
    }
}
