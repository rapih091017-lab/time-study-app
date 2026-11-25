package com.example.data.sync

object GoogleAppsScriptCode {

    const val SCRIPT_TEXT = """/**
 * Google Apps Script v2 untuk Time Study & WLA App
 * Mendukung:
 * 1. doPost: Upsert idempotent ke sheet "Laps" & "Sessions" + update otomatis ke sheet "Master"
 * 2. doGet: action=master (daftar dropdown), action=sessions, action=laps, action=ping
 */

function doGet(e) {
  try {
    var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "ping";
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. Tes Koneksi
    if (action === "ping") {
      return jsonResponse({
        status: "success",
        message: "Koneksi berhasil! Google Apps Script siap digunakan."
      });
    }

    // 2. Ambil Master Data untuk Dropdown Combobox
    if (action === "master") {
      var masterSheet = ss.getSheetByName("Master");
      if (!masterSheet) {
        masterSheet = initMasterSheet(ss);
      }
      var lastRow = masterSheet.getLastRow();
      var lastCol = Math.max(masterSheet.getLastColumn(), 6);
      var data = lastRow > 1 ? masterSheet.getRange(2, 1, lastRow - 1, Math.min(lastCol, 7)).getValues() : [];

      var operators = [];
      var studyNames = [];
      var lines = [];
      var machines = [];
      var buildings = [];
      var sessions = [];
      var observers = [];

      for (var i = 0; i < data.length; i++) {
        if (data[i][0]) operators.push(String(data[i][0]).trim());
        if (data[i][1]) studyNames.push(String(data[i][1]).trim());
        if (data[i][2]) lines.push(String(data[i][2]).trim());
        if (data[i][3]) machines.push(String(data[i][3]).trim());
        if (data[i][4]) buildings.push(String(data[i][4]).trim());
        if (data[i][5]) sessions.push(String(data[i][5]).trim());
        if (data[i].length > 6 && data[i][6]) observers.push(String(data[i][6]).trim());
      }

      return jsonResponse({
        status: "success",
        data: {
          operators: uniqueList(operators),
          studyNames: uniqueList(studyNames),
          lines: uniqueList(lines),
          machines: uniqueList(machines),
          buildings: uniqueList(buildings),
          sessions: uniqueList(sessions),
          observers: uniqueList(observers)
        }
      });
    }

    // 3. Tarik Data Sessions dari Sheets
    if (action === "sessions") {
      var sSheet = ss.getSheetByName("Sessions");
      if (!sSheet || sSheet.getLastRow() <= 1) {
        return jsonResponse({ status: "success", sessions: [] });
      }
      var sReadCols = Math.min(Math.max(sSheet.getLastColumn(), 15), 16);
      var sValues = sSheet.getRange(2, 1, sSheet.getLastRow() - 1, sReadCols).getValues();
      var sessionsList = [];
      for (var s = 0; s < sValues.length; s++) {
        var row = sValues[s];
        if (!row[0]) continue;
        var dateStr = formatDateYmd(row[2]);
        var syncedMillis = parseTimestampMillis(row[14], row[2]);
        sessionsList.push({
          sessionId: String(row[0]),
          mode: String(row[1] || "TS"),
          date: dateStr,
          sessionName: String(row[3] || "Shift 1"),
          operatorName: String(row[4] || ""),
          studyName: String(row[5] || ""),
          line: String(row[6] || ""),
          machine: String(row[7] || ""),
          building: String(row[8] || ""),
          notes: String(row[9] || ""),
          stopwatchCount: parseInt(row[10]) || 1,
          lapCount: parseInt(row[11]) || 0,
          totalDurationMillis: Math.round((parseFloat(row[12]) || 0) * 1000),
          status: String(row[13] || "COMPLETED"),
          observerName: sReadCols >= 16 ? String(row[15] || "") : "",
          createdAt: syncedMillis,
          updatedAt: syncedMillis
        });
      }
      return jsonResponse({ status: "success", sessions: sessionsList });
    }

    // 4. Tarik Data Laps dari Sheets
    if (action === "laps") {
      var lSheet = ss.getSheetByName("Laps");
      if (!lSheet || lSheet.getLastRow() <= 1) {
        return jsonResponse({ status: "success", laps: [] });
      }
      var lLastCol = Math.min(Math.max(lSheet.getLastColumn(), 15), 17);
      var lHeaders = lSheet.getRange(1, 1, 1, lLastCol).getValues()[0];
      var col10Header = String(lHeaders[10] || "").trim().toLowerCase();
      var hasElemCol = col10Header.indexOf("elemen") !== -1 || col10Header.indexOf("stopwatch") !== -1;
      var colOffset = hasElemCol ? 1 : 0;
      var lReadCols = Math.min(lLastCol, 16 + colOffset);
      var lValues = lSheet.getRange(2, 1, lSheet.getLastRow() - 1, lReadCols).getValues();
      var lapsList = [];
      for (var l = 0; l < lValues.length; l++) {
        var lRow = lValues[l];
        if (!lRow[0]) continue;
        var lDateStr = formatDateYmd(lRow[3]);
        var lTimeMillis = parseTimestampMillis(lRow[14 + colOffset], lRow[3]);
        lapsList.push({
          lapId: String(lRow[0]),
          sessionId: String(lRow[1]),
          mode: String(lRow[2] || "TS"),
          date: lDateStr,
          sessionName: String(lRow[4] || "Shift 1"),
          operatorName: String(lRow[5] || ""),
          studyName: String(lRow[6] || ""),
          line: String(lRow[7] || ""),
          machine: String(lRow[8] || ""),
          building: String(lRow[9] || ""),
          elementName: hasElemCol ? String(lRow[10] || "") : "",
          lapNumber: parseInt(lRow[10 + colOffset]) || 1,
          lapDurationMillis: Math.round((parseFloat(lRow[11 + colOffset]) || 0) * 1000),
          cumulativeMillis: Math.round((parseFloat(lRow[12 + colOffset]) || 0) * 1000),
          label: String(lRow[13 + colOffset] || ""),
          timestampMillis: lTimeMillis
        });
      }
      return jsonResponse({ status: "success", laps: lapsList });
    }

    return jsonResponse({ status: "error", message: "Action tidak dikenal" });
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000);

    var rawData = e.postData.contents;
    var data = JSON.parse(rawData);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    if (data.action === "ping") {
      lock.releaseLock();
      return jsonResponse({ status: "success", message: "Koneksi POST berhasil!" });
    }

    if (data.action === "deleteSessions") {
      var targetIds = data.sessionIds || (data.sessionId ? [data.sessionId] : []);
      var idMap = {};
      for (var d = 0; d < targetIds.length; d++) {
        var tid = String(targetIds[d] || "").trim();
        if (tid) idMap[tid] = true;
      }
      var deletedRows = 0;
      deletedRows += deleteRowsByColumnMatch(ss.getSheetByName("Sessions"), 1, idMap);
      deletedRows += deleteRowsByColumnMatch(ss.getSheetByName("Laps"), 2, idMap);
      deletedRows += deleteRowsByColumnMatch(ss.getSheetByName("WLA"), 1, idMap);
      deletedRows += deleteRowsByColumnMatch(ss.getSheetByName("Time Study"), 1, idMap);
      deletedRows += deleteRowsByColumnMatch(ss.getSheetByName("Observasi Cepat"), 1, idMap);
      lock.releaseLock();
      return jsonResponse({
        status: "success",
        deletedRows: deletedRows,
        message: "Berhasil menghapus " + targetIds.length + " sesi (" + deletedRows + " baris) di Google Sheets."
      });
    }

    var nowStr = Utilities.formatDate(new Date(), "Asia/Jakarta", "yyyy-MM-dd HH:mm:ss");

    // Definisi Header Resmi
    var sessionHeaders = [
      "Session ID", "Mode", "Tanggal", "Shift", "Operator", "Nama Time Study", 
      "Line", "Mesin", "Gedung", "Catatan", "Jumlah Stopwatch", 
      "Jumlah Lap", "Total Durasi (detik)", "Status", "Waktu Sync", "Observer"
    ];

    var lapHeaders = [
      "Lap ID", "Session ID", "Mode", "Tanggal", "Shift", "Operator", 
      "Nama Time Study", "Line", "Mesin", "Gedung", 
      "Lap No", "Waktu Lap (detik)", "Waktu Kumulatif (detik)", "Label Lap", "Timestamp Catat", "Observer"
    ];

    // Inisialisasi Sheet Sessions & Laps dengan validasi header otomatis
    var sessionSheet = ss.getSheetByName("Sessions");
    if (!sessionSheet) {
      sessionSheet = ss.insertSheet("Sessions");
    }
    ensureSheetHeaders(sessionSheet, sessionHeaders, "#F1F5F9");

    var lapSheet = ss.getSheetByName("Laps");
    if (!lapSheet) {
      lapSheet = ss.insertSheet("Laps");
    }
    migrateLapSheetRemoveElementColumn(lapSheet);
    ensureSheetHeaders(lapSheet, lapHeaders, "#F1F5F9");

    // Inisialisasi Sheet Master & Sheet Khusus
    var masterSheet = ss.getSheetByName("Master");
    if (!masterSheet) {
      masterSheet = initMasterSheet(ss);
    } else {
      initMasterSheet(ss);
    }
    var wlaSheet = initWlaSheet(ss);
    var tsSheet = initTimeStudySheet(ss);
    var quickSheet = initQuickObsSheet(ss);

    // Bangun Session Lookup Map untuk menjamin kolom info sesi pada Laps selalu terisi lengkap
    var sLookup = {};

    // 1. Ambil data sesi yang sudah ada di sheet Sessions
    var sLastRow = sessionSheet.getLastRow();
    var sDataCols = Math.min(Math.max(sessionSheet.getLastColumn(), 10), 16);
    var sData = sLastRow > 1 ? sessionSheet.getRange(2, 1, sLastRow - 1, sDataCols).getValues() : [];
    var sRowMap = {};
    for (var i = 0; i < sData.length; i++) {
      var existId = String(sData[i][0]).trim();
      if (existId) {
        sRowMap[existId] = i + 2;
        sLookup[existId] = {
          mode: sData[i][1] || "TS",
          date: sData[i][2] || "",
          sessionName: sData[i][3] || "Shift 1",
          operatorName: sData[i][4] || "",
          studyName: sData[i][5] || "",
          line: sData[i][6] || "",
          machine: sData[i][7] || "",
          building: sData[i][8] || "",
          observerName: sDataCols >= 16 ? (sData[i][15] || "") : ""
        };
      }
    }

    // 2. Simpan Sesi (Idempotent berdasarkan Session ID)
    var sessions = data.sessions || [];
    if (sessions.length > 0) {
      for (var s = 0; s < sessions.length; s++) {
        var item = sessions[s];
        var sName = item.sessionName || item.shift || "Shift 1";
        var opName = item.operatorName || item.operator || "";
        var obsName = item.observerName || item.observer || "";
        var stName = item.studyName || "";
        var ln = item.line || "";
        var mc = item.machine || "";
        var bg = item.building || "";
        var md = item.mode || "TS";
        var dt = item.date || "";

        // Update lookup map
        sLookup[item.sessionId] = {
          mode: md,
          date: dt,
          sessionName: sName,
          operatorName: opName,
          observerName: obsName,
          studyName: stName,
          line: ln,
          machine: mc,
          building: bg
        };

        // Raw Session row (16 kolom)
        var sRow = [
          item.sessionId, md, dt, sName, opName, stName, ln, mc, bg,
          item.notes || "", item.stopwatchCount || 1, item.lapCount || 0,
          item.totalDurationSeconds || 0, item.status || "COMPLETED", nowStr, obsName
        ];

        if (sRowMap[item.sessionId]) {
          sessionSheet.getRange(sRowMap[item.sessionId], 1, 1, sRow.length).setValues([sRow]);
        } else {
          sessionSheet.appendRow(sRow);
          sRowMap[item.sessionId] = sessionSheet.getLastRow();
        }

        // Upsert ke Sheet Khusus berdasarkan kategori
        if (md === "WLA") {
          var wRow = [
            item.sessionId, dt, sName, opName, stName, ln, mc, bg,
            item.stopwatchCount || 1, item.lapCount || 0,
            item.totalDurationSeconds || 0, item.status || "COMPLETED", nowStr, obsName
          ];
          upsertById(wlaSheet, item.sessionId, wRow);
        } else {
          var isQuick = (stName && stName.toLowerCase().indexOf("observasi cepat") !== -1) ||
                        (item.notes && item.notes.toLowerCase().indexOf("observasi cepat") !== -1);
          if (isQuick) {
            var qRow = [
              item.sessionId, dt, sName, stName, item.lapCount || 0,
              item.totalDurationSeconds || 0, item.notes || "", item.status || "COMPLETED", nowStr, obsName
            ];
            upsertById(quickSheet, item.sessionId, qRow);
          } else {
            var tRow = [
              item.sessionId, dt, sName, opName, stName, ln, mc, bg,
              item.lapCount || 0, item.totalDurationSeconds || 0, item.status || "COMPLETED", nowStr, obsName
            ];
            upsertById(tsSheet, item.sessionId, tRow);
          }
        }
      }
    }

    // 3. Simpan Laps (Idempotent berdasarkan Lap ID) dengan validasi & fallback ke parent session
    var laps = data.laps || [];
    if (laps.length > 0) {
      var lLastRow = lapSheet.getLastRow();
      var lData = lLastRow > 1 ? lapSheet.getRange(2, 1, lLastRow - 1, 1).getValues() : [];
      var lRowMap = {};
      for (var j = 0; j < lData.length; j++) {
        var existLapId = String(lData[j][0]).trim();
        if (existLapId) {
          lRowMap[existLapId] = j + 2;
        }
      }

      for (var l = 0; l < laps.length; l++) {
        var lap = laps[l];
        var parent = sLookup[lap.sessionId] || null;

        var lMode = lap.mode || (parent ? parent.mode : "TS");
        var lDate = lap.date || (parent ? parent.date : "");
        var lSessionName = lap.sessionName || lap.shift || (parent ? parent.sessionName : "Shift 1");
        var lOperatorName = lap.operatorName || lap.operator || (parent ? parent.operatorName : "");
        var lObserverName = lap.observerName || lap.observer || (parent ? parent.observerName : "");
        var lStudyName = lap.studyName || (parent ? parent.studyName : "");
        var lLine = lap.line || (parent ? parent.line : "");
        var lMachine = lap.machine || (parent ? parent.machine : "");
        var lBuilding = lap.building || (parent ? parent.building : "");
        var lLapNo = lap.lapNumber || 1;
        var lLapDuration = typeof lap.lapDurationSeconds === "number" ? lap.lapDurationSeconds : (parseFloat(lap.lapDurationSeconds) || 0);
        var lCumulative = typeof lap.cumulativeSeconds === "number" ? lap.cumulativeSeconds : (parseFloat(lap.cumulativeSeconds) || 0);
        var lLabel = lap.label || "";
        var lTimestamp = lap.timestampFormatted || nowStr;

        // 16 Kolom persis sesuai header Laps
        var lRow = [
          lap.lapId, lap.sessionId, lMode, lDate, lSessionName, lOperatorName,
          lStudyName, lLine, lMachine, lBuilding,
          lLapNo, lLapDuration, lCumulative, lLabel, lTimestamp, lObserverName
        ];

        if (lRowMap[lap.lapId]) {
          lapSheet.getRange(lRowMap[lap.lapId], 1, 1, lRow.length).setValues([lRow]);
        } else {
          lapSheet.appendRow(lRow);
          lRowMap[lap.lapId] = lapSheet.getLastRow();
        }
      }
    }

    // Jalankan perbaikan otomatis baris lama di Laps yang mungkin belum lengkap
    perbaikiDanLengkapiSheetLaps();

    // 4. Tulis Nilai Baru ke Sheet Master (jika saveToMaster = true)
    if (data.saveToMaster) {
      appendNewMasterValues(masterSheet, sessions);
    }

    lock.releaseLock();
    return jsonResponse({
      status: "success",
      syncedSessions: sessions.length,
      syncedLaps: laps.length,
      message: "Berhasil sinkronisasi " + sessions.length + " sesi dan " + laps.length + " lap ke Google Sheets (Data Gedung, Mesin, Operator, Observer, Shift telah divalidasi)."
    });
  } catch (err) {
    if (lock) lock.releaseLock();
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

/**
 * Memastikan baris 1 sheet memiliki header yang lengkap dan benar.
 * Jika sheet sudah pernah dibuat tapi kolomnya kurang/berbeda, header akan diperbarui.
 */
function ensureSheetHeaders(sheet, expectedHeaders, bgColor) {
  if (!sheet) return;
  var lastRow = sheet.getLastRow();
  var lastCol = sheet.getLastColumn();

  if (lastRow === 0) {
    sheet.appendRow(expectedHeaders);
    sheet.getRange(1, 1, 1, expectedHeaders.length).setFontWeight("bold").setBackground(bgColor || "#F1F5F9");
    return;
  }

  // Periksa apakah header saat ini sudah sesuai
  var currentHeaders = sheet.getRange(1, 1, 1, Math.max(lastCol, expectedHeaders.length)).getValues()[0];
  var needUpdate = false;
  for (var i = 0; i < expectedHeaders.length; i++) {
    if (String(currentHeaders[i] || "").trim() !== expectedHeaders[i]) {
      needUpdate = true;
      break;
    }
  }

  if (needUpdate) {
    sheet.getRange(1, 1, 1, expectedHeaders.length).setValues([expectedHeaders]).setFontWeight("bold").setBackground(bgColor || "#F1F5F9");
  }
}

/**
 * Menghapus kolom lama "Nama Stopwatch/Elemen" (kolom K / indeks 10) jika masih ada di sheet Laps lama.
 */
function migrateLapSheetRemoveElementColumn(lapSheet) {
  if (!lapSheet || lapSheet.getLastRow() === 0 || lapSheet.getLastColumn() < 11) return;
  var colKHeader = String(lapSheet.getRange(1, 11).getValue() || "").trim().toLowerCase();
  if (colKHeader.indexOf("elemen") !== -1 || colKHeader.indexOf("stopwatch") !== -1) {
    lapSheet.deleteColumn(11);
  }
}

/**
 * FUNGSI PERBAIKAN OTOMATIS:
 * Menyelaraskan sheet Laps dengan sheet Sessions.
 * Jika ada baris di sheet Laps yang kolom Operator, Observer, Mesin, Gedung, Line, atau Shift-nya masih kosong,
 * fungsi ini akan mengambil nilainya dari sheet Sessions berdasarkan Session ID!
 * Bisa dipanggil manual dari editor Apps Script via tombol Run.
 */
function perbaikiDanLengkapiSheetLaps() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;

  var sessionSheet = ss.getSheetByName("Sessions");
  var lapSheet = ss.getSheetByName("Laps");
  if (!sessionSheet || !lapSheet) return;

  var sessionHeaders = [
    "Session ID", "Mode", "Tanggal", "Shift", "Operator", "Nama Time Study", 
    "Line", "Mesin", "Gedung", "Catatan", "Jumlah Stopwatch", 
    "Jumlah Lap", "Total Durasi (detik)", "Status", "Waktu Sync", "Observer"
  ];
  ensureSheetHeaders(sessionSheet, sessionHeaders, "#F1F5F9");

  migrateLapSheetRemoveElementColumn(lapSheet);
  var lapHeaders = [
    "Lap ID", "Session ID", "Mode", "Tanggal", "Shift", "Operator", 
    "Nama Time Study", "Line", "Mesin", "Gedung", 
    "Lap No", "Waktu Lap (detik)", "Waktu Kumulatif (detik)", "Label Lap", "Timestamp Catat", "Observer"
  ];
  ensureSheetHeaders(lapSheet, lapHeaders, "#F1F5F9");

  var sLast = sessionSheet.getLastRow();
  if (sLast <= 1) return;

  // Bangun peta data induk sesi: Session ID -> metadata
  var sReadCols = Math.min(Math.max(sessionSheet.getLastColumn(), 10), 16);
  var sData = sessionSheet.getRange(2, 1, sLast - 1, sReadCols).getValues();
  var sessionMap = {};
  for (var i = 0; i < sData.length; i++) {
    var sid = String(sData[i][0]).trim();
    if (sid) {
      sessionMap[sid] = {
        mode: sData[i][1] || "TS",
        date: sData[i][2] || "",
        sessionName: sData[i][3] || "Shift 1",
        operator: sData[i][4] || "",
        studyName: sData[i][5] || "",
        line: sData[i][6] || "",
        machine: sData[i][7] || "",
        building: sData[i][8] || "",
        observer: sReadCols >= 16 ? (sData[i][15] || "") : ""
      };
    }
  }

  // Telusuri baris Laps dan isi yang kosong
  var lLast = lapSheet.getLastRow();
  if (lLast <= 1) return;

  var lRange = lapSheet.getRange(2, 1, lLast - 1, 16);
  var lVals = lRange.getValues();
  var anyChanged = false;

  for (var j = 0; j < lVals.length; j++) {
    var row = lVals[j];
    var lSessionId = String(row[1]).trim();
    var parent = sessionMap[lSessionId];
    if (parent) {
      // Index 2: Mode
      if (!row[2] && parent.mode) { row[2] = parent.mode; anyChanged = true; }
      // Index 3: Tanggal
      if (!row[3] && parent.date) { row[3] = parent.date; anyChanged = true; }
      // Index 4: Shift
      if (!row[4] && parent.sessionName) { row[4] = parent.sessionName; anyChanged = true; }
      // Index 5: Operator
      if (!row[5] && parent.operator) { row[5] = parent.operator; anyChanged = true; }
      // Index 6: Nama Time Study
      if (!row[6] && parent.studyName) { row[6] = parent.studyName; anyChanged = true; }
      // Index 7: Line
      if (!row[7] && parent.line) { row[7] = parent.line; anyChanged = true; }
      // Index 8: Mesin
      if (!row[8] && parent.machine) { row[8] = parent.machine; anyChanged = true; }
      // Index 9: Gedung
      if (!row[9] && parent.building) { row[9] = parent.building; anyChanged = true; }
      // Index 15: Observer
      if (!row[15] && parent.observer) { row[15] = parent.observer; anyChanged = true; }
    }
  }

  if (anyChanged) {
    lRange.setValues(lVals);
    Logger.log("Berhasil memperbarui data Operator/Observer/Mesin/Gedung/Shift yang kosong di sheet Laps.");
  }
}

function initMasterSheet(ss) {
  if (!ss) {
    ss = SpreadsheetApp.getActiveSpreadsheet();
  }
  if (!ss) {
    throw new Error("Spreadsheet tidak ditemukan! Pastikan Google Apps Script ini dibuka dari spreadsheet Google Sheets Anda (Menu: Ekstensi > Apps Script).");
  }
  var headers = ["Operator", "Time Study", "Line", "Mesin", "Gedung", "Shift", "Observer"];
  var sheet = ss.getSheetByName("Master");
  if (!sheet) {
    sheet = ss.insertSheet("Master");
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#E2F3F0");
    sheet.appendRow(["Budi Santoso", "Perakitan Modul", "Line 01", "CNC-01", "Gedung A", "Shift 1", "Andi (IE)"]);
    sheet.appendRow(["Siti Aminah", "Packaging Box", "Line 02", "Press-02", "Gedung B", "Shift 2", "Rina (IE)"]);
  } else {
    ensureSheetHeaders(sheet, headers, "#E2F3F0");
  }
  return sheet;
}

function initWlaSheet(ss) {
  if (!ss) ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return null;
  var headers = [
    "Session ID", "Tanggal", "Shift", "Operator", "Nama Time Study", 
    "Line", "Mesin", "Gedung", "Jumlah Stopwatch", "Jumlah Lap", 
    "Total Durasi (detik)", "Status", "Waktu Sync", "Observer"
  ];
  var sheet = ss.getSheetByName("WLA");
  if (!sheet) {
    sheet = ss.insertSheet("WLA");
  }
  ensureSheetHeaders(sheet, headers, "#E8F4FD");
  return sheet;
}

function initTimeStudySheet(ss) {
  if (!ss) ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return null;
  var headers = [
    "Session ID", "Tanggal", "Shift", "Operator", "Nama Time Study", 
    "Line", "Mesin", "Gedung", "Jumlah Lap", "Total Durasi (detik)", 
    "Status", "Waktu Sync", "Observer"
  ];
  var sheet = ss.getSheetByName("Time Study");
  if (!sheet) {
    sheet = ss.insertSheet("Time Study");
  }
  ensureSheetHeaders(sheet, headers, "#FFF3CD");
  return sheet;
}

function initQuickObsSheet(ss) {
  if (!ss) ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return null;
  var headers = [
    "Session ID", "Tanggal", "Shift", "Nama Observasi Cepat", 
    "Jumlah Lap", "Total Durasi (detik)", "Catatan", "Status", "Waktu Sync", "Observer"
  ];
  var sheet = ss.getSheetByName("Observasi Cepat");
  if (!sheet) {
    sheet = ss.insertSheet("Observasi Cepat");
  }
  ensureSheetHeaders(sheet, headers, "#FDE8ED");
  return sheet;
}

function upsertById(sheet, idVal, rowData) {
  if (!sheet) return;
  var lastRow = sheet.getLastRow();
  var data = lastRow > 1 ? sheet.getRange(2, 1, lastRow - 1, 1).getValues() : [];
  for (var i = 0; i < data.length; i++) {
    if (String(data[i][0]) === String(idVal)) {
      sheet.getRange(i + 2, 1, 1, rowData.length).setValues([rowData]);
      return;
    }
  }
  sheet.appendRow(rowData);
}

/**
 * Jalankan fungsi ini langsung dari editor Apps Script (pilih fungsi ini lalu klik 'Run/Jalankan')
 * untuk membuat/menyiapkan SEMUA sheet secara manual serta memperbaiki data lap lama!
 */
function inisialisasiSemuaSheet() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) {
    throw new Error("Spreadsheet tidak ditemukan! Pastikan membuka Apps Script via menu 'Ekstensi > Apps Script' di Google Sheets.");
  }

  // 1. Raw Data & Master
  initMasterSheet(ss);

  var sessionHeaders = [
    "Session ID", "Mode", "Tanggal", "Shift", "Operator", "Nama Time Study", 
    "Line", "Mesin", "Gedung", "Catatan", "Jumlah Stopwatch", 
    "Jumlah Lap", "Total Durasi (detik)", "Status", "Waktu Sync", "Observer"
  ];
  var sessionSheet = ss.getSheetByName("Sessions");
  if (!sessionSheet) {
    sessionSheet = ss.insertSheet("Sessions");
  }
  ensureSheetHeaders(sessionSheet, sessionHeaders, "#F1F5F9");

  var lapHeaders = [
    "Lap ID", "Session ID", "Mode", "Tanggal", "Shift", "Operator", 
    "Nama Time Study", "Line", "Mesin", "Gedung", 
    "Lap No", "Waktu Lap (detik)", "Waktu Kumulatif (detik)", "Label Lap", "Timestamp Catat", "Observer"
  ];
  var lapSheet = ss.getSheetByName("Laps");
  if (!lapSheet) {
    lapSheet = ss.insertSheet("Laps");
  }
  migrateLapSheetRemoveElementColumn(lapSheet);
  ensureSheetHeaders(lapSheet, lapHeaders, "#F1F5F9");

  // 2. Sheet Khusus Hasil Kategori
  initWlaSheet(ss);
  initTimeStudySheet(ss);
  initQuickObsSheet(ss);

  // 3. Jalankan pemulihan data Laps
  perbaikiDanLengkapiSheetLaps();

  Logger.log("Berhasil! Sheet Master, Sessions, Laps, WLA, Time Study, dan Observasi Cepat telah siap & data telah diselaraskan.");
}

function appendNewMasterValues(masterSheet, sessions) {
  var lastRow = masterSheet.getLastRow();
  var readCols = Math.min(Math.max(masterSheet.getLastColumn(), 6), 7);
  var existing = lastRow > 1 ? masterSheet.getRange(2, 1, lastRow - 1, readCols).getValues() : [];
  var setOp = {}, setStudy = {}, setLine = {}, setMac = {}, setBld = {}, setSes = {}, setObs = {};

  for (var i = 0; i < existing.length; i++) {
    if (existing[i][0]) setOp[String(existing[i][0]).toLowerCase()] = true;
    if (existing[i][1]) setStudy[String(existing[i][1]).toLowerCase()] = true;
    if (existing[i][2]) setLine[String(existing[i][2]).toLowerCase()] = true;
    if (existing[i][3]) setMac[String(existing[i][3]).toLowerCase()] = true;
    if (existing[i][4]) setBld[String(existing[i][4]).toLowerCase()] = true;
    if (existing[i][5]) setSes[String(existing[i][5]).toLowerCase()] = true;
    if (existing[i].length > 6 && existing[i][6]) setObs[String(existing[i][6]).toLowerCase()] = true;
  }

  sessions.forEach(function(s) {
    var op = (s.operatorName || s.operator || "").trim();
    var study = (s.studyName || "").trim();
    var line = (s.line || "").trim();
    var mac = (s.machine || "").trim();
    var bld = (s.building || "").trim();
    var ses = (s.sessionName || s.shift || "").trim();
    var obs = (s.observerName || s.observer || "").trim();

    var isNew = false;
    var row = ["", "", "", "", "", "", ""];
    if (op && !setOp[op.toLowerCase()]) { row[0] = op; setOp[op.toLowerCase()] = true; isNew = true; }
    if (study && !setStudy[study.toLowerCase()]) { row[1] = study; setStudy[study.toLowerCase()] = true; isNew = true; }
    if (line && !setLine[line.toLowerCase()]) { row[2] = line; setLine[line.toLowerCase()] = true; isNew = true; }
    if (mac && !setMac[mac.toLowerCase()]) { row[3] = mac; setMac[mac.toLowerCase()] = true; isNew = true; }
    if (bld && !setBld[bld.toLowerCase()]) { row[4] = bld; setBld[bld.toLowerCase()] = true; isNew = true; }
    if (ses && !setSes[ses.toLowerCase()]) { row[5] = ses; setSes[ses.toLowerCase()] = true; isNew = true; }
    if (obs && !setObs[obs.toLowerCase()]) { row[6] = obs; setObs[obs.toLowerCase()] = true; isNew = true; }

    if (isNew) {
      masterSheet.appendRow(row);
    }
  });
}

function uniqueList(arr) {
  var seen = {};
  var out = [];
  for (var i = 0; i < arr.length; i++) {
    var val = arr[i];
    if (val && !seen[val]) {
      seen[val] = true;
      out.push(val);
    }
  }
  return out;
}

function formatDateYmd(val) {
  if (!val) return "";
  if (Object.prototype.toString.call(val) === "[object Date]" && !isNaN(val.getTime())) {
    return Utilities.formatDate(val, "Asia/Jakarta", "yyyy-MM-dd");
  }
  var s = String(val).trim();
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.substring(0, 10);
  var d = new Date(s);
  if (!isNaN(d.getTime())) {
    return Utilities.formatDate(d, "Asia/Jakarta", "yyyy-MM-dd");
  }
  return s;
}

function parseTimestampMillis(val, fallbackDateVal) {
  var target = val || fallbackDateVal;
  if (!target) return Date.now();
  if (Object.prototype.toString.call(target) === "[object Date]" && !isNaN(target.getTime())) {
    return target.getTime();
  }
  var s = String(target).trim();
  if (!s) return Date.now();
  var normalized = s.replace(" ", "T");
  var d = new Date(normalized);
  if (!isNaN(d.getTime())) return d.getTime();
  var d2 = new Date(s);
  if (!isNaN(d2.getTime())) return d2.getTime();
  return Date.now();
}

function deleteRowsByColumnMatch(sheet, colOneBased, idMap) {
  if (!sheet || sheet.getLastRow() <= 1) return 0;
  var lastRow = sheet.getLastRow();
  var vals = sheet.getRange(2, colOneBased, lastRow - 1, 1).getValues();
  var deleted = 0;
  for (var r = vals.length - 1; r >= 0; r--) {
    var val = String(vals[r][0] || "").trim();
    if (val && idMap[val]) {
      sheet.deleteRow(r + 2);
      deleted++;
    }
  }
  return deleted;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}"""

