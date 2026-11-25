package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.NeutralDark
import java.util.Locale

object TimeFormatter {
    fun formatMillis(millis: Long): String {
        val totalCentis = millis / 10
        val centis = totalCentis % 100
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
        }
    }

    fun formatDurationShort(millis: Long): String {
        val totalSeconds = millis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.US, "%dj %02dm %02dd", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02dm %02dd", minutes, seconds)
        }
    }
}

@Composable
fun LargeStopwatchDisplay(
    millis: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    val formatted = TimeFormatter.formatMillis(millis)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 2.dp,
                color = if (isRunning) IndustrialAmber else Color(0xFF334155),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 24.dp, horizontal = 16.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (formatted.length > 8) 42.sp else 52.sp,
                color = if (isRunning) Color(0xFFFDE047) else Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isRunning) "● STOPWATCH BERJALAN" else "⏸ STOPWATCH BERHENTI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) IndustrialAmber else Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CompactStopwatchDisplay(
    millis: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val formatted = TimeFormatter.formatMillis(millis)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = if (isRunning) Color(0xFFFDE047) else Color.White
        )
    }
}
