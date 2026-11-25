package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.sync.GoogleAppsScriptSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SyncManagerRobolectricTest {

    private lateinit var context: Context
    private lateinit var syncManager: GoogleAppsScriptSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("time_study_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        syncManager = GoogleAppsScriptSyncManager(context)
    }

    @Test
    fun testDatabaseLockedByDefaultAndSupportsManualOverride() {
        assertTrue(syncManager.isDatabaseLocked())
        assertEquals(GoogleAppsScriptSyncManager.DEFAULT_SPREADSHEET_URL, syncManager.getSpreadsheetUrl())

        // Even if a custom URL is saved while locked, locked mode enforces DEFAULT_SPREADSHEET_URL
        val customUrl = "https://docs.google.com/spreadsheets/d/CUSTOM_ID/edit"
        syncManager.saveSpreadsheetUrl(customUrl)
        assertEquals(GoogleAppsScriptSyncManager.DEFAULT_SPREADSHEET_URL, syncManager.getSpreadsheetUrl())

        // Unlocking switches to manual override
        syncManager.setDatabaseLocked(false)
        assertFalse(syncManager.isDatabaseLocked())
        assertEquals(customUrl, syncManager.getSpreadsheetUrl())

        // Re-locking enforces DEFAULT_SPREADSHEET_URL again
        syncManager.setDatabaseLocked(true)
        assertTrue(syncManager.isDatabaseLocked())
        assertEquals(GoogleAppsScriptSyncManager.DEFAULT_SPREADSHEET_URL, syncManager.getSpreadsheetUrl())
    }

    @Test
    fun testOnboardingGuideDefaultUnseenAndCanBeDismissed() {
        assertFalse(syncManager.hasSeenGuide())
        syncManager.setHasSeenGuide(true)
        assertTrue(syncManager.hasSeenGuide())
    }
}
