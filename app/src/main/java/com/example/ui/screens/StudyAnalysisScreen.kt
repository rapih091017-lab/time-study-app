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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.example.data.model.LapEntity
import com.example.data.model.SessionEntity
import com.example.ui.components.BarChartItem
import com.example.ui.components.BigActionButton
import com.example.ui.components.BigOutlinedActionButton
import com.example.ui.components.LeaderboardFastestLaps
import com.example.ui.components.PlayfulBarChart
import com.example.ui.components.PlayfulLapItemRow
import com.example.ui.components.TimeFormatter
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavender
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulOrange
import com.example.ui.theme.PlayfulSkyBlue
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.TimeStudyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAnalysisScreen(
    studyName: String,
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    var sessionsForStudy by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    var lapsForStudy by remember { mutableStateOf<List<LapEntity>>(emptyList()) }

    LaunchedEffect(studyName) {
        sessionsForStudy = viewModel.repository.getSessionsForStudyName(studyName)
        lapsForStudy = viewModel.repository.getLapsForStudyName(studyName)
    }

    val overallStats = remember(lapsForStudy) {
        viewModel.calculateStats(lapsForStudy)
    }

    val fastestLaps = remember(lapsForStudy) {
        lapsForStudy.sortedBy { it.lapDurationMillis }.take(3)
    }

    // Prepare chart bars comparing average lap duration across sessions
    val chartItems = remember(sessionsForStudy, lapsForStudy) {
        val colors = listOf(PlayfulTeal, PlayfulMustard, PlayfulCoral, PlayfulSkyBlue, PlayfulLavender, PlayfulOrange)
        sessionsForStudy.mapIndexed { idx, s ->
            val sLaps = lapsForStudy.filter { it.sessionId == s.sessionId }
            val avgSec = if (sLaps.isNotEmpty()) sLaps.sumOf { it.lapDurationMillis } / (sLaps.size * 1000.0) else 0.0
            BarChartItem(
                label = s.sessionName.ifBlank { "Shift ${idx + 1}" },
                valueSeconds = avgSec,
                color = colors[idx % colors.size]
            )
        }.filter { it.valueSeconds > 0.0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = studyName,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = PlayfulBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Analisis Agregat ${sessionsForStudy.size} Sesi",
                            fontSize = 11.sp,
                            color = PlayfulGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("analysis_back_button")
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
            // Big Summary Card
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
                        Text(
                            text = "STATISTIK KINERJA AKTIVITAS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = PlayfulGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(title = "TOTAL SESI", value = "${sessionsForStudy.size}", color = PlayfulBlack)
                            StatBox(title = "TOTAL LAP", value = "${overallStats.count}", color = PlayfulBlack)
                            StatBox(title = "UNIT/MIN", value = String.format(Locale.US, "%.1f", overallStats.unitsPerMinute), color = PlayfulCoral)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(title = "RATA-RATA", value = TimeFormatter.formatMillis(overallStats.avgDurationMillis), color = PlayfulTeal)
                            StatBox(title = "TERCEPAT", value = TimeFormatter.formatMillis(overallStats.minDurationMillis), color = PlayfulTeal)
                            StatBox(title = "TERLAMBAT", value = TimeFormatter.formatMillis(overallStats.maxDurationMillis), color = PlayfulCoral)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PlayfulBackground)
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STANDAR DEVIASI: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulGray
                            )
                            Text(
                                text = String.format(Locale.US, "%.2fs", overallStats.stdDevMillis / 1000.0),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulMustard
                            )
                        }
                    }
                }
            }

            // Bar Chart Comparison Across Sessions
            if (chartItems.isNotEmpty()) {
                item {
                    PlayfulBarChart(items = chartItems)
                }
            }

            // Top 3 Leaderboard
            if (fastestLaps.isNotEmpty()) {
                item {
                    LeaderboardFastestLaps(fastestLaps = fastestLaps)
                }
            }

            // Export Actions for this Study
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val firstSession = sessionsForStudy.firstOrNull()
                    if (firstSession != null) {
                        BigActionButton(
                            text = "Excel (.xlsx)",
                            icon = Icons.Default.FileDownload,
                            onClick = { viewModel.exportSessionData(firstSession.sessionId, isExcel = true) },
                            backgroundColor = Color(0xFF16A34A),
                            minHeight = 48.dp,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        BigActionButton(
                            text = "CSV (.csv)",
                            icon = Icons.Default.Share,
                            onClick = { viewModel.exportSessionData(firstSession.sessionId, isExcel = false) },
                            backgroundColor = PlayfulBlack,
                            minHeight = 48.dp,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // All Laps Table
            item {
                Text(
                    text = "SELURUH LAP TERCATAT (${lapsForStudy.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = PlayfulGray,
                    letterSpacing = 1.sp
                )
            }

            if (lapsForStudy.isEmpty()) {
                item {
                    Text("Belum ada data lap pada aktivitas ini.", fontSize = 13.sp, color = PlayfulGray)
                }
            } else {
                items(lapsForStudy.reversed(), key = { it.lapId }) { lap ->
                    PlayfulLapItemRow(
                        lap = lap,
                        isFastest = lap.lapDurationMillis == overallStats.minDurationMillis && overallStats.count > 1,
                        isSlowest = lap.lapDurationMillis == overallStats.maxDurationMillis && overallStats.count > 1,
                        onEditLabel = { viewModel.updateLapLabel(lap.lapId, lap.label) },
                        onDelete = { viewModel.deleteLap(lap) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
