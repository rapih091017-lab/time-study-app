package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private data class GuideStep(
    val badge: String,
    val title: String,
    val description: String,
    val tips: List<String>,
    val icon: ImageVector,
    val accentBg: Color,
    val accentTint: Color
)

@Composable
fun OnboardingGuideDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            GuideStep(
                badge = "LANGKAH 1 / 4",
                title = "Pilih Mode Pengamatan",
                description = "Mulai Time Study sesuai kebutuhan di lapangan secara cepat dan akurat:",
                tips = listOf(
                    "Observasi Cepat: Langsung buka stopwatch instan tanpa isi formulir.",
                    "Time Study: 1 stopwatch lengkap dengan data Shift, Observer, Operator, Line & Mesin.",
                    "Time Study WLA: Multi-stopwatch untuk beberapa elemen kerja sekaligus."
                ),
                icon = Icons.Default.Timer,
                accentBg = PlayfulLavenderLight,
                accentTint = PlayfulBlack
            ),
            GuideStep(
                badge = "LANGKAH 2 / 4",
                title = "Dropdown Master & Ketik Manual",
                description = "Pengisian form Setup dibuat fleksibel agar pengamatan di lapangan tidak terhambat:",
                tips = listOf(
                    "Kolom dengan dropdown (Time Study, Shift, Observer, Line, Mesin, Gedung) bisa dipilih dari Master ATAU langsung diketik manual jika belum ada di daftar.",
                    "Kolom Nama Operator langsung diketik manual tanpa dropdown.",
                    "Hasil otomatis menghitung rata-rata waktu dan output Unit/Min."
                ),
                icon = Icons.Default.EditNote,
                accentBg = PlayfulCream,
                accentTint = PlayfulCoral
            ),
            GuideStep(
                badge = "LANGKAH 3 / 4",
                title = "Database Google Sheets Terkunci",
                description = "Aplikasi sudah langsung diarahkan ke Google Spreadsheet database utama:",
                tips = listOf(
                    "Link Spreadsheet utama terkunci otomatis agar tidak berubah tidak sengaja.",
                    "Anda tetap bisa menekan 'Ubah Manual' di menu Pengaturan bila perlu mengganti link.",
                    "Untuk sinkronisasi 2 arah otomatis, pastikan URL Web App Apps Script (/exec) sudah terisi di Pengaturan."
                ),
                icon = Icons.Default.CloudDone,
                accentBg = PlayfulTealLight,
                accentTint = PlayfulTeal
            ),
            GuideStep(
                badge = "LANGKAH 4 / 4",
                title = "Riwayat per Bulan & Ekspor",
                description = "Pantau hasil observasi lintas tanggal dan ekspor laporan kapan saja:",
                tips = listOf(
                    "Di menu Riwayat, gunakan panah bulan (< >) atau tombol Kalender untuk melihat data bulan-bulan sebelumnya.",
                    "Urutkan riwayat berdasarkan Terbaru, Terlama, Lap Terbanyak, atau Nama A-Z.",
                    "Panduan ini dapat dibuka kembali kapan saja melalui ikon (?) di Beranda atau menu Pengaturan."
                ),
                icon = Icons.Default.CalendarMonth,
                accentBg = PlayfulSkyBlueLight,
                accentTint = PlayfulBlack
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PlayfulWhite,
            shadowElevation = 12.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 440.dp)
                .testTag("onboarding_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Row: Step Badge + Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PlayfulMustard,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = step.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = PlayfulBlack,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("guide_skip_button")
                    ) {
                        Text(
                            text = "Lewati",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayfulGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(step.accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = step.title,
                            tint = step.accentTint,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = step.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayfulBlack,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = step.description,
                        fontSize = 13.sp,
                        color = PlayfulGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        step.tips.forEachIndexed { idx, tip ->
                            Surface(
                                color = PlayfulBackground,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(PlayfulBlack),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PlayfulWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tip,
                                        fontSize = 12.sp,
                                        color = PlayfulBlack,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { idx ->
                        val active = idx == currentStep
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(if (active) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (active) PlayfulBlack else PlayfulBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Sebelumnya",
                                tint = PlayfulBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali", color = PlayfulBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == steps.lastIndex) PlayfulTeal else PlayfulBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(if (currentStep > 0) 1.3f else 1f)
                            .heightIn(min = 48.dp)
                            .testTag("guide_next_button")
                    ) {
                        if (currentStep < steps.lastIndex) {
                            Text("Lanjut", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlayfulWhite)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PlayfulWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PlayfulWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Aplikasi", fontWeight = FontWeight.Black, fontSize = 13.sp, color = PlayfulWhite)
                        }
                    }
                }
            }
        }
    }
}