    const val SETUP_GUIDE = """PANDUAN UPDATE & DEPLOY APPS SCRIPT:

1. Buka spreadsheet Google Sheets Anda.
2. Klik menu: Ekstensi (Extensions) > Apps Script.
3. Hapus seluruh isi editor Kode.gs, lalu Tempel (PASTE) kode terbaru dari tombol "Salin Kode".
4. Klik tombol Simpan (ikon disket).
5. (SANGAT DIREKOMENDASIKAN - Validasi & Perbaikan Otomatis):
   - Pada pilihan dropdown fungsi di toolbar atas editor, pilih 'perbaikiDanLengkapiSheetLaps' atau 'inisialisasiSemuaSheet'.
   - Klik tombol 'Run' (Jalankan). Jika Google meminta izin otorisasi akun pertama kali, klik 'Review permissions' lalu izinkan.
    - Kolom Gedung, Mesin, Operator, Observer, dan Shift pada sheet Laps akan langsung otomatis terisi lengkap sesuai data di Sessions!
6. Klik tombol "Deploy" (Pojok kanan atas) > "Manage deployments" (Kelola penerapan).
7. Klik ikon pensil Edit > Pada pilihan Versi (Version), pilih "New version" (Versi Baru).
8. Pastikan Who has access: "Anyone" (Siapa saja).
9. Klik tombol "Deploy".
10. Salin URL Web App dan tempel ke menu Pengaturan di aplikasi.

STRUKTUR TABEL SPREADSHEET (LENGKAP & OTOMATIS):
• RAW DATA LENGKAP:
  - Sheet "Sessions" (16 kolom): Data induk semua sesi (TS, WLA, & Cepat) + Observer & Shift
  - Sheet "Laps" (16 kolom): Data detail putaran lengkap dengan Gedung, Mesin, Operator, Observer, Line, & Shift
  - Sheet "Master" (7 kolom): Operator, Time Study, Line, Mesin, Gedung, Shift, & Observer
• SHEET KHUSUS HASIL KATEGORI:
  - Sheet "WLA" (14 kolom): Khusus ringkasan hasil Workload Analysis multi-stopwatch
  - Sheet "Time Study" (13 kolom): Khusus hasil observasi Time Study standar
  - Sheet "Observasi Cepat" (10 kolom): Khusus hasil observasi cepat mandiri"""
}
