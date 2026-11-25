package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LapEntity
import com.example.ui.theme.PlayfulBackground
import com.example.ui.theme.PlayfulBlack
import com.example.ui.theme.PlayfulBorder
import com.example.ui.theme.PlayfulCoral
import com.example.ui.theme.PlayfulCream
import com.example.ui.theme.PlayfulGray
import com.example.ui.theme.PlayfulLavender
import com.example.ui.theme.PlayfulLavenderLight
import com.example.ui.theme.PlayfulLightGray
import com.example.ui.theme.PlayfulMustard
import com.example.ui.theme.PlayfulOrange
import com.example.ui.theme.PlayfulSkyBlue
import com.example.ui.theme.PlayfulTeal
import com.example.ui.theme.PlayfulTealLight
import com.example.ui.theme.PlayfulWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Floating Pill Bottom Navigation Bar (as seen in Reference Image 2)
 */
@Composable
fun FloatingPillNavBar(
    selectedTab: Int, // 0: Home, 1: Database, 2: History/Stats, 3: Settings
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = PlayfulBlack,
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NavPillItem(
                    icon = Icons.Default.Home,
                    label = "Beranda",
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    testTag = "nav_home"
                )
                NavPillItem(
                    icon = Icons.Default.Storage,
                    label = "Database",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    testTag = "nav_database"
                )
                NavPillItem(
                    icon = Icons.Default.BarChart,
                    label = "Riwayat",
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    testTag = "nav_history"
                )
                NavPillItem(
                    icon = Icons.Default.Settings,
                    label = "Pengaturan",
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    testTag = "nav_settings"
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (isSelected) PlayfulWhite else Color.Transparent)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PlayfulBlack else PlayfulGray,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Top Page Header with Circular Avatar Initials + Greeting + Today's Date
 */
@Composable
fun PlayfulPageHeader(
    title: String,
    subtitle: String,
    avatarName: String = "Engineer",
    rightActionIcon: ImageVector? = null,
    onRightActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val initial = avatarName.trim().take(1).uppercase().ifBlank { "E" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Circle Avatar with initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PlayfulLavenderLight)
                    .border(2.dp, PlayfulLavender, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = PlayfulBlack
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = PlayfulBlack,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlayfulGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (rightActionIcon != null && onRightActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PlayfulWhite)
                    .border(1.dp, PlayfulBorder, CircleShape)
                    .clickable { onRightActionClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = rightActionIcon,
                    contentDescription = null,
                    tint = PlayfulBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Horizontal Pill Date Strip (Min - Sab) with dots on dates that have recorded data
 */
data class DayDateItem(
    val dayLabel: String,
    val dayNumber: String,
    val fullDate: String,
    val hasData: Boolean
)

@Composable
fun HorizontalDateStrip(
    selectedDate: String,
    datesWithData: List<String>,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    displayYear: Int? = null,
    displayMonth: Int? = null, // 0-based Calendar.MONTH
    allOptionLabel: String = "Semua"
) {
    val daysList = remember(datesWithData, displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        val formatFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatDayName = SimpleDateFormat("EEE", Locale("id", "ID"))
        val formatDayNum = SimpleDateFormat("dd", Locale.getDefault())

        val list = mutableListOf<DayDateItem>()
        if (displayYear != null && displayMonth != null) {
            cal.set(Calendar.YEAR, displayYear)
            cal.set(Calendar.MONTH, displayMonth)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..maxDays) {
                val full = formatFull.format(cal.time)
                val dName = formatDayName.format(cal.time).replace(".", "").take(3).replaceFirstChar { it.uppercase() }
                val dNum = formatDayNum.format(cal.time)
                list.add(
                    DayDateItem(
                        dayLabel = dName,
                        dayNumber = dNum,
                        fullDate = full,
                        hasData = datesWithData.contains(full)
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Show 14 days centered around today
            cal.add(Calendar.DAY_OF_YEAR, -5)
            for (i in 0..13) {
                val full = formatFull.format(cal.time)
                val dName = formatDayName.format(cal.time).replace(".", "").take(3).replaceFirstChar { it.uppercase() }
                val dNum = formatDayNum.format(cal.time)
                list.add(
                    DayDateItem(
                        dayLabel = dName,
                        dayNumber = dNum,
                        fullDate = full,
                        hasData = datesWithData.contains(full)
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Semua" pill option
        val isAllSelected = selectedDate.isBlank()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(if (isAllSelected) PlayfulBlack else PlayfulWhite)
                .border(1.dp, if (isAllSelected) PlayfulBlack else PlayfulBorder, RoundedCornerShape(22.dp))
                .clickable { onDateSelected("") }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = allOptionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAllSelected) PlayfulWhite else PlayfulGray
            )
        }

        daysList.forEach { item ->
            val isSelected = selectedDate == item.fullDate
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) PlayfulBlack else PlayfulWhite)
                    .clickable { onDateSelected(item.fullDate) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Small dot if session exists
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.hasData) {
                                    if (isSelected) PlayfulWhite else PlayfulCoral
                                } else Color.Transparent
                            )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.dayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) PlayfulWhite.copy(alpha = 0.7f) else PlayfulGray
                    )
                    Text(
                        text = item.dayNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) PlayfulWhite else PlayfulBlack
                    )
                }
            }
        }
    }
}

