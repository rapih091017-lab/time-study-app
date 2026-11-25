package com.example

import com.example.data.model.LapEntity
import com.example.data.model.LapStats
import com.example.data.model.SessionEntity
import com.example.data.sync.ExportHelper
import com.example.data.sync.GoogleAppsScriptSyncManager
import com.example.ui.components.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTimeFormatterSecondsAndCentis() {
        val formatted = TimeFormatter.formatMillis(12340L) // 12 seconds 34 centis
        assertEquals("00:12.34", formatted)
    }

    @Test
    fun testTimeFormatterMinutes() {
        val formatted = TimeFormatter.formatMillis(125480L) // 2 min 5 sec 48 centis
        assertEquals("02:05.48", formatted)
    }

    @Test
    fun testTimeFormatterHours() {
        val formatted = TimeFormatter.formatMillis(3665120L) // 1 hr 1 min 5 sec 12 centis
        assertEquals("01:01:05.12", formatted)
    }

    @Test
    fun testUnitsPerMinuteCalculation() {
        // Laps: 2.0s and 4.0s -> avg = 3.0s -> 60 / 3.0 = 20.0 unit/min
        val stats = LapStats.calculate(listOf(2000L, 4000L))
        assertEquals(2, stats.count)
        assertEquals(3000L, stats.avgDurationMillis)
        assertEquals(20.0, stats.unitsPerMinute, 0.001)
    }

    @Test
    fun testExportCsvHasNoElementColumnAndIncludesObserverAndShift() {
        val session = SessionEntity(
            sessionId = "s1",
            mode = "TS",
            date = "2026-11-18",
            sessionName = "Shift 1",
            operatorName = "Budi",
            observerName = "Andi",
            studyName = "Observasi Cepat",
            line = "Line 1",
            machine = "M1",
            building = "Gedung A"
        )
        val lap = LapEntity(
            lapId = "l1",
            sessionId = "s1",
            elementId = "e1",
            elementName = "Elemen 1",
            sessionName = "Shift 1",
            lapNumber = 1,
            lapDurationMillis = 3000L,
            cumulativeMillis = 3000L
        )
        val csv = ExportHelper.generateLapsCsv(listOf(lap), mapOf("s1" to session))
        val lines = csv.lines()
        val headerLine = lines.first()
        val dataLine = lines[1]
        assertFalse(headerLine.contains("Elemen"))
        assertFalse(headerLine.contains("Sesi / Shift"))
        assertTrue(headerLine.contains("Shift"))
        assertTrue(headerLine.contains("Observer"))
        assertTrue(headerLine.contains("Gedung,Lap No,Waktu Lap (detik)"))
        assertTrue(dataLine.contains("Andi"))
        assertTrue(dataLine.contains("Shift 1"))
    }

    @Test
    fun testDefaultSpreadsheetUrl() {
        assertEquals(
            "https://docs.google.com/spreadsheets/d/1GTs6VgbRK_5cxcHJl7yQMzCMehnl_Qbpfyy89gY-flg/edit?gid=1092414501#gid=1092414501",
            GoogleAppsScriptSyncManager.DEFAULT_SPREADSHEET_URL
        )
    }

    @Test
    fun testAppsScriptContainsDeleteSessionsHandlerAndObserverShiftColumns() {
        val script = com.example.data.sync.GoogleAppsScriptCode.SCRIPT_TEXT
        assertTrue(script.contains("deleteSessions"))
        assertTrue(script.contains("deleteRowsByColumnMatch"))
        assertTrue(script.contains("Observasi Cepat"))
        assertTrue(script.contains("\"Observer\""))
        assertTrue(script.contains("\"Shift\""))
        assertTrue(script.contains("observers"))
    }

    @Test
    fun testHistoryMonthFilterAndSortOrders() {
        val sNovOld = SessionEntity(
            sessionId = "1",
            mode = "TS",
            date = "2026-11-05",
            sessionName = "Shift 1",
            studyName = "Packing Box",
            lapCount = 5,
            createdAt = 100L
        )
        val sOct = SessionEntity(
            sessionId = "2",
            mode = "WLA",
            date = "2026-10-20",
            sessionName = "Shift 2",
            studyName = "Assembly Line",
            lapCount = 12,
            createdAt = 50L
        )
        val sNovNew = SessionEntity(
            sessionId = "3",
            mode = "TS",
            date = "2026-11-19",
            sessionName = "Shift 1",
            studyName = "Cutting Sheet",
            lapCount = 8,
            createdAt = 200L
        )
        val all = listOf(sNovOld, sOct, sNovNew)

        val octFiltered = all.filter { it.date.startsWith("2026-10-") }
        assertEquals(listOf("2"), octFiltered.map { it.sessionId })

        val newestSorted = all.sortedWith(compareByDescending<SessionEntity> { it.date }.thenByDescending { it.createdAt })
        assertEquals(listOf("3", "1", "2"), newestSorted.map { it.sessionId })

        val oldestSorted = all.sortedWith(compareBy<SessionEntity> { it.date }.thenBy { it.createdAt })
        assertEquals(listOf("2", "1", "3"), oldestSorted.map { it.sessionId })

        val mostLapsSorted = all.sortedByDescending { it.lapCount }
        assertEquals(listOf("2", "3", "1"), mostLapsSorted.map { it.sessionId })

        val nameAzSorted = all.sortedBy { it.studyName.lowercase() }
        assertEquals(listOf("2", "3", "1"), nameAzSorted.map { it.sessionId })

        assertEquals(4, com.example.viewmodel.HistorySortOrder.entries.size)
    }
}
