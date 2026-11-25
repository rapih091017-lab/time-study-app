package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.sync.GoogleAppsScriptCode
import com.example.ui.components.BigActionButton
import com.example.ui.components.BigOutlinedActionButton
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.PlayfulPageHeader
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulOrange
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.TimeStudyViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.selectNavTab(0)
    }

    var gasUrlInput by remember { mutableStateOf(viewModel.syncManager.getGasUrl()) }
    var spreadsheetUrlInput by remember { mutableStateOf(viewModel.syncManager.getSpreadsheetUrl()) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val pendingSessionsCount by viewModel.pendingSessionsCount.collectAsStateWithLifecycle()
    val pendingLapsCount by viewModel.pendingLapsCount.collectAsStateWithLifecycle()
    val saveToMaster by viewModel.saveToMaster.collectAsStateWithLifecycle()
    val isDatabaseLocked by viewModel.isDatabaseLocked.collectAsStateWithLifecycle()

    val totalPending = pendingSessionsCount + pendingLapsCount

    LaunchedEffect(isDatabaseLocked) {
        spreadsheetUrlInput = viewModel.syncManager.getSpreadsheetUrl()
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingPillNavBar(
                selectedTab = 3,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                PlayfulPageHeader(
                    title = "Pengaturan",
                    subtitle = "Konfigurasi Cloud, Master Data & Ekspor"
                )
            }

            // Panduan Penggunaan Aplikasi Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PlayfulLavenderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PlayfulWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Panduan",
                                    tint = PlayfulBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Panduan Aplikasi",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = PlayfulBlack
                                )
                                Text(
                                    text = "Lihat ringkasan cara pakai stopwatch, Master & riwayat",
                                    fontSize = 11.sp,
                                    color = PlayfulGray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.openOnboardingGuide() },
                            colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("settings_open_guide_button")
                        ) {
                            Text("Buka", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlayfulWhite)
                        }
                    }
                }
            }

            // Google Sheets Sync Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PlayfulTealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = PlayfulTeal, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Sheets Web App",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = PlayfulBlack
                                )
                                Text(
                                    text = if (isOnline) "Perangkat Online" else "Perangkat Offline",
                                    fontSize = 12.sp,
                                    color = if (isOnline) PlayfulTeal else PlayfulGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "URL Web App (Google Apps Script):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayfulBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = gasUrlInput,
                            onValueChange = {
                                gasUrlInput = it
                                viewModel.syncManager.saveGasUrl(it)
                            },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 12.sp, color = PlayfulGray) },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = PlayfulGray, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlayfulBlack,
                                unfocusedBorderColor = PlayfulBorder,
                                focusedContainerColor = PlayfulWhite,
                                unfocusedContainerColor = PlayfulWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gas_url_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Connection and Sync Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BigOutlinedActionButton(
                                text = if (isTestingConnection) "Menguji..." else "Tes Koneksi",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    isTestingConnection = true
                                    viewModel.testGasConnection(gasUrlInput) { _, msg ->
                                        isTestingConnection = false
                                        testResultText = msg
                                    }
                                },
                                minHeight = 48.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "test_connection_button"
                            )

                            BigActionButton(
                                text = if (syncing) "Sync..." else "Sync Sekarang",
                                icon = Icons.Default.Sync,
                                onClick = { viewModel.syncData(silent = false) },
                                enabled = !syncing && isOnline,
                                backgroundColor = PlayfulBlack,
                                minHeight = 48.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "sync_now_button"
                            )
                        }

                        if (testResultText != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PlayfulBackground)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = testResultText!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlayfulBlack
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Master Sync Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BigOutlinedActionButton(
                                text = "Refresh Master",
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel.refreshMasterData() },
                                minHeight = 44.dp,
                                modifier = Modifier.weight(1f)
                            )

                            BigOutlinedActionButton(
                                text = "Tarik Sessions",
                                icon = Icons.Default.CloudDownload,
                                onClick = { viewModel.pullSessionsFromSheets() },
                                minHeight = 44.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Database Google Sheets:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlayfulBlack,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isDatabaseLocked) PlayfulTealLight else PlayfulCream,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isDatabaseLocked) "Terkunci Default" else "Input Manual",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDatabaseLocked) PlayfulTeal else PlayfulOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = spreadsheetUrlInput,
                            onValueChange = {
                                if (!isDatabaseLocked) {
                                    spreadsheetUrlInput = it
                                    viewModel.syncManager.saveSpreadsheetUrl(it)
                                }
                            },
                            readOnly = isDatabaseLocked,
                            placeholder = { Text("https://docs.google.com/spreadsheets/d/...", fontSize = 12.sp, color = PlayfulGray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDatabaseLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isDatabaseLocked) "Database Terkunci" else "Mode Manual",
                                    tint = if (isDatabaseLocked) PlayfulTeal else PlayfulOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlayfulBlack,
                                unfocusedBorderColor = PlayfulBorder,
                                focusedContainerColor = if (isDatabaseLocked) PlayfulBackground else PlayfulWhite,
                                unfocusedContainerColor = if (isDatabaseLocked) PlayfulBackground else PlayfulWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spreadsheet_url_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isDatabaseLocked) {
                                "Terkunci ke Spreadsheet utama Anda agar aman. Pilih 'Ubah Manual' jika ingin mengganti link."
                            } else {
                                "Mode input manual aktif. Anda dapat mengetik link Spreadsheet lain atau kembali mengunci ke default."
                            },
                            fontSize = 11.sp,
                            color = PlayfulGray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BigOutlinedActionButton(
                                text = if (isDatabaseLocked) "Ubah Manual" else "Kunci Default",
                                icon = if (isDatabaseLocked) Icons.Default.Edit else Icons.Default.Lock,
                                onClick = {
                                    if (isDatabaseLocked) {
                                        viewModel.setDatabaseLocked(false)
                                    } else {
                                        viewModel.resetSpreadsheetToDefaultLocked()
                                    }
                                },
                                minHeight = 46.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                testTag = "toggle_database_lock_button"
                            )

                            BigActionButton(
                                text = "Buka Sheets",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                onClick = { viewModel.openDatabaseSpreadsheet(context) },
                                backgroundColor = Color(0xFF16A34A),
                                minHeight = 46.dp,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                testTag = "settings_open_spreadsheet_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle: Simpan nilai baru ke Master
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PlayfulBackground)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simpan Nilai Baru ke Master",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PlayfulBlack
                                )
                                Text(
                                    text = "Nilai yang diketik otomatis masuk ke sheet Master",
                                    fontSize = 11.sp,
                                    color = PlayfulGray
                                )
                            }
                            Switch(
                                checked = saveToMaster,
                                onCheckedChange = { viewModel.setSaveToMaster(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PlayfulWhite,
                                    checkedTrackColor = PlayfulTeal
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data belum tersinkron:", fontSize = 12.sp, color = PlayfulGray)
                            Text(
                                text = "$totalPending data ($pendingSessionsCount sesi, $pendingLapsCount lap)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPending > 0) PlayfulCoral else PlayfulTeal
                            )
                        }
                    }
                }
            }

            // Global Export Section
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
                            text = "Ekspor Semua Data",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = PlayfulBlack
                        )
                        Text(
                            text = "Unduh cadangan seluruh sesi & lap dalam format Excel atau CSV",
                            fontSize = 12.sp,
                            color = PlayfulGray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                testTag = "settings_export_excel"
                            )
                            BigActionButton(
                                text = "CSV (.csv)",
                                icon = Icons.Default.Share,
                                onClick = { viewModel.exportAllData(isExcel = false) },
                                backgroundColor = PlayfulBlack,
                                minHeight = 48.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                testTag = "settings_export_csv"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        BigOutlinedActionButton(
                            text = "Export Ringkasan Sesi (.xlsx)",
                            icon = Icons.Default.FileDownload,
                            onClick = { viewModel.exportSessionsSummary(isExcel = true) },
                            minHeight = 46.dp,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Deployment Guide Card
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
                            Text(
                                text = "Kode Apps Script (v2)",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = PlayfulBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Google Apps Script", GoogleAppsScriptCode.SCRIPT_TEXT)
                                    clipboard.setPrimaryClip(clip)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Kode Apps Script berhasil disalin!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PlayfulBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Kode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = PlayfulTealLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PlayfulTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tips Run di Apps Script: Jika ingin membuat sheet secara instan di Google Sheets, pilih fungsi 'inisialisasiSemuaSheet' di toolbar atas lalu klik 'Run' (Jalankan).",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlayfulTeal,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = GoogleAppsScriptCode.SETUP_GUIDE,
                            fontSize = 12.sp,
                            color = PlayfulBlack,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlayfulBlack)
                                .padding(12.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = GoogleAppsScriptCode.SCRIPT_TEXT,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = PlayfulWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
