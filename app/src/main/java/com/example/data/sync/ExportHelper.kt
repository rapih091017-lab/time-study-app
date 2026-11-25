package com.example.data.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.LapEntity
import com.example.data.model.SessionEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun generateLapsCsv(
        laps: List<LapEntity>,
        sessionsMap: Map<String, SessionEntity>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens it with perfect UTF-8 encoding
        sb.append("\uFEFF")
        sb.append("Lap ID,Session ID,Mode,Tanggal,Shift,Observer,Operator,Nama Time Study,Line,Mesin,Gedung,Lap No,Waktu Lap (detik),Waktu Kumulatif (detik),Label Lap,Timestamp Catat\n")

        for (lap in laps) {
            val session = sessionsMap[lap.sessionId]
            val mode = session?.mode ?: "TS"
            val date = session?.date ?: ""
            val sessionName = lap.sessionName.ifBlank { session?.sessionName ?: "Shift 1" }
            val observer = session?.observerName ?: ""
            val operator = session?.operatorName ?: ""
            val study = session?.studyName ?: ""
            val line = session?.line ?: ""
            val machine = session?.machine ?: ""
            val building = session?.building ?: ""
            val lapNo = lap.lapNumber
            val lapSec = String.format(Locale.US, "%.2f", lap.lapDurationMillis / 1000.0)
            val cumSec = String.format(Locale.US, "%.2f", lap.cumulativeMillis / 1000.0)
            val label = escapeCsv(lap.label)
            val timeStr = dateFormat.format(Date(lap.timestampMillis))

            sb.append("${lap.lapId},${lap.sessionId},\"$mode\",\"$date\",\"${escapeCsv(sessionName)}\",\"${escapeCsv(observer)}\",\"${escapeCsv(operator)}\",\"${escapeCsv(study)}\",\"${escapeCsv(line)}\",\"${escapeCsv(machine)}\",\"${escapeCsv(building)}\",$lapNo,$lapSec,$cumSec,\"$label\",\"$timeStr\"\n")
        }
        return sb.toString()
    }

    fun generateSessionsCsv(sessions: List<SessionEntity>): String {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("Session ID,Mode,Tanggal,Shift,Observer,Operator,Nama Time Study,Line,Mesin,Gedung,Catatan,Jumlah Stopwatch,Jumlah Lap,Total Durasi (detik),Status,Dibuat Pada\n")

        for (s in sessions) {
            val durSec = String.format(Locale.US, "%.2f", s.totalDurationMillis / 1000.0)
            val createdStr = dateFormat.format(Date(s.createdAt))
            sb.append("${s.sessionId},\"${s.mode}\",\"${s.date}\",\"${escapeCsv(s.sessionName)}\",\"${escapeCsv(s.observerName)}\",\"${escapeCsv(s.operatorName)}\",\"${escapeCsv(s.studyName)}\",\"${escapeCsv(s.line)}\",\"${escapeCsv(s.machine)}\",\"${escapeCsv(s.building)}\",\"${escapeCsv(s.notes)}\",${s.stopwatchCount},${s.lapCount},$durSec,\"${s.status}\",\"$createdStr\"\n")
        }
        return sb.toString()
    }

    fun generateTabSeparatedLaps(
        laps: List<LapEntity>,
        sessionsMap: Map<String, SessionEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("Lap ID\tSession ID\tMode\tTanggal\tShift\tObserver\tOperator\tNama Time Study\tLine\tMesin\tGedung\tLap No\tWaktu Lap (detik)\tWaktu Kumulatif (detik)\tLabel\tTimestamp\n")
        for (lap in laps) {
            val s = sessionsMap[lap.sessionId]
            val mode = s?.mode ?: "TS"
            val date = s?.date ?: ""
            val sessionName = lap.sessionName.ifBlank { s?.sessionName ?: "Shift 1" }
            val observer = s?.observerName ?: ""
            val operator = s?.operatorName ?: ""
            val study = s?.studyName ?: ""
            val line = s?.line ?: ""
            val machine = s?.machine ?: ""
            val building = s?.building ?: ""
            val lapNo = lap.lapNumber
            val lapSec = String.format(Locale.US, "%.2f", lap.lapDurationMillis / 1000.0)
            val cumSec = String.format(Locale.US, "%.2f", lap.cumulativeMillis / 1000.0)
            val label = lap.label.replace("\t", " ")
            val timeStr = dateFormat.format(Date(lap.timestampMillis))
            sb.append("${lap.lapId}\t${lap.sessionId}\t$mode\t$date\t$sessionName\t$observer\t$operator\t$study\t$line\t$machine\t$building\t$lapNo\t$lapSec\t$cumSec\t$label\t$timeStr\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Builds genuine OpenXML (.xlsx) file in memory containing Sheet 1 "Laps" and Sheet 2 "Sessions".
     */
    fun createXlsxFile(
        laps: List<LapEntity>,
        sessionsMap: Map<String, SessionEntity>,
        allSessionsList: List<SessionEntity>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->

            // 1. [Content_Types].xml
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            addZipEntry(zos, "[Content_Types].xml", contentTypes)

            // 2. _rels/.rels
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            addZipEntry(zos, "_rels/.rels", rels)

            // 3. xl/_rels/workbook.xml.rels
            val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            addZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRels)

            // 4. xl/workbook.xml
            val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Laps" sheetId="1" r:id="rId1"/>
    <sheet name="Sessions" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""
            addZipEntry(zos, "xl/workbook.xml", workbook)

            // 5. xl/styles.xml
            val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><name val="Calibri"/><sz val="11"/></font></fonts>
  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
            addZipEntry(zos, "xl/styles.xml", styles)

            // 6. xl/worksheets/sheet1.xml (Laps)
            val sheet1Builder = StringBuilder()
            sheet1Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")
            // Header Row
            val lapHeaders = listOf(
                "Lap ID", "Session ID", "Mode", "Tanggal", "Shift", "Observer", "Operator",
                "Nama Time Study", "Line", "Mesin", "Gedung",
                "Lap No", "Waktu Lap (detik)", "Waktu Kumulatif (detik)", "Label Lap", "Timestamp Catat"
            )
            appendXmlRow(sheet1Builder, 1, lapHeaders)

            var rowIdx = 2
            for (lap in laps) {
                val session = sessionsMap[lap.sessionId]
                val mode = session?.mode ?: "TS"
                val date = session?.date ?: ""
                val sessionName = lap.sessionName.ifBlank { session?.sessionName ?: "Shift 1" }
                val observer = session?.observerName ?: ""
                val operator = session?.operatorName ?: ""
                val study = session?.studyName ?: ""
                val line = session?.line ?: ""
                val machine = session?.machine ?: ""
                val building = session?.building ?: ""
                val lapNo = lap.lapNumber.toString()
                val lapSec = String.format(Locale.US, "%.2f", lap.lapDurationMillis / 1000.0)
                val cumSec = String.format(Locale.US, "%.2f", lap.cumulativeMillis / 1000.0)
                val label = lap.label
                val timeStr = dateFormat.format(Date(lap.timestampMillis))

                val rowValues = listOf(
                    lap.lapId, lap.sessionId, mode, date, sessionName, observer, operator,
                    study, line, machine, building, lapNo, lapSec, cumSec, label, timeStr
                )
                appendXmlRow(sheet1Builder, rowIdx, rowValues)
                rowIdx++
            }
            sheet1Builder.append("""  </sheetData>
</worksheet>""")
            addZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Builder.toString())

            // 7. xl/worksheets/sheet2.xml (Sessions)
            val sheet2Builder = StringBuilder()
            sheet2Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")
            val sessionHeaders = listOf(
                "Session ID", "Mode", "Tanggal", "Shift", "Observer", "Operator", "Nama Time Study",
                "Line", "Mesin", "Gedung", "Catatan", "Jumlah Stopwatch", "Jumlah Lap",
                "Total Durasi (detik)", "Status", "Dibuat Pada"
            )
            appendXmlRow(sheet2Builder, 1, sessionHeaders)

            var sRowIdx = 2
            val sessionList = if (allSessionsList.isNotEmpty()) allSessionsList else sessionsMap.values.toList()
            for (s in sessionList) {
                val durSec = String.format(Locale.US, "%.2f", s.totalDurationMillis / 1000.0)
                val createdStr = dateFormat.format(Date(s.createdAt))
                val rowValues = listOf(
                    s.sessionId, s.mode, s.date, s.sessionName, s.observerName, s.operatorName, s.studyName,
                    s.line, s.machine, s.building, s.notes, s.stopwatchCount.toString(),
                    s.lapCount.toString(), durSec, s.status, createdStr
                )
                appendXmlRow(sheet2Builder, sRowIdx, rowValues)
                sRowIdx++
            }
            sheet2Builder.append("""  </sheetData>
</worksheet>""")
            addZipEntry(zos, "xl/worksheets/sheet2.xml", sheet2Builder.toString())
        }

        return baos.toByteArray()
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun appendXmlRow(sb: StringBuilder, rowIndex: Int, values: List<String>) {
        sb.append("    <row r=\"$rowIndex\">\n")
        values.forEachIndexed { colIdx, value ->
            val colLetter = getColumnLetter(colIdx)
            val cellRef = "$colLetter$rowIndex"
            sb.append("      <c r=\"$cellRef\" t=\"inlineStr\"><is><t>")
            sb.append(escapeXml(value))
            sb.append("</t></is></c>\n")
        }
        sb.append("    </row>\n")
    }

    private fun getColumnLetter(colIndex: Int): String {
        var num = colIndex
        var colName = ""
        while (num >= 0) {
            colName = ('A' + (num % 26)) + colName
            num = (num / 26) - 1
        }
        return colName
    }

    fun shareExportFile(
        context: Context,
        bytes: ByteArray,
        filenamePrefix: String,
        extension: String
    ): Result<String> {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${filenamePrefix}_$timeStamp.$extension"

            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mime = if (extension == "xlsx") {
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            } else {
                "text/csv"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Ekspor Data Time Study").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success("Export berhasil: $fileName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Time Study Data"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }
}
