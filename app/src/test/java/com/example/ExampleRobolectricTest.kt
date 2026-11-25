package com.example

import com.example.data.model.LapEntity
import com.example.data.model.SessionEntity
import com.example.data.sync.ExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleModelTest {

    @Test
    fun testSessionEntityCreation() {
        val session = SessionEntity(
            sessionId = "test-uuid-1",
            mode = "TS",
            date = "2026-11-05",
            sessionName = "Shift 2",
            operatorName = "Budi",
            observerName = "Sari",
            studyName = "Perakitan Modul",
            line = "Line 01",
            machine = "M-01",
            building = "Gedung A"
        )
        assertEquals("test-uuid-1", session.sessionId)
        assertEquals("TS", session.mode)
        assertEquals("Shift 2", session.sessionName)
        assertEquals("Sari", session.observerName)
        assertEquals("Perakitan Modul", session.studyName)
        assertEquals("ACTIVE", session.status)
        assertEquals("PENDING", session.syncStatus)
    }

    @Test
    fun testLapDurationCalculations() {
        val lap1 = LapEntity(
            lapId = "lap-1",
            sessionId = "sess-1",
            elementId = "elem-1",
            elementName = "Elemen A",
            sessionName = "Shift 1",
            lapNumber = 1,
            lapDurationMillis = 5400L,
            cumulativeMillis = 5400L
        )
        val lap2 = LapEntity(
            lapId = "lap-2",
            sessionId = "sess-1",
            elementId = "elem-1",
            elementName = "Elemen A",
            sessionName = "Shift 1",
            lapNumber = 2,
            lapDurationMillis = 4200L,
            cumulativeMillis = 9600L
        )
        assertEquals(5400L, lap1.lapDurationMillis)
        assertEquals(4200L, lap2.lapDurationMillis)
        assertEquals(9600L, lap2.cumulativeMillis)
        assertEquals("Shift 1", lap2.sessionName)
    }

    @Test
    fun testXlsxGenerationInMemory() {
        val session = SessionEntity(
            sessionId = "sess-100",
            mode = "TS",
            date = "2026-11-05",
            sessionName = "Shift 1",
            operatorName = "Rudi",
            observerName = "Andi",
            studyName = "Cutting Kain"
        )
        val lap = LapEntity(
            lapId = "lap-100",
            sessionId = "sess-100",
            elementId = "elem-1",
            elementName = "Cutting",
            sessionName = "Shift 1",
            lapNumber = 1,
            lapDurationMillis = 3200L,
            cumulativeMillis = 3200L
        )
        val xlsxBytes = ExportHelper.createXlsxFile(
            listOf(lap),
            mapOf(session.sessionId to session),
            listOf(session)
        )
        // Verify genuine ZIP / OpenXML header PK (0x50, 0x4B)
        assertTrue(xlsxBytes.size > 200)
        assertEquals(0x50.toByte(), xlsxBytes[0])
        assertEquals(0x4B.toByte(), xlsxBytes[1])
    }
}
