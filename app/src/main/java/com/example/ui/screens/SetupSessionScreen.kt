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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.BigOutlinedActionButton
import com.example.ui.components.MasterComboboxField
import com.example.ui.components.PlayfulPrimaryButton
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavender
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulSkyBlue
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulWhite
import com.example.viewmodel.TimeStudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSessionScreen(
    mode: String, // "TS" or "WLA"
    viewModel: TimeStudyViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val masterOptions by viewModel.masterOptions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mode == "WLA") "Setup Time Study WLA" else "Setup Time Study",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = PlayfulBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("setup_back_button")
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
        bottomBar = {
            Surface(
                color = PlayfulWhite,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PlayfulPrimaryButton(
                        text = "Mulai Sesi Sekarang",
                        icon = Icons.Default.PlayArrow,
                        onClick = { viewModel.startSessionFromForm(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "start_session_button"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fast Fill Card: Pakai data sesi terakhir
            item {
                BigOutlinedActionButton(
                    text = "Pakai data sesi terakhir",
                    icon = Icons.Default.History,
                    onClick = { viewModel.useLastSessionData() },
                    color = PlayfulBlack,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "use_last_session_button"
                )
            }

            if (mode == "TS") {
                item {
                    Surface(
                        color = PlayfulLavenderLight,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.startQuickObservation() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PlayfulCoral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = PlayfulWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Butuh cepat tanpa isi data?",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = PlayfulBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Buka langsung sebagai stopwatch biasa ⚡",
                                    fontSize = 11.sp,
                                    color = PlayfulGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PlayfulGray)
                        }
                    }
                }
            }

            // Main Form Container Card
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Nama Time Study (Wajib) - Dropdown Master + Input Manual
                        MasterComboboxField(
                            label = "Nama Time Study (Aktivitas)",
                            value = formState.studyName,
                            onValueChange = { viewModel.updateFormField(studyName = it) },
                            options = masterOptions.studyNames,
                            placeholder = "Pilih dari Master atau ketik manual...",
                            isRequired = true,
                            isError = formState.isStudyNameError,
                            testTag = "study_name_input"
                        )

                        // 2. Shift - Dropdown Master + Input Manual (Full-width agar tidak sempit)
                        MasterComboboxField(
                            label = "Shift",
                            value = formState.sessionName,
                            onValueChange = { viewModel.updateFormField(sessionName = it) },
                            options = masterOptions.sessions,
                            placeholder = "Pilih Shift atau ketik manual (mis. Shift 1)...",
                            testTag = "shift_input"
                        )

                        // 3. Tanggal
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tanggal (YYYY-MM-DD)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = formState.date,
                                onValueChange = { viewModel.updateFormField(date = it) },
                                placeholder = { Text("2026-11-25", fontSize = 13.sp, color = PlayfulGray) },
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
                                    .testTag("date_input")
                            )
                        }

                        // 4. Observer - Dropdown Master + Input Manual
                        MasterComboboxField(
                            label = "Observer",
                            value = formState.observerName,
                            onValueChange = { viewModel.updateFormField(observerName = it) },
                            options = masterOptions.observers,
                            placeholder = "Pilih observer atau ketik manual...",
                            testTag = "observer_input"
                        )

                        // 5. Nama Operator - Input Manual Langsung (TANPA dropdown)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Nama Operator (Yang Dikerjakan)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = formState.operatorName,
                                onValueChange = { viewModel.updateFormField(operatorName = it) },
                                placeholder = {
                                    Text(
                                        "Ketik nama operator...",
                                        fontSize = 13.sp,
                                        color = PlayfulGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    if (formState.operatorName.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateFormField(operatorName = "") }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Hapus operator",
                                                tint = PlayfulGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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
                                    .testTag("operator_input")
                            )
                        }

                        // 6. Line - Dropdown Master + Input Manual
                        MasterComboboxField(
                            label = "Line",
                            value = formState.line,
                            onValueChange = { viewModel.updateFormField(line = it) },
                            options = masterOptions.lines,
                            placeholder = "Pilih Line atau ketik manual...",
                            testTag = "line_input"
                        )

                        // 7. Mesin - Dropdown Master + Input Manual
                        MasterComboboxField(
                            label = "Mesin",
                            value = formState.machine,
                            onValueChange = { viewModel.updateFormField(machine = it) },
                            options = masterOptions.machines,
                            placeholder = "Pilih mesin atau ketik manual...",
                            testTag = "machine_input"
                        )

                        // 8. Gedung / Area - Dropdown Master + Input Manual
                        MasterComboboxField(
                            label = "Gedung / Area",
                            value = formState.building,
                            onValueChange = { viewModel.updateFormField(building = it) },
                            options = masterOptions.buildings,
                            placeholder = "Pilih Gedung / Area atau ketik manual...",
                            testTag = "building_input"
                        )

                        // 9. Catatan (Opsional)
                        Column {
                            Text(
                                text = "Catatan Tambahan (Opsional)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PlayfulBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = formState.notes,
                                onValueChange = { viewModel.updateFormField(notes = it) },
                                placeholder = { Text("Kondisi shift, hambatan awal, variasi...", fontSize = 13.sp, color = PlayfulGray) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PlayfulBlack,
                                    unfocusedBorderColor = PlayfulBorder,
                                    focusedContainerColor = PlayfulWhite,
                                    unfocusedContainerColor = PlayfulWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notes_input")
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