/**
 * 3D-ish Canvas Ornaments (geometric cylinders, gradient concentric spheres)
 */
@Composable
fun Geometric3dOrnament(
    modifier: Modifier = Modifier,
    accentColor: Color = PlayfulCoral
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer glow circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(w * 0.7f, h * 0.4f),
                radius = w * 0.5f
            )
        )

        // Concentric ring
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = w * 0.32f,
            center = Offset(w * 0.65f, h * 0.45f),
            style = Stroke(width = 8f)
        )

        // Spherical solid bubble
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), accentColor),
                start = Offset(w * 0.5f, h * 0.2f),
                end = Offset(w * 0.8f, h * 0.7f)
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.65f, h * 0.45f)
        )
    }
}

/**
 * Giant Stopwatch Dial with Curved Accent Header
 */
@Composable
fun CurvedStopwatchDial(
    millis: Long,
    statusText: String, // "BERJALAN", "DIJEDA", "SIAP"
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val formatted = TimeFormatter.formatMillis(millis)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(PlayfulTeal, Color(0xFF1E5E52))
                )
            )
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background subtle wave / circle ornament
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.5f, size.height * 0.4f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                style = Stroke(width = 4f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status pill
            Surface(
                color = statusColor.copy(alpha = 0.22f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Giant time text
            Text(
                text = formatted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = if (formatted.length > 8) 44.sp else 54.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Presisi 1/100 detik",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Top 3 Leaderboard Fastest Laps Row (#1 Gold, #2 Lavender/Silver, #3 Coral/Bronze)
 */
@Composable
fun LeaderboardFastestLaps(
    fastestLaps: List<com.example.data.model.LapEntity>,
    modifier: Modifier = Modifier,
    showElementName: Boolean = false
) {
    if (fastestLaps.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PlayfulWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEADERBOARD LAP TERCEPAT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = PlayfulGray,
                letterSpacing = 1.sp
            )
            Text(
                text = "Top 3",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlayfulBlack
            )
        }

        fastestLaps.take(3).forEachIndexed { idx, lap ->
            val rankNumber = idx + 1
            val (badgeBg, badgeText) = when (rankNumber) {
                1 -> Pair(PlayfulMustard, PlayfulBlack)
                2 -> Pair(PlayfulLavender, PlayfulBlack)
                else -> Pair(PlayfulCoral, Color.White)
            }

            Surface(
                color = PlayfulBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$rankNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeText
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val isQuickObs = lap.elementName.startsWith("Observasi Cepat")
                        val showElement = showElementName && lap.elementName.isNotBlank() && lap.elementName != "Elemen" && !isQuickObs
                        Text(
                            text = "Lap #${lap.lapNumber}" + if (showElement) " • ${lap.elementName}" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PlayfulBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (lap.label.isNotBlank()) {
                            Text(
                                text = lap.label,
                                fontSize = 10.sp,
                                color = PlayfulGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PlayfulWhite)
                            .border(1.dp, PlayfulBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = TimeFormatter.formatMillis(lap.lapDurationMillis),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = PlayfulBlack
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Bar Chart with Circular Label at Top of Bars (matching Reference Image 1)
 */
data class BarChartItem(
    val label: String,
    val valueSeconds: Double,
    val color: Color
)

@Composable
fun PlayfulBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxValue = items.maxOfOrNull { it.valueSeconds }?.coerceAtLeast(1.0) ?: 1.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PlayfulWhite)
            .padding(18.dp)
    ) {
        Text(
            text = "PERBANDINGAN RATA-RATA LAP PER SHIFT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = PlayfulGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.take(7).forEach { item ->
                val fraction = (item.valueSeconds / maxValue).toFloat().coerceIn(0.12f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pill label on top of bar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PlayfulBlack)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fs", item.valueSeconds),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // The bar
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((80 * fraction).dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(item.color)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlayfulBlack,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Combobox / Autocomplete Dropdown with type-ahead search AND clickable dropdown toggle.
 * Supports picking from Master OR typing manually when value is not in Master.
 */
@Composable
fun MasterComboboxField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    placeholder: String = "",
    isRequired: Boolean = false,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAllOptions by remember { mutableStateOf(false) }

    val displayedOptions = remember(value, options, showAllOptions) {
        if (showAllOptions || value.isBlank()) {
            options.take(25)
        } else {
            options.filter { it.contains(value, ignoreCase = true) }.take(25)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label + if (isRequired) " *" else "",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isError) PlayfulCoral else PlayfulBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    showAllOptions = false
                    onValueChange(it)
                    expanded = options.any { opt -> opt.contains(it, ignoreCase = true) }
                },
                placeholder = { Text(placeholder, fontSize = 13.sp, color = PlayfulGray, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                isError = isError,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlayfulBlack,
                    unfocusedBorderColor = PlayfulBorder,
                    focusedContainerColor = PlayfulWhite,
                    unfocusedContainerColor = PlayfulWhite
                ),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (value.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onValueChange("")
                                    showAllOptions = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = PlayfulGray, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                showAllOptions = true
                                expanded = !expanded
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih dari Master", tint = PlayfulBlack)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            )

            DropdownMenu(
                expanded = expanded && (displayedOptions.isNotEmpty() || showAllOptions),
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 360.dp)
                    .background(PlayfulWhite)
            ) {
                if (displayedOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Belum ada daftar di Master. Langsung ketik manual pada kolom di atas.",
                                fontSize = 12.sp,
                                color = PlayfulGray
                            )
                        },
                        enabled = false,
                        onClick = {}
                    )
                } else {
                    displayedOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(opt, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = PlayfulBlack)
                            },
                            onClick = {
                                onValueChange(opt)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Atau ketik langsung jika tidak ada di daftar",
                                fontSize = 11.sp,
                                color = PlayfulGray
                            )
                        },
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}

/**
 * Alias that also supports manual input alongside Master dropdown selection
 */
@Composable
fun MasterDropdownField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    placeholder: String = "Pilih dari Master atau ketik manual...",
    isRequired: Boolean = false,
    isError: Boolean = false,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    MasterComboboxField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        options = options,
        placeholder = placeholder,
        isRequired = isRequired,
        isError = isError,
        testTag = testTag,
        modifier = modifier
    )
}

@Composable
fun PlayfulLapItemRow(
    lap: LapEntity,
    isFastest: Boolean = false,
    isSlowest: Boolean = false,
    onEditLabel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlayfulWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lap_item_${lap.lapNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap number pill
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PlayfulBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${lap.lapNumber}",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = PlayfulBlack
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeFormatter.formatMillis(lap.lapDurationMillis),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = when {
                            isFastest -> PlayfulTeal
                            isSlowest -> PlayfulCoral
                            else -> PlayfulBlack
                        }
                    )
                    if (isFastest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PlayfulTealLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Tercepat", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlayfulTeal)
                        }
                    } else if (isSlowest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PlayfulCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Terlambat", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlayfulCoral)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditLabel() }
                ) {
                    Text(
                        text = "Kum: ${TimeFormatter.formatMillis(lap.cumulativeMillis)}",
                        fontSize = 11.sp,
                        color = PlayfulGray
                    )
                    if (lap.label.isNotBlank()) {
                        Text(
                            text = " • \"${lap.label}\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayfulBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Text(
                            text = " • (+ Label)",
                            fontSize = 10.sp,
                            color = PlayfulGray.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(onClick = onEditLabel, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit label", tint = PlayfulGray, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus lap", tint = PlayfulCoral, modifier = Modifier.size(18.dp))
            }
        }
    }
}

