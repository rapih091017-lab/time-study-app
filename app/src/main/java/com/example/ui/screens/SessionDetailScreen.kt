package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BigActionButton
import com.example.ui.components.BigOutlinedActionButton
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.LeaderboardFastestLaps
import com.example.ui.components.PlayfulLapItemRow
import com.example.ui.components.PlayfulPrimaryButton
import com.example.ui.components.TimeFormatter
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulSkyBlueLight
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.Screen
import com.example.viewmodel.TimeStudyViewModel
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()

    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val fastestLaps = remember(laps) {
        laps.sortedBy { it.lapDurationMillis }.take(3)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ringkasan Sesi",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = PlayfulBlack
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PlayfulWhite)
                                .border(1.dp, PlayfulBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = PlayfulBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteSessionDialog = true },
                        modifier = Modifier.testTag("delete_session_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PlayfulWhite)
                                .border(1.dp, PlayfulBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Sesi",
                                tint = PlayfulCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlayfulBackground)
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
            val session = activeSession
            if (session == null) {
                item {
                    Text("Memuat data sesi...", color = PlayfulGray)
                }
            } else {
                // Header Info Card
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (session.mode == "WLA") PlayfulSkyBlueLight else PlayfulCream)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(session.mode, fontWeight = FontWeight.Black, fontSize = 11.sp, color = PlayfulBlack, maxLines = 1)
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
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = PlayfulBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (session.syncStatus == "SYNCED") PlayfulTealLight else PlayfulCream)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (session.syncStatus == "SYNCED") "✓ Tersinkron" else "Belum sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (session.syncStatus == "SYNCED") PlayfulTeal else PlayfulCoral,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = session.studyName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulBlack
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MetaRow(label = "Tanggal", value = session.date)
                            MetaRow(label = "Shift", value = session.sessionName.ifBlank { "Shift 1" })
                            if (session.observerName.isNotBlank()) MetaRow(label = "Observer", value = session.observerName)
                            if (session.operatorName.isNotBlank()) MetaRow(label = "Operator", value = session.operatorName)
                            if (session.line.isNotBlank()) MetaRow(label = "Line", value = session.line)
                            if (session.machine.isNotBlank()) MetaRow(label = "Mesin", value = session.machine)
                            if (session.building.isNotBlank()) MetaRow(label = "Gedung", value = session.building)
                            if (session.notes.isNotBlank()) MetaRow(label = "Catatan", value = session.notes)

                            Spacer(modifier = Modifier.height(14.dp))

                            // Large Stats Numbers
                            val sessionStats = remember(laps) { viewModel.calculateStats(laps) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(PlayfulBackground)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TOTAL LAP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                                        Text("${laps.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = PlayfulBlack)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TOTAL WAKTU", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                                        Text(
                                            TimeFormatter.formatDurationShort(session.totalDurationMillis),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PlayfulTeal
                                        )
                                    }
                                }

                                if (session.mode != "WLA" && laps.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("RATA-RATA / LAP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                                            Text(
                                                TimeFormatter.formatMillis(sessionStats.avgDurationMillis),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PlayfulBlack
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("UNIT/MIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                                            Text(
                                                String.format(Locale.US, "%.1f", sessionStats.unitsPerMinute),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PlayfulCoral
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Leaderboard
                if (fastestLaps.isNotEmpty()) {
                    item {
                        LeaderboardFastestLaps(fastestLaps = fastestLaps)
                    }
                }

                // Export & Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BigActionButton(
                                text = "Excel (.xlsx)",
                                icon = Icons.Default.FileDownload,
                                onClick = { viewModel.exportSessionData(session.sessionId, isExcel = true) },
                                backgroundColor = Color(0xFF16A34A),
                                minHeight = 48.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                testTag = "export_excel_session"
                            )
                            BigActionButton(
                                text = "CSV (.csv)",
                                icon = Icons.Default.Share,
                                onClick = { viewModel.exportSessionData(session.sessionId, isExcel = false) },
                                backgroundColor = PlayfulBlack,
                                minHeight = 48.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                testTag = "export_csv_session"
                            )
                        }

                        // Salin ke Clipboard (TSV) Fallback Button
                        BigOutlinedActionButton(
                            text = "Salin Data ke Clipboard (TSV)",
                            icon = Icons.Default.ContentCopy,
                            onClick = { viewModel.copySessionTsvToClipboard(session.sessionId) },
                            minHeight = 46.dp,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Continue session button
                        BigOutlinedActionButton(
                            text = "Lanjutkan Sesi Ini",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                if (session.mode == "WLA") {
                                    viewModel.navigateTo(Screen.WlaStudy(session.sessionId))
                                } else {
                                    viewModel.navigateTo(Screen.SingleStudy(session.sessionId))
                                }
                            },
                            minHeight = 46.dp,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "resume_session_button"
                        )
                    }
                }

                // Laps Detail Table
                item {
                    Text(
                        text = "DAFTAR LAP TERCATAT (${laps.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulGray,
                        letterSpacing = 1.sp
                    )
                }

                if (laps.isEmpty()) {
                    item {
                        Text("Tidak ada lap pada sesi ini.", fontSize = 13.sp, color = PlayfulGray)
                    }
                } else {
                    items(laps, key = { it.lapId }) { lap ->
                        PlayfulLapItemRow(
                            lap = lap,
                            onEditLabel = { viewModel.updateLapLabel(lap.lapId, lap.label) },
                            onDelete = { viewModel.deleteLap(lap) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDeleteSessionDialog && activeSession != null) {
        val s = activeSession!!
        DeleteSessionDialog(
            session = s,
            onDeleteLocalOnly = {
                viewModel.deleteSession(s.sessionId, deleteFromSheets = false, navigateBackOnComplete = true)
                showDeleteSessionDialog = false
            },
            onDeleteEverywhere = {
                viewModel.deleteSession(s.sessionId, deleteFromSheets = true, navigateBackOnComplete = true)
                showDeleteSessionDialog = false
            },
            onDismiss = { showDeleteSessionDialog = false }
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(text = "$label: ", fontSize = 13.sp, color = PlayfulGray, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 13.sp,
            color = PlayfulBlack,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}
