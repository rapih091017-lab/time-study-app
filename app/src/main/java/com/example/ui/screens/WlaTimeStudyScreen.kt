package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.StopwatchElementEntity
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.BigActionButton
import com.example.ui.components.DeleteSessionDialog
import com.example.ui.components.HoldToConfirmButton
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
import com.example.ui.theme.PlayfulSkyBlueLight
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.TimeStudyViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WlaTimeStudyScreen(
    sessionId: String,
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val elements by viewModel.elements.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val liveTimes by viewModel.liveElapsedTimes.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialogs
    var showAddElementDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var elementToRename by remember { mutableStateOf<StopwatchElementEntity?>(null) }
    var elementToDelete by remember { mutableStateOf<StopwatchElementEntity?>(null) }
    var elementToReset by remember { mutableStateOf<StopwatchElementEntity?>(null) }
    var elementToComplete by remember { mutableStateOf<StopwatchElementEntity?>(null) }
    var lapToDelete by remember { mutableStateOf<LapEntity?>(null) }
    var lapToEditLabel by remember { mutableStateOf<LapEntity?>(null) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var showDeleteCurrentSessionDialog by remember { mutableStateOf(false) }

    val expandedLapsMap = remember { mutableStateMapOf<String, Boolean>() }

    val isAnyRunning = elements.any { it.isRunning }
    val isAnyElapsed = elements.any { (liveTimes[it.elementId] ?: it.elapsedMillis) > 0L }
    val hasLaps = laps.isNotEmpty()

    BackHandler {
        if (isAnyRunning || isAnyElapsed || hasLaps) {
            showExitConfirmationDialog = true
        } else {
            viewModel.navigateBack()
        }
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
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeSession?.studyName ?: "Time Study WLA",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = PlayfulBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = PlayfulSkyBlueLight,
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
                            append("${elements.size} Stopwatch")
                        }
                        Text(
                            text = meta,
                            fontSize = 11.sp,
                            color = PlayfulGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isAnyRunning || isAnyElapsed || hasLaps) {
                                showExitConfirmationDialog = true
                            } else {
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier.testTag("wla_back_button")
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
                        onClick = { showAddElementDialog = true },
                        modifier = Modifier.testTag("add_stopwatch_button")
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
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Stopwatch",
                                tint = PlayfulBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlayfulBackground)
            )
        },
        bottomBar = {
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
                    // Global Actions Row: Mulai Semua, Pause Semua, Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = PlayfulTeal,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.startAllWla() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mulai Semua",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = PlayfulOrange,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.pauseAllWla() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pause Semua",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = PlayfulBackground,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlayfulBorder),
                            modifier = Modifier
                                .weight(0.8f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showResetDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PlayfulBlack, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset",
                                    color = PlayfulBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HoldToConfirmButton(
                        text = "SELESAI SESI WLA",
                        icon = Icons.Default.Check,
                        holdDurationMs = 1000L,
                        onConfirmed = { viewModel.finishSessionToHome() },
                        buttonColor = PlayfulBlack,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "finish_wla_session_button"
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ELEMEN KERJA AKTIF (${elements.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "+ Tambah Stopwatch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayfulTeal,
                        modifier = Modifier
                            .clickable { showAddElementDialog = true }
                            .padding(4.dp)
                    )
                }
            }

            // Each stopwatch card in distinct playful colors
            items(elements, key = { it.elementId }) { element ->
                val elapsed = liveTimes[element.elementId] ?: element.elapsedMillis
                val elementLaps = laps.filter { it.elementId == element.elementId }
                val isExpanded = expandedLapsMap[element.elementId] ?: false
                val isElementRunning = element.isRunning
                val isElementPaused = !isElementRunning && elapsed > 0 && !element.isCompleted
                val isElementCompleted = element.isCompleted

                PlayfulWlaElementCard(
                    element = element,
                    elapsedMillis = elapsed,
                    isRunning = isElementRunning,
                    isPaused = isElementPaused,
                    isCompleted = isElementCompleted,
                    laps = elementLaps,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedLapsMap[element.elementId] = !isExpanded },
                    onStart = { viewModel.startWlaElement(element.elementId) },
                    onPause = { viewModel.pauseWlaElement(element.elementId) },
                    onResume = { viewModel.resumeWlaElement(element.elementId) },
                    onReset = { elementToReset = element },
                    onComplete = { elementToComplete = element },
                    onReopen = { viewModel.reopenWlaElement(element.elementId) },
                    onRecordLap = { viewModel.recordWlaLap(element.elementId) },
                    onRename = { elementToRename = element },
                    onDelete = { elementToDelete = element },
                    onEditLapLabel = { lapToEditLabel = it },
                    onDeleteLap = { lapToDelete = it }
                )
            }

            // WLA Summary Statistics Card
            if (laps.isNotEmpty()) {
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
                                text = "RINGKASAN ELEMEN WLA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = PlayfulGray,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            elements.forEach { el ->
                                val elLaps = laps.filter { it.elementId == el.elementId }
                                val stats = viewModel.calculateStats(elLaps)
                                val elTime = liveTimes[el.elementId] ?: el.elapsedMillis

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(el.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = el.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PlayfulBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = TimeFormatter.formatMillis(elTime),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PlayfulBlack
                                        )
                                        Text(
                                            text = "${stats.count} lap • avg ${TimeFormatter.formatMillis(stats.avgDurationMillis)}",
                                            fontSize = 10.sp,
                                            color = PlayfulGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Reset WLA Chaining
    if (showResetDialog) {
        AppConfirmationDialog(
            title = "Mulai Sesi WLA Baru?",
            message = "Sesi WLA saat ini akan disimpan, lalu sistem langsung memulai sesi WLA baru dengan daftar stopwatch yang sama dan lap kosong.",
            confirmText = "Mulai Sesi Baru",
            cancelText = "Batal",
            isDestructive = false,
            onConfirm = {
                viewModel.resetWlaChaining()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    // Dialog: Reset Individual Element Stopwatch
    if (elementToReset != null) {
        val el = elementToReset!!
        AppConfirmationDialog(
            title = "Reset Stopwatch '${el.name}'?",
            message = "Waktu stopwatch '${el.name}' akan kembali ke 00:00.00 dan semua lap pada elemen ini akan dihapus.",
            confirmText = "Reset Stopwatch",
            cancelText = "Batal",
            isDestructive = true,
            onConfirm = {
                viewModel.resetWlaElement(el.elementId)
                elementToReset = null
            },
            onDismiss = { elementToReset = null }
        )
    }

    // Dialog: Complete Individual Element
    if (elementToComplete != null) {
        val el = elementToComplete!!
        AppConfirmationDialog(
            title = "Selesaikan '${el.name}'?",
            message = "Stopwatch '${el.name}' akan dikunci sebagai selesai dan lap akhir akan dicatat otomatis. Elemen lainnya tetap berjalan.",
            confirmText = "Tandai Selesai",
            cancelText = "Batal",
            isDestructive = false,
            onConfirm = {
                viewModel.completeWlaElement(el.elementId)
                elementToComplete = null
            },
            onDismiss = { elementToComplete = null }
        )
    }

    // Dialog: Add Element
    if (showAddElementDialog) {
        var elementNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddElementDialog = false },
            title = { Text("Tambah Stopwatch Elemen", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Nama elemen kerja yang diamati:", fontSize = 13.sp, color = PlayfulGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = elementNameInput,
                        onValueChange = { elementNameInput = it },
                        placeholder = { Text("Contoh: Cuci Kolong, Rakit Body") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("new_stopwatch_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addWlaElement(elementNameInput.ifBlank { "Elemen ${elements.size + 1}" })
                        showAddElementDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddElementDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = PlayfulBlack)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PlayfulWhite
        )
    }

    // Dialog: Rename Element
    if (elementToRename != null) {
        val el = elementToRename!!
        var renameInput by remember(el.elementId) { mutableStateOf(el.name) }
        AlertDialog(
            onDismissRequest = { elementToRename = null },
            title = { Text("Ganti Nama Stopwatch", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameWlaElement(el.elementId, renameInput)
                        }
                        elementToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { elementToRename = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal", color = PlayfulBlack)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = PlayfulWhite
        )
    }

    // Dialog: Delete Element
    if (elementToDelete != null) {
        val el = elementToDelete!!
        AppConfirmationDialog(
            title = "Hapus Stopwatch '${el.name}'?",
            message = "Seluruh lap pada stopwatch ini juga akan dihapus.",
            confirmText = "Hapus",
            cancelText = "Batal",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteWlaElement(el.elementId)
                elementToDelete = null
            },
            onDismiss = { elementToDelete = null }
        )
    }

    // Dialog: Delete Lap
    if (lapToDelete != null) {
        val lap = lapToDelete!!
        AppConfirmationDialog(
            title = "Hapus Lap #${lap.lapNumber}?",
            message = "Nomor lap akan dikoreksi otomatis.",
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

    // Dialog: Konfirmasi Keamanan saat ingin Kembali/Keluar
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = {
                Text(
                    text = "Observasi WLA Masih Aktif",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PlayfulBlack
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stopwatch WLA sedang berjalan atau memiliki data lap yang belum diselesaikan. Pilih tindakan agar data tidak hilang:",
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
                            .testTag("exit_delete_wla_session_button")
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
}

@Composable
fun PlayfulWlaElementCard(
    element: StopwatchElementEntity,
    elapsedMillis: Long,
    isRunning: Boolean,
    isPaused: Boolean,
    isCompleted: Boolean = false,
    laps: List<LapEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onRecordLap: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onEditLapLabel: (LapEntity) -> Unit,
    onDeleteLap: (LapEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = parseHexColor(element.colorHex)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wla_element_card_${element.elementId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
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
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = element.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = PlayfulBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PlayfulTealLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PlayfulTeal, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PlayfulTeal, maxLines = 1)
                            }
                        }
                    } else if (isPaused) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PlayfulOrange.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Dijeda", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PlayfulOrange, maxLines = 1)
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = PlayfulGray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ganti Nama") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        if (isCompleted) {
                            DropdownMenuItem(
                                text = { Text("Buka Kembali", color = PlayfulTeal) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PlayfulTeal) },
                                onClick = {
                                    showMenu = false
                                    onReopen()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Tandai Selesai", color = PlayfulTeal) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = PlayfulTeal) },
                                onClick = {
                                    showMenu = false
                                    onComplete()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Reset Stopwatch") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onReset()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Stopwatch", color = PlayfulCoral) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PlayfulCoral) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Timer Display + Status / Reopen Pill (No crowding with 4 buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimeFormatter.formatMillis(elapsedMillis),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = when {
                        isCompleted -> PlayfulTeal
                        isRunning -> PlayfulTeal
                        else -> PlayfulBlack
                    }
                )

                if (isCompleted) {
                    Surface(
                        color = PlayfulTealLight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onReopen() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PlayfulTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Lagi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlayfulTeal)
                        }
                    }
                } else {
                    Surface(
                        color = when {
                            isRunning -> PlayfulTealLight
                            isPaused -> PlayfulCream
                            else -> PlayfulBackground
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = when {
                                isRunning -> "BERJALAN"
                                isPaused -> "DIJEDA"
                                else -> "SIAP"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isRunning -> PlayfulTeal
                                isPaused -> PlayfulOrange
                                else -> PlayfulGray
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width Controls Row (Prevents collision with timer on narrow screens)
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PlayfulBackground.copy(alpha = 0.7f))
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        !isRunning && elapsedMillis == 0L -> {
                            RoundControlButton(
                                icon = Icons.Default.PlayArrow,
                                label = "Mulai",
                                onClick = onStart,
                                bgColor = tagColor,
                                contentColor = PlayfulBlack,
                                size = 46.dp
                            )
                        }
                        isRunning -> {
                            RoundControlButton(
                                icon = Icons.Default.Pause,
                                label = "Pause",
                                onClick = onPause,
                                bgColor = PlayfulOrange,
                                contentColor = Color.White,
                                size = 44.dp
                            )
                            RoundControlButton(
                                icon = Icons.Default.Flag,
                                label = "Lap",
                                onClick = onRecordLap,
                                bgColor = PlayfulBlack,
                                contentColor = PlayfulWhite,
                                size = 44.dp
                            )
                            RoundControlButton(
                                icon = Icons.Default.Check,
                                label = "Selesai",
                                onClick = onComplete,
                                bgColor = PlayfulTeal,
                                contentColor = PlayfulWhite,
                                size = 44.dp
                            )
                        }
                        else -> { // isPaused
                            RoundControlButton(
                                icon = Icons.Default.PlayArrow,
                                label = "Resume",
                                onClick = onResume,
                                bgColor = PlayfulTeal,
                                contentColor = Color.White,
                                size = 44.dp
                            )
                            RoundControlButton(
                                icon = Icons.Default.Refresh,
                                label = "Reset",
                                onClick = onReset,
                                bgColor = PlayfulWhite,
                                contentColor = PlayfulBlack,
                                size = 44.dp
                            )
                            RoundControlButton(
                                icon = Icons.Default.Flag,
                                label = "Lap",
                                onClick = onRecordLap,
                                bgColor = PlayfulBlack,
                                contentColor = PlayfulWhite,
                                size = 44.dp
                            )
                            RoundControlButton(
                                icon = Icons.Default.Check,
                                label = "Selesai",
                                onClick = onComplete,
                                bgColor = PlayfulTeal,
                                contentColor = PlayfulWhite,
                                size = 44.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lap expander header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${laps.size} Lap tercatat" + if (laps.isNotEmpty()) " (Terakhir: ${TimeFormatter.formatMillis(laps.last().lapDurationMillis)})" else "",
                    fontSize = 12.sp,
                    color = PlayfulGray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Sembunyikan" else "Buka Lap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayfulBlack
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PlayfulBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (laps.isEmpty()) {
                        Text(
                            text = "Belum ada lap tercatat",
                            fontSize = 12.sp,
                            color = PlayfulGray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        laps.reversed().forEach { lap ->
                            PlayfulLapItemRow(
                                lap = lap,
                                onEditLabel = { onEditLapLabel(lap) },
                                onDelete = { onDeleteLap(lap) }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        PlayfulMustard
    }
}
