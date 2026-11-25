package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionEntity
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.Geometric3dOrnament
import com.example.ui.components.HorizontalDateStrip
import com.example.ui.components.PlayfulPageHeader
import com.example.ui.components.TimeFormatter
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavender
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulOrange
import com.example.ui.theme.PlayfulSkyBlue
import com.example.ui.theme.PlayfulSkyBlueLight
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.Screen
import com.example.viewmodel.TimeStudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSessionsCount by viewModel.pendingSessionsCount.collectAsStateWithLifecycle()
    val pendingLapsCount by viewModel.pendingLapsCount.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val datesWithData by viewModel.datesWithData.collectAsStateWithLifecycle()
    val homeDateFilter by viewModel.homeDateFilter.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val elements by viewModel.elements.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val liveTimes by viewModel.liveElapsedTimes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val totalPending = pendingSessionsCount + pendingLapsCount

    val filteredSessions = if (homeDateFilter.isBlank()) {
        allSessions.take(6)
    } else {
        allSessions.filter { it.date == homeDateFilter }
    }

    val todayFormatted = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).format(Date())
    val operatorGreeting = allSessions.firstOrNull()?.operatorName?.ifBlank { "Engineer" } ?: "Engineer"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingPillNavBar(
                selectedTab = 0,
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
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Avatar + Halo, [Operator] + Tanggal
            item {
                PlayfulPageHeader(
                    title = "Halo, $operatorGreeting",
                    subtitle = todayFormatted,
                    avatarName = operatorGreeting,
                    rightActionIcon = Icons.Default.Settings,
                    onRightActionClick = { viewModel.selectNavTab(3) }
                )
            }

            // Sync Status Chip + Quick Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (totalPending > 0) PlayfulCream else PlayfulTealLight,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectNavTab(3) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) PlayfulTeal else PlayfulGray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!isOnline) {
                                    "Offline ($totalPending)"
                                } else if (totalPending > 0) {
                                    "$totalPending blm sync"
                                } else {
                                    "Tersinkron ✓"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlayfulBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOnline && totalPending > 0) {
                            Surface(
                                color = PlayfulBlack,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.syncData(silent = false) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFF16A34A),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.openDatabaseSpreadsheet(context) }
                                .testTag("home_open_spreadsheet_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Sheets", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Buka Database Spreadsheet", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }

                        Surface(
                            color = PlayfulWhite,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, PlayfulBorder),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.openOnboardingGuide() }
                                .testTag("home_guide_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Panduan Aplikasi",
                                    tint = PlayfulBlack,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active In-Progress Session Banner (Melanjutkan Sesi yang Belum Selesai)
            if (activeSession != null && activeSession?.status == "ACTIVE") {
                val s = activeSession!!
                val isWla = s.mode == "WLA"
                val activeElement = elements.firstOrNull()
                val activeMillis = if (isWla) {
                    elements.maxOfOrNull { liveTimes[it.elementId] ?: it.elapsedMillis } ?: 0L
                } else {
                    activeElement?.let { liveTimes[it.elementId] ?: it.elapsedMillis } ?: 0L
                }
                val anyRunning = elements.any { it.isRunning }

                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulTealLight),
                        border = BorderStroke(1.5.dp, PlayfulTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag("home_active_session_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (anyRunning) PlayfulCoral else PlayfulOrange)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (anyRunning) "SEDANG BERJALAN ⏱️" else "SESI AKTIF (DIJEDA) ⏸️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PlayfulTeal,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (isWla) PlayfulSkyBlue else PlayfulMustard,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isWla) "WLA" else (if (s.studyName.startsWith("Observasi Cepat")) "Cepat" else "TS"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PlayfulBlack,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { sessionToDelete = s },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_active_session_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus sesi aktif",
                                            tint = PlayfulCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.studyName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PlayfulBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${s.sessionName} • ${laps.size} lap tercatat",
                                        fontSize = 12.sp,
                                        color = PlayfulGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TimeFormatter.formatMillis(activeMillis),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulBlack
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.loadSession(s.sessionId)
                                        if (isWla) {
                                            viewModel.navigateTo(Screen.WlaStudy(s.sessionId))
                                        } else {
                                            viewModel.navigateTo(Screen.SingleStudy(s.sessionId))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lanjutkan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.finishSessionToHome() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Selesai",
                                        fontSize = 12.sp,
                                        color = PlayfulBlack,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Date Strip (Min - Sab) with dot markers
            item {
                HorizontalDateStrip(
                    selectedDate = homeDateFilter,
                    datesWithData = datesWithData,
                    onDateSelected = { viewModel.setHomeDateFilter(it) }
                )
            }

            // Hero Card: Observasi Cepat / Time Study Cepat (Langsung Pakai Seperti Stopwatch Biasa Tanpa Form)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulLavender),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { viewModel.handleQuickObservationClick() }
                            .testTag("home_quick_observation_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 172.dp)
                        ) {
                            // 3D-ish geometric ornament on right side
                            Geometric3dOrnament(
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 6.dp),
                                accentColor = PlayfulCoral
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 88.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = PlayfulCoral,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = PlayfulWhite,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "TIME STUDY CEPAT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PlayfulWhite
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Observasi Cepat",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulBlack,
                                    lineHeight = 26.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Buka stopwatch seketika tanpa isi formulir. Langsung jalan seperti stopwatch biasa!",
                                    fontSize = 11.sp,
                                    color = PlayfulBlack.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = PlayfulBlack,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Mulai Langsung ⚡",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PlayfulWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Two Big Color Blocks Side-by-Side: Time Study (Mustard) & WLA (Sky Blue) dengan Form Setup
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Card: Time Study (Mustard)
                    Card(
                        onClick = { viewModel.openSetup("TS") },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulMustard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 168.dp)
                            .testTag("home_ts_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PlayfulBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = PlayfulWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PlayfulBlack.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Setup Form",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlayfulBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column {
                                Text(
                                    text = "Time Study",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Single stopwatch dengan data operator & mesin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlayfulBlack.copy(alpha = 0.75f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Right Card: Time Study WLA (Sky Blue)
                    Card(
                        onClick = { viewModel.openSetup("WLA") },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulSkyBlue),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 168.dp)
                            .testTag("home_wla_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PlayfulBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = PlayfulWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PlayfulBlack.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Multi-Elemen",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlayfulBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column {
                                Text(
                                    text = "Time Study WLA",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Multi stopwatch untuk analisa beban kerja",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlayfulBlack.copy(alpha = 0.75f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: Sesi Terakhir
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (homeDateFilter.isBlank()) "SESI TERAKHIR" else "SESI PADA TANGGAL INI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Semua (${allSessions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayfulBlack,
                        modifier = Modifier
                            .clickable { viewModel.selectNavTab(2) }
                            .padding(4.dp)
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Sesi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tekan kartu Time Study atau WLA di atas untuk memulai pengamatan",
                                fontSize = 13.sp,
                                color = PlayfulGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredSessions, key = { it.sessionId }) { session ->
                    PlayfulSessionCard(
                        session = session,
                        onClick = {
                            viewModel.loadSession(session.sessionId)
                            if (session.status == "ACTIVE") {
                                if (session.mode == "WLA") {
                                    viewModel.navigateTo(Screen.WlaStudy(session.sessionId))
                                } else {
                                    viewModel.navigateTo(Screen.SingleStudy(session.sessionId))
                                }
                            } else {
                                viewModel.navigateTo(Screen.SessionDetail(session.sessionId))
                            }
                        },
                        onDelete = { sessionToDelete = session },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
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

@Composable
fun PlayfulSessionCard(
    session: SessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_card_${session.sessionId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Mode Tag Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (session.mode == "WLA") PlayfulSkyBlueLight else PlayfulCream)
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

                    // Session Pill
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

                // Sync status chip + delete button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (session.syncStatus == "SYNCED") PlayfulTealLight else PlayfulCream
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (session.syncStatus == "SYNCED") "✓ Tersinkron" else "Belum sync",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.syncStatus == "SYNCED") PlayfulTeal else PlayfulOrange,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_session_${session.sessionId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus sesi",
                            tint = PlayfulCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = session.studyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = PlayfulBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips row: Observer, Operator, Line, Mesin, Gedung
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (session.observerName.isNotBlank()) {
                    SmallDetailChip(label = "Obs: ${session.observerName}", icon = "👁️")
                }
                if (session.operatorName.isNotBlank()) {
                    SmallDetailChip(label = session.operatorName, icon = "👤")
                }
                if (session.line.isNotBlank()) {
                    SmallDetailChip(label = "Line ${session.line}", icon = "📍")
                }
                if (session.machine.isNotBlank()) {
                    SmallDetailChip(label = session.machine, icon = "⚙️")
                }
                if (session.building.isNotBlank()) {
                    SmallDetailChip(label = session.building, icon = "🏢")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration & Laps count summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlayfulBackground)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.lapCount} Lap tercatat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlayfulBlack
                )
                Text(
                    text = if (session.totalDurationMillis > 0) TimeFormatter.formatDurationShort(session.totalDurationMillis) else "Aktif",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = PlayfulTeal
                )
            }
        }
    }
}

@Composable
fun SmallDetailChip(label: String, icon: String) {
    Surface(
        color = PlayfulBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = icon, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PlayfulBlack
            )
        }
    }
}
