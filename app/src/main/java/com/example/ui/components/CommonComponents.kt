package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionEntity
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialGreen
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.NeutralDark
import com.example.ui.theme.NeutralGray

@Composable
fun SyncStatusBadge(
    isOnline: Boolean,
    pendingCount: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) IndustrialGreen else Color(0xFF94A3B8))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (!isOnline) {
                    "Offline ($pendingCount pending)"
                } else if (pendingCount > 0) {
                    "Online • $pendingCount belum sync"
                } else {
                    "Online • Tersinkron"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeutralDark
            )
        }
    }
}

@Composable
fun AutocompleteChipRow(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.take(6).forEach { suggestion ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { onSelect(suggestion) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = suggestion,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeutralDark
                )
            }
        }
    }
}

@Composable
fun AppConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Ya, Lanjutkan",
    cancelText: String = "Batal",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NeutralDark)
        },
        text = {
            Text(text = message, fontSize = 14.sp, color = NeutralGray)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) IndustrialRed else IndustrialAmber,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(cancelText, color = NeutralDark)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun DeleteSessionDialog(
    session: SessionEntity,
    onDeleteLocalOnly: () -> Unit,
    onDeleteEverywhere: () -> Unit,
    onDismiss: () -> Unit
) {
    val isSynced = session.syncStatus == "SYNCED"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hapus '${session.studyName}'?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = NeutralDark
            )
        },
        text = {
            Text(
                text = if (isSynced) {
                    "Sesi ${session.sessionName} (${session.lapCount} lap) sudah tersinkron ke Google Sheets. Pilih cara penghapusan:"
                } else {
                    "Sesi ${session.sessionName} (${session.lapCount} lap) belum tersinkron ke Google Sheets dan akan dihapus permanen dari perangkat ini."
                },
                fontSize = 13.sp,
                color = NeutralGray
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSynced) {
                    Button(
                        onClick = onDeleteEverywhere,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hapus di HP & Google Sheets", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDeleteLocalOnly,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hapus di HP Saja", color = NeutralDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDeleteLocalOnly,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hapus Sesi", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = NeutralGray)
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}
