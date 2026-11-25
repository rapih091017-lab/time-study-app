package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
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
import com.example.data.model.SessionEntity
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.PlayfulPageHeader
import com.example.ui.components.PlayfulPrimaryButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseScreen(
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectNavTab(0)
    }

    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedModeFilter by remember { mutableStateOf("ALL") } // "ALL", "TS", "WLA"
    var selectedLineFilter by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    val distinctLines = remember(allSessions) {
        allSessions.map { it.line }.filter { it.isNotBlank() }.distinct()
    }

    val filteredSessions = allSessions.filter { s ->
        val matchesMode = when (selectedModeFilter) {
            "TS" -> s.mode == "TS"
            "WLA" -> s.mode == "WLA"
            else -> true
        }

        val matchesLine = selectedLineFilter.isBlank() || s.line.equals(selectedLineFilter, ignoreCase = true)

        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() ||
                s.studyName.contains(query, ignoreCase = true) ||
                s.observerName.contains(query, ignoreCase = true) ||
                s.operatorName.contains(query, ignoreCase = true) ||
                s.line.contains(query, ignoreCase = true) ||
                s.machine.contains(query, ignoreCase = true) ||
                s.building.contains(query, ignoreCase = true) ||
                s.sessionName.contains(query, ignoreCase = true) ||
                s.notes.contains(query, ignoreCase = true)

        matchesMode && matchesLine && matchesQuery
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingPillNavBar(
                selectedTab = 1,
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
                    title = "Database Observasi",
                    subtitle = "${allSessions.size} total sesi tersimpan",
                    rightActionIcon = Icons.Default.CloudDownload,
                    onRightActionClick = { viewModel.pullSessionsFromSheets() }
                )
            }

            // "Tarik dari Google Sheets" Banner Button
            item {
                Surface(
                    color = PlayfulTealLight,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.pullSessionsFromSheets() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PlayfulTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tarik dari Google Sheets",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = PlayfulTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Unduh data sesi dari perangkat lain",
                                    fontSize = 11.sp,
                                    color = PlayfulBlack.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PlayfulTeal)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("Unduh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // "Buka Database Spreadsheet" Quick Access Card
            item {
                Surface(
                    color = PlayfulWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFF16A34A).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.openDatabaseSpreadsheet(context) }
                        .testTag("open_spreadsheet_database_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Buka Google Sheets",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Buka Database Spreadsheet",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = PlayfulBlack
                                )
                                Text(
                                    text = "Lihat langsung data di Google Sheets",
                                    fontSize = 11.sp,
                                    color = PlayfulGray
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF16A34A))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
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
                        .testTag("database_search_input")
                )
            }

            // Filter Chips Row: Mode & Lines
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "Semua Mode",
                        isSelected = selectedModeFilter == "ALL",
                        onClick = { selectedModeFilter = "ALL" }
                    )
                    FilterPill(
                        label = "Time Study",
                        isSelected = selectedModeFilter == "TS",
                        onClick = { selectedModeFilter = "TS" }
                    )
                    FilterPill(
                        label = "WLA",
                        isSelected = selectedModeFilter == "WLA",
                        onClick = { selectedModeFilter = "WLA" }
                    )

                    distinctLines.forEach { line ->
                        FilterPill(
                            label = "Line $line",
                            isSelected = selectedLineFilter == line,
                            onClick = {
                                selectedLineFilter = if (selectedLineFilter == line) "" else line
                            }
                        )
                    }
                }
            }

            // Results count
            item {
                Text(
                    text = "HASIL PENCARIAN (${filteredSessions.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = PlayfulGray,
                    letterSpacing = 1.sp
                )
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
                                text = "Tidak Ada Data Ditemukan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba gunakan kata kunci lain atau tarik data dari Google Sheets",
                                fontSize = 12.sp,
                                color = PlayfulGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredSessions, key = { it.sessionId }) { session ->
                    DatabaseSessionCard(
                        session = session,
                        onClick = {
                            viewModel.navigateTo(Screen.StudyAnalysis(session.studyName))
                        },
                        onDelete = { sessionToDelete = session }
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
fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PlayfulBlack else PlayfulWhite)
            .border(1.dp, if (isSelected) PlayfulBlack else PlayfulBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) PlayfulWhite else PlayfulBlack
        )
    }
}

@Composable
fun DatabaseSessionCard(
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
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.date, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PlayfulGray, maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_database_session_${session.sessionId}")
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.studyName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = PlayfulBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Details chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (session.observerName.isNotBlank()) SmallDetailChip(label = "Obs: ${session.observerName}", icon = "👁️")
                if (session.operatorName.isNotBlank()) SmallDetailChip(label = session.operatorName, icon = "👤")
                if (session.line.isNotBlank()) SmallDetailChip(label = "Line ${session.line}", icon = "📍")
                if (session.machine.isNotBlank()) SmallDetailChip(label = session.machine, icon = "⚙️")
                if (session.building.isNotBlank()) SmallDetailChip(label = session.building, icon = "🏢")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlayfulBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.lapCount} Lap",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlayfulBlack
                )
                Text(
                    text = "Lihat Analisis & Grafik →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = PlayfulTeal
                )
            }
        }
    }
}
