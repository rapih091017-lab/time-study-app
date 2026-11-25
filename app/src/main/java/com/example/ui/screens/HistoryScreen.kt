package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionEntity
import com.example.ui.components.BigActionButton
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.HorizontalDateStrip
import com.example.ui.components.PlayfulPageHeader
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulSkyBlueLight
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.HistorySortOrder
import com.example.viewmodel.Screen
import com.example.viewmodel.TimeStudyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectNavTab(0)
    }

    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val datesWithData by viewModel.datesWithData.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val historyYear by viewModel.historyYear.collectAsStateWithLifecycle()
    val historyMonth by viewModel.historyMonth.collectAsStateWithLifecycle()
    val historyFilterByMonth by viewModel.historyFilterByMonth.collectAsStateWithLifecycle()
    val historyDateFilter by viewModel.historyDateFilter.collectAsStateWithLifecycle()
    val historySortOrder by viewModel.historySortOrder.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    val monthYearLabel = remember(historyYear, historyMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, historyYear)
        cal.set(Calendar.MONTH, historyMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }

    val monthPrefix = remember(historyYear, historyMonth) {
        String.format(Locale.US, "%04d-%02d-", historyYear, historyMonth + 1)
    }

    val filteredSessions = remember(
        allSessions,
        searchQuery,
        historyDateFilter,
        historyFilterByMonth,
        monthPrefix,
        historySortOrder
    ) {
        val filtered = allSessions.filter { session ->
            val matchesSearch = searchQuery.isBlank() ||
                    session.studyName.contains(searchQuery, ignoreCase = true) ||
                    session.observerName.contains(searchQuery, ignoreCase = true) ||
                    session.operatorName.contains(searchQuery, ignoreCase = true) ||
                    session.line.contains(searchQuery, ignoreCase = true) ||
                    session.machine.contains(searchQuery, ignoreCase = true) ||
                    session.sessionName.contains(searchQuery, ignoreCase = true) ||
                    session.date.contains(searchQuery, ignoreCase = true)

            val matchesTime = when {
                historyDateFilter.isNotBlank() -> session.date == historyDateFilter
                historyFilterByMonth -> session.date.startsWith(monthPrefix)
                else -> true
            }

            matchesSearch && matchesTime
        }

        when (historySortOrder) {
            HistorySortOrder.NEWEST -> filtered.sortedWith(
                compareByDescending<SessionEntity> { it.date }.thenByDescending { it.createdAt }
            )
            HistorySortOrder.OLDEST -> filtered.sortedWith(
                compareBy<SessionEntity> { it.date }.thenBy { it.createdAt }
            )
            HistorySortOrder.MOST_LAPS -> filtered.sortedByDescending { it.lapCount }
            HistorySortOrder.NAME_AZ -> filtered.sortedBy { it.studyName.lowercase(Locale.getDefault()) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingPillNavBar(
                selectedTab = 2,
                onTabSelected = { tab -> viewModel.selectNavTab(tab) }
            )
        },
        containerColor = PlayfulBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                PlayfulPageHeader(
                    title = "Riwayat Observasi",
                    subtitle = "${allSessions.size} sesi tercatat di perangkat",
                    rightActionIcon = Icons.Default.Sync,
                    onRightActionClick = { viewModel.syncData(silent = false) }
                )
            }

            // Cross-device Sync Banner
            item {
                Surface(
                    color = PlayfulTealLight,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = !syncing) { viewModel.syncData(silent = false) }
                        .testTag("history_sync_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Sinkronkan dari Google Sheets",
                                tint = PlayfulTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (syncing) "Menyinkronkan dengan Sheets..." else "Sinkronkan Riwayat Antar Perangkat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlayfulBlack
                                )
                                Text(
                                    text = if (isOnline) "Ketuk untuk upload & tarik sesi terbaru dari Spreadsheet" else "Perangkat offline • sambungkan internet untuk sync",
                                    fontSize = 11.sp,
                                    color = PlayfulGray
                                )
                            }
                        }
                    }
                }
            }

            // Month Navigator & Calendar Picker Card (supports browsing previous months)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.stepHistoryMonth(-1) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("history_prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Bulan Sebelumnya",
                                    tint = PlayfulBlack
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = monthYearLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        historyDateFilter.isNotBlank() -> "Tanggal dipilih: $historyDateFilter"
                                        historyFilterByMonth -> "Menampilkan bulan ini"
                                        else -> "Menampilkan semua bulan"
                                    },
                                    fontSize = 11.sp,
                                    color = PlayfulGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { viewModel.stepHistoryMonth(1) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("history_next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Bulan Berikutnya",
                                    tint = PlayfulBlack
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllMonths = !historyFilterByMonth && historyDateFilter.isBlank()
                            Surface(
                                color = if (isAllMonths) PlayfulBlack else PlayfulBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isAllMonths) PlayfulBlack else PlayfulBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.setHistoryDateFilter("")
                                        viewModel.setHistoryFilterByMonth(false)
                                    }
                                    .testTag("history_all_months_chip")
                            ) {
                                Text(
                                    text = "Semua Bulan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllMonths) PlayfulWhite else PlayfulBlack,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            val isThisMonthFilter = historyFilterByMonth && historyDateFilter.isBlank()
                            Surface(
                                color = if (isThisMonthFilter) PlayfulBlack else PlayfulBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isThisMonthFilter) PlayfulBlack else PlayfulBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.setHistoryDateFilter("")
                                        viewModel.setHistoryFilterByMonth(true)
                                    }
                                    .testTag("history_month_only_chip")
                            ) {
                                Text(
                                    text = "Filter Bulan Ini",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isThisMonthFilter) PlayfulWhite else PlayfulBlack,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                color = if (historyDateFilter.isNotBlank()) PlayfulTeal else PlayfulTealLight,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showDatePicker = true }
                                    .testTag("history_calendar_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pilih Tanggal dari Kalender",
                                        tint = if (historyDateFilter.isNotBlank()) PlayfulWhite else PlayfulTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (historyDateFilter.isNotBlank()) historyDateFilter else "Kalender",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (historyDateFilter.isNotBlank()) PlayfulWhite else PlayfulTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Date Strip for the selected month
            item {
                HorizontalDateStrip(
                    selectedDate = historyDateFilter,
                    datesWithData = datesWithData,
                    onDateSelected = { viewModel.setHistoryDateFilter(it) },
                    displayYear = historyYear,
                    displayMonth = historyMonth
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari aktivitas, observer, operator, mesin...", fontSize = 13.sp, color = PlayfulGray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PlayfulBlack, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PlayfulGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlayfulBlack,
                        unfocusedBorderColor = PlayfulBorder,
                        focusedContainerColor = PlayfulWhite,
                        unfocusedContainerColor = PlayfulWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            // Sort Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Urutkan",
                        tint = PlayfulGray,
                        modifier = Modifier.size(18.dp)
                    )
                    HistorySortOrder.entries.forEach { order ->
                        val selected = historySortOrder == order
                        Surface(
                            color = if (selected) PlayfulBlack else PlayfulWhite,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (selected) PlayfulBlack else PlayfulBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setHistorySortOrder(order) }
                                .testTag("history_sort_${order.name.lowercase()}")
                        ) {
                            Text(
                                text = order.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) PlayfulWhite else PlayfulBlack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Export Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BigActionButton(
                        text = "Excel (.xlsx)",
                        icon = Icons.Default.FileDownload,
                        onClick = { viewModel.exportAllData(isExcel = true) },
                        backgroundColor = Color(0xFF16A34A),
                        minHeight = 48.dp,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        testTag = "export_all_excel"
                    )
                    BigActionButton(
                        text = "CSV (.csv)",
                        icon = Icons.Default.Share,
                        onClick = { viewModel.exportAllData(isExcel = false) },
                        backgroundColor = PlayfulBlack,
                        minHeight = 48.dp,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        testTag = "export_all_csv"
                    )
                }
            }

            // Section Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR SESI (${filteredSessions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = historySortOrder.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayfulTeal
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tidak Ada Sesi Ditemukan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PlayfulBlack
                            )
                            if (searchQuery.isNotEmpty() || historyDateFilter.isNotEmpty() || historyFilterByMonth) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Coba pilih 'Semua Bulan' atau cari kata kunci lain",
                                    fontSize = 12.sp,
                                    color = PlayfulGray
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredSessions, key = { it.sessionId }) { session ->
                    Card(
                        onClick = {
                            viewModel.loadSession(session.sessionId)
                            viewModel.navigateTo(Screen.SessionDetail(session.sessionId))
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${session.sessionId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (session.mode == "WLA") PlayfulSkyBlueLight else PlayfulCream
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = session.mode,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PlayfulBlack,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PlayfulLavenderLight)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = session.sessionName.ifBlank { "Shift 1" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PlayfulBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.date,
                                        fontSize = 11.sp,
                                        color = PlayfulGray,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = session.studyName,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = PlayfulBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = buildString {
                                        if (session.observerName.isNotBlank()) append("Obs: ${session.observerName} • ")
                                        if (session.operatorName.isNotBlank()) append("${session.operatorName} • ")
                                        if (session.line.isNotBlank()) append("Line ${session.line} • ")
                                        append("${session.lapCount} Lap")
                                    },
                                    fontSize = 12.sp,
                                    color = PlayfulGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Delete button
                            IconButton(
                                onClick = { sessionToDelete = session },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("delete_history_session_${session.sessionId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus sesi",
                                    tint = PlayfulCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = selectedMillis
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH)
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                            viewModel.setHistoryMonth(year, month)
                            viewModel.setHistoryDateFilter(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih", fontWeight = FontWeight.Bold, color = PlayfulBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal", color = PlayfulGray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        DeleteSessionDialog(
            session = s,
            onDeleteLocalOnly = {
                viewModel.deleteSession(s.sessionId, deleteFromSheets = false, navigateBackOnComplete = false)
                sessionToDelete = null
            },
            onDeleteEverywhere = {
                viewModel.deleteSession(s.sessionId, deleteFromSheets = true, navigateBackOnComplete = false)
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }
}
