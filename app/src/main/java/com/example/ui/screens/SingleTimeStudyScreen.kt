package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.example.data.model.LapEntity
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.CurvedStopwatchDial
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.HoldToConfirmButton
import com.example.ui.components.LeaderboardFastestLaps
import com.example.ui.components.MasterComboboxField
import com.example.ui.components.PlayfulLapItemRow
import com.example.ui.components.RoundControlButton
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
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import java.util.Locale
import com.example.viewmodel.TimeStudyViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleTimeStudyScreen(
    sessionId: String,
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val elements by viewModel.elements.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val liveTimes by viewModel.liveElapsedTimes.collectAsStateWithLifecycle()
    val masterOptions by viewModel.masterOptions.collectAsStateWithLifecycle()

    val currentElement = elements.firstOrNull()
    val currentMillis = currentElement?.let { liveTimes[it.elementId] ?: it.elapsedMillis } ?: 0L
    val isRunning = currentElement?.isRunning == true
    val isPaused = !isRunning && currentMillis > 0

    val stats = viewModel.calculateStats(laps)
    val fastestLaps = remember(laps) { laps.sortedBy { it.lapDurationMillis }.take(3) }

    val snackbarHostState = remember { SnackbarHostState() }

    val isQuickDraft = activeSession?.status == "DRAFT"
    val hasObservationData = isRunning || currentMillis > 0L || laps.isNotEmpty()
    val hasEditedMeta = activeSession != null && (
        activeSession!!.operatorName.isNotBlank() ||
        activeSession!!.observerName.isNotBlank() ||
        activeSession!!.line.isNotBlank() ||
        activeSession!!.machine.isNotBlank() ||
        activeSession!!.building.isNotBlank()
    )

    // Dialog states
    var lapToDelete by remember { mutableStateOf<LapEntity?>(null) }
    var lapToEditLabel by remember { mutableStateOf<LapEntity?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showEditSessionDialog by remember { mutableStateOf(false) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteCurrentSessionDialog by remember { mutableStateOf(false) }
    var showQuickSaveDialog by remember { mutableStateOf(false) }
    var showQuickDiscardConfirmDialog by remember { mutableStateOf(false) }
    var quickSaveStudyName by remember(activeSession?.sessionId, activeSession?.studyName) {
        mutableStateOf(activeSession?.studyName ?: "Observasi Cepat")
    }

    val onRequestExit: () -> Unit = {
        if (isQuickDraft) {
            if (hasObservationData || hasEditedMeta) {
                showQuickSaveDialog = true
            } else {
                viewModel.discardQuickObservationAndExit(silent = true)
            }
        } else {
            if (hasObservationData) {
                showExitConfirmationDialog = true
            } else {
                viewModel.navigateBack()
            }
        }
    }

    BackHandler {
        onRequestExit()
    }

    LaunchedEffect(Unit) {
        viewModel.undoMessageEvent.collectLatest { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteLap()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable { showEditSessionDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeSession?.studyName ?: "Time Study",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = PlayfulBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = PlayfulLavenderLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = activeSession?.sessionName ?: "Shift 1",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlayfulBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        val meta = buildString {
                            if (!activeSession?.observerName.isNullOrBlank()) append("Obs: ${activeSession?.observerName} • ")
                            if (!activeSession?.operatorName.isNullOrBlank()) append("${activeSession?.operatorName} • ")
                            if (!activeSession?.line.isNullOrBlank()) append("Line ${activeSession?.line} • ")
                            if (!activeSession?.machine.isNullOrBlank()) append("${activeSession?.machine} • ")
                            if (!activeSession?.building.isNullOrBlank()) append(activeSession?.building)
                        }
                        Text(
                            text = if (meta.isNotBlank()) meta else "Ketuk di sini untuk edit info sesi ✍️",
                            fontSize = 11.sp,
                            color = if (meta.isNotBlank()) PlayfulGray else PlayfulCoral,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onRequestExit,
                        modifier = Modifier.testTag("single_study_back_button")
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
                        onClick = { showEditSessionDialog = true },
                        modifier = Modifier.testTag("single_study_edit_info_button")
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit data sesi",
                                tint = PlayfulBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlayfulBackground)
            )
        },
        bottomBar = {
            // Control Action Bar (Playful round buttons + LAP + Selesai)
            Surface(
                color = PlayfulWhite,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Row of round action buttons: RESET, START/PAUSE/RESUME, and BIG LAP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. RESET Button (Round) - Triggers Session Chaining
                        RoundControlButton(
                            icon = Icons.Default.Refresh,
                            label = "Reset",
                            onClick = { showResetDialog = true },
                            bgColor = PlayfulBackground,
                            contentColor = PlayfulBlack,
                            size = 56.dp,
                            testTag = "round_reset_button"
                        )

                        // 2. Distinct START / PAUSE / RESUME Button (Round)
                        when {
                            !isRunning && currentMillis == 0L -> {
                                RoundControlButton(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Mulai",
                                    onClick = { viewModel.startSingleStopwatch() },
                                    bgColor = PlayfulTeal,
                                    contentColor = Color.White,
                                    size = 64.dp,
                                    testTag = "round_start_button"
                                )
                            }
                            isRunning -> {
                                RoundControlButton(
                                    icon = Icons.Default.Pause,
                                    label = "Pause",
                                    onClick = { viewModel.pauseSingleStopwatch() },
                                    bgColor = PlayfulOrange,
                                    contentColor = Color.White,
                                    size = 64.dp,
                                    testTag = "round_pause_button"
                                )
                            }
                            else -> {
                                // Paused
                                RoundControlButton(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Resume",
                                    onClick = { viewModel.resumeSingleStopwatch() },
                                    bgColor = PlayfulTeal,
                                    contentColor = Color.White,
                                    size = 64.dp,
                                    testTag = "round_resume_button"
                                )
                            }
                        }

                        // 3. BIG LAP BUTTON (Largest, most prominent, thumb-friendly)
                        RoundControlButton(
                            icon = Icons.Default.Flag,
                            label = "Lap #${laps.size + 1}",
                            onClick = { viewModel.recordSingleLap() },
                            bgColor = PlayfulMustard,
                            contentColor = PlayfulBlack,
                            size = 76.dp,
                            testTag = "round_lap_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SELESAI SESI: Hold 1 second -> Saves & returns to Home automatically
                    HoldToConfirmButton(
                        text = "SELESAI SESI",
                        icon = Icons.Default.Check,
                        holdDurationMs = 1000L,
                        onConfirmed = { viewModel.finishSessionToHome() },
                        buttonColor = PlayfulBlack,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "finish_session_button"
                    )
                }
            }
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
            // 1. Giant Curved Stopwatch Dial (Teal Header)
            item {
                CurvedStopwatchDial(
                    millis = currentMillis,
                    statusText = when {
                        isRunning -> "BERJALAN"
                        isPaused -> "DIJEDA"
                        else -> "SIAP"
                    },
                    statusColor = when {
                        isRunning -> PlayfulMustard
                        isPaused -> PlayfulCoral
                        else -> PlayfulGray
                    }
                )
            }

            // 2. Summary Stats Cards
            if (laps.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatBox(title = "LAPS", value = "${stats.count}", color = PlayfulBlack)
                                StatBox(title = "RATA-RATA", value = TimeFormatter.formatMillis(stats.avgDurationMillis), color = PlayfulTeal)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.testTag("unit_per_min_stat")
                                ) {
                                    Text(text = "UNIT/MIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", stats.unitsPerMinute),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PlayfulCoral
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PlayfulBackground)
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatBox(title = "TERCEPAT", value = TimeFormatter.formatMillis(stats.minDurationMillis), color = PlayfulTeal)
                                StatBox(title = "TERLAMBAT", value = TimeFormatter.formatMillis(stats.maxDurationMillis), color = PlayfulCoral)
                                StatBox(title = "STD DEV", value = String.format(Locale.US, "%.1fs", stats.stdDevMillis / 1000.0), color = PlayfulBlack)
                            }
                        }
                    }
                }

                // 3. Top 3 Fastest Leaderboard
                item {
                    LeaderboardFastestLaps(fastestLaps = fastestLaps)
                }
            }

            // 4. Laps List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RIWAYAT LAP (${laps.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Terbaru di atas",
                        fontSize = 11.sp,
                        color = PlayfulGray
                    )
                }
            }

            if (laps.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Lap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tekan tombol Mulai lalu tekan Lap saat elemen selesai",
                                fontSize = 12.sp,
                                color = PlayfulGray
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(laps.reversed(), key = { _, lap -> lap.lapId }) { _, lap ->
                    PlayfulLapItemRow(
                        lap = lap,
                        isFastest = lap.lapDurationMillis == stats.minDurationMillis && stats.count > 1,
                        isSlowest = lap.lapDurationMillis == stats.maxDurationMillis && stats.count > 1,
                        onEditLabel = { lapToEditLabel = lap },
                        onDelete = { lapToDelete = lap }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Reset Sesi Confirmation (Starts new session automatically with incremented session name)
    if (showResetDialog) {
        AppConfirmationDialog(
            title = "Mulai Sesi Baru?",
            message = "Sesi saat ini akan disimpan, lalu sistem langsung memulai sesi baru (${activeSession?.studyName}) dengan stopwatch 00:00.00 dan daftar lap baru.",
            confirmText = "Mulai Sesi Baru",
            cancelText = "Batal",
            isDestructive = false,
            onConfirm = {
                viewModel.resetSingleStopwatchChaining()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    // Dialog: Delete Lap Confirmation
    if (lapToDelete != null) {
        val lap = lapToDelete!!
        AppConfirmationDialog(
            title = "Hapus Lap #${lap.lapNumber}?",
            message = "Nomor lap dan durasi kumulatif akan dikoreksi otomatis. Anda juga dapat membatalkan melalui snackbar.",
            confirmText = "Hapus Lap",
            cancelText = "Batal",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteLap(lap)
                lapToDelete = null
            },
            onDismiss = { lapToDelete = null }
        )
    }

    // Dialog: Edit Lap Label
    if (lapToEditLabel != null) {
        val lap = lapToEditLabel!!
        var currentLabel by remember(lap.lapId) { mutableStateOf(lap.label) }
        AlertDialog(
            onDismissRequest = { lapToEditLabel = null },
            title = { Text("Label Lap #${lap.lapNumber}", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = { currentLabel = it },
                    placeholder = { Text("Contoh: Normal, Part Tertukar, Delay...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLapLabel(lap.lapId, currentLabel)
                        lapToEditLabel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { lapToEditLabel = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = PlayfulBlack)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PlayfulWhite
        )
    }

    // Dialog: Edit Session Info (Memudahkan Observasi Cepat diisi kapan saja)
    if (showEditSessionDialog && activeSession != null) {
        val s = activeSession!!
        var editStudyName by remember(s.sessionId) { mutableStateOf(s.studyName) }
        var editObserverName by remember(s.sessionId) { mutableStateOf(s.observerName) }
        var editOperatorName by remember(s.sessionId) { mutableStateOf(s.operatorName) }
        var editLine by remember(s.sessionId) { mutableStateOf(s.line) }
        var editMachine by remember(s.sessionId) { mutableStateOf(s.machine) }
        var editBuilding by remember(s.sessionId) { mutableStateOf(s.building) }
        var editSessionName by remember(s.sessionId) { mutableStateOf(s.sessionName) }

        AlertDialog(
            onDismissRequest = { showEditSessionDialog = false },
            title = {
                Text(
                    text = "Edit Informasi Sesi",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PlayfulBlack
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Lengkapi atau ubah aktivitas, observer, operator, line, mesin, atau shift.",
                        fontSize = 12.sp,
                        color = PlayfulGray
                    )
                    MasterComboboxField(
                        label = "Nama Time Study",
                        value = editStudyName,
                        onValueChange = { editStudyName = it },
                        options = masterOptions.studyNames,
                        placeholder = "Pilih atau ketik nama aktivitas...",
                        isRequired = true,
                        testTag = "edit_study_name_input"
                    )
                    MasterComboboxField(
                        label = "Observer",
                        value = editObserverName,
                        onValueChange = { editObserverName = it },
                        options = masterOptions.observers,
                        placeholder = "Pilih observer atau ketik manual...",
                        testTag = "edit_observer_input"
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Nama Operator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PlayfulBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editOperatorName,
                            onValueChange = { editOperatorName = it },
                            placeholder = { Text("Ketik nama operator...", fontSize = 13.sp, color = PlayfulGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MasterComboboxField(
                        label = "Line",
                        value = editLine,
                        onValueChange = { editLine = it },
                        options = masterOptions.lines,
                        placeholder = "Pilih Line atau ketik manual...",
                        testTag = "edit_line_input"
                    )
                    MasterComboboxField(
                        label = "Mesin",
                        value = editMachine,
                        onValueChange = { editMachine = it },
                        options = masterOptions.machines,
                        placeholder = "Pilih mesin atau ketik manual...",
                        testTag = "edit_machine_input"
                    )
                    MasterComboboxField(
                        label = "Gedung / Area",
                        value = editBuilding,
                        onValueChange = { editBuilding = it },
                        options = masterOptions.buildings,
                        placeholder = "Pilih Gedung / Area atau ketik manual...",
                        testTag = "edit_building_input"
                    )
                    MasterComboboxField(
                        label = "Shift",
                        value = editSessionName,
                        onValueChange = { editSessionName = it },
                        options = masterOptions.sessions,
                        placeholder = "Pilih Shift atau ketik manual...",
                        testTag = "edit_shift_input"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSessionMetadata(
                            studyName = editStudyName,
                            operatorName = editOperatorName,
                            observerName = editObserverName,
                            line = editLine,
                            machine = editMachine,
                            building = editBuilding,
                            sessionName = editSessionName
                        )
                        showEditSessionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditSessionDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Batal", color = PlayfulBlack)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = PlayfulWhite
        )
    }

    // Dialog: Konfirmasi Keamanan saat ingin Kembali/Keluar
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = {
                Text(
                    text = "Observasi Masih Aktif",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PlayfulBlack
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Observasi sedang berjalan atau memiliki data lap yang belum diselesaikan. Pilih tindakan agar data tidak hilang:",
                        fontSize = 13.sp,
                        color = PlayfulBlack,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showExitConfirmationDialog = false
                            viewModel.finishSessionToHome()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Selesai & Keluar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showExitConfirmationDialog = false
                            viewModel.pauseAndReturnToHome()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlayfulTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jeda & Kembali ke Beranda", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showExitConfirmationDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tetap Lanjut Observasi", color = PlayfulBlack)
                    }
                    OutlinedButton(
                        onClick = {
                            showExitConfirmationDialog = false
                            showDeleteCurrentSessionDialog = true
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlayfulCoral),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exit_delete_session_button")
                    ) {
                        Text("Hapus / Batalkan Sesi Ini", color = PlayfulCoral, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {},
            shape = RoundedCornerShape(26.dp),
            containerColor = PlayfulWhite
        )
    }

    if (showDeleteCurrentSessionDialog && activeSession != null) {
        val s = activeSession!!
        DeleteSessionDialog(
            session = s,
            onDeleteLocalOnly = {
                showDeleteCurrentSessionDialog = false
                viewModel.deleteSession(s.sessionId, deleteFromSheets = false, navigateBackOnComplete = true)
            },
            onDeleteEverywhere = {
                showDeleteCurrentSessionDialog = false
                viewModel.deleteSession(s.sessionId, deleteFromSheets = true, navigateBackOnComplete = true)
            },
            onDismiss = { showDeleteCurrentSessionDialog = false }
        )
    }

    // Dialog: Popup Simpan atau Buang khusus Observasi Cepat (DRAFT) sebelum keluar
    if (showQuickSaveDialog && activeSession != null) {
        AlertDialog(
            onDismissRequest = { showQuickSaveDialog = false },
            modifier = Modifier.testTag("quick_obs_save_dialog"),
            title = {
                Text(
                    text = "Simpan Observasi Cepat?",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PlayfulBlack
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Observasi ini masih bersifat sementara (belum masuk Riwayat atau Sesi Aktif). Pilih apakah hasil pengukuran mau disimpan sebelum keluar:",
                        fontSize = 12.sp,
                        color = PlayfulGray,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PlayfulBackground)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL WAKTU", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                            Text(
                                text = TimeFormatter.formatMillis(currentMillis),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulBlack
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LAP TERCATAT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
                            Text(
                                text = "${laps.size} lap",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulTeal
                            )
                        }
                    }

                    if (isRunning) {
                        Surface(
                            color = PlayfulCream,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Stopwatch masih berjalan — memilih simpan akan menghentikan/menjeda waktu secara otomatis.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PlayfulBlack,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = quickSaveStudyName,
                        onValueChange = { quickSaveStudyName = it },
                        label = { Text("Nama Observasi (opsional)") },
                        placeholder = { Text("Contoh: Assembly Motor, QC...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_save_name_input")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showQuickSaveDialog = false
                                viewModel.saveQuickObservationAndExit(quickSaveStudyName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_save_history_button")
                        ) {
                            Text("Simpan ke Riwayat", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showQuickSaveDialog = false
                                viewModel.promoteQuickDraftToActiveAndReturnHome(quickSaveStudyName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PlayfulTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_save_active_button")
                        ) {
                            Text("Simpan ke Sesi Aktif (Jeda)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showQuickSaveDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_continue_button")
                        ) {
                            Text("Tetap Lanjut Observasi", color = PlayfulBlack, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                showQuickSaveDialog = false
                                showQuickDiscardConfirmDialog = true
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlayfulCoral),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_discard_button")
                        ) {
                            Text("Jangan Simpan (Buang)", color = PlayfulCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(26.dp),
            containerColor = PlayfulWhite
        )
    }

    // Konfirmasi tahap ke-2 agar user tidak tidak sengaja membuang data hasil pengukuran
    if (showQuickDiscardConfirmDialog) {
        AppConfirmationDialog(
            title = "Buang Hasil Observasi?",
            message = "Durasi ${TimeFormatter.formatMillis(currentMillis)} dan ${laps.size} lap akan dihapus permanen dan tidak disimpan ke Riwayat maupun Database.",
            confirmText = "Ya, Buang Data",
            cancelText = "Kembali",
            isDestructive = true,
            onConfirm = {
                showQuickDiscardConfirmDialog = false
                viewModel.discardQuickObservationAndExit(silent = false)
            },
            onDismiss = {
                showQuickDiscardConfirmDialog = false
                showQuickSaveDialog = true
            }
        )
    }
}

@Composable
fun StatBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = PlayfulGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
    }
}

