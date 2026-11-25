package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["sessionId"], unique = true), Index(value = ["createdAt"])]
)
data class SessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val mode: String, // "TS" (Time Study) or "WLA" (Work Load Analysis)
    val date: String, // YYYY-MM-DD
    val sessionName: String = "Shift 1", // "Shift 1", "Shift 2", "Pagi", dll.
    val operatorName: String = "",
    val observerName: String = "",
    val studyName: String, // Required
    val line: String = "",
    val machine: String = "",
    val building: String = "",
    val notes: String = "",
    val stopwatchCount: Int = 1,
    val lapCount: Int = 0,
    val totalDurationMillis: Long = 0L,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED"
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stopwatch_elements",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["elementId"], unique = true)]
)
data class StopwatchElementEntity(
    @PrimaryKey
    val elementId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val name: String,
    val colorHex: String = "#F6C445",
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val lastStartTimeMillis: Long = 0L,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "laps",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["elementId"]), Index(value = ["lapId"], unique = true)]
)
data class LapEntity(
    @PrimaryKey
    val lapId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val elementId: String,
    val elementName: String,
    val sessionName: String = "Shift 1",
    val lapNumber: Int,
    val lapDurationMillis: Long,
    val cumulativeMillis: Long,
    val label: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)

@Entity(
    tableName = "master_items",
    primaryKeys = ["category", "value"]
)
data class MasterItemEntity(
    val category: String, // "operator", "observer", "studyName", "line", "machine", "building", "session"
    val value: String
)

data class SessionWithDetails(
    val session: SessionEntity,
    val elements: List<StopwatchElementEntity> = emptyList(),
    val laps: List<LapEntity> = emptyList()
)

data class LapStats(
    val count: Int = 0,
    val minDurationMillis: Long = 0L,
    val maxDurationMillis: Long = 0L,
    val avgDurationMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val stdDevMillis: Long = 0L,
    val unitsPerMinute: Double = 0.0
) {
    companion object {
        fun calculate(lapDurationsMillis: List<Long>): LapStats {
            if (lapDurationsMillis.isEmpty()) return LapStats()
            val count = lapDurationsMillis.size
            val total = lapDurationsMillis.sum()
            val min = lapDurationsMillis.minOrNull() ?: 0L
            val max = lapDurationsMillis.maxOrNull() ?: 0L
            val avg = total / count
            val avgSeconds = total.toDouble() / (count * 1000.0)
            val unitsPerMin = if (avgSeconds > 0.0) 60.0 / avgSeconds else 0.0

            val variance = lapDurationsMillis
                .map { val diff = (it - avg).toDouble(); diff * diff }
                .average()
            val stdDev = kotlin.math.sqrt(variance).toLong()

            return LapStats(
                count = count,
                minDurationMillis = min,
                maxDurationMillis = max,
                avgDurationMillis = avg,
                totalDurationMillis = total,
                stdDevMillis = stdDev,
                unitsPerMinute = unitsPerMin
            )
        }
    }
}

data class MasterOptions(
    val operators: List<String> = emptyList(),
    val observers: List<String> = emptyList(),
    val studyNames: List<String> = emptyList(),
    val lines: List<String> = emptyList(),
    val machines: List<String> = emptyList(),
    val buildings: List<String> = emptyList(),
    val sessions: List<String> = listOf("Shift 1", "Shift 2", "Shift 3", "Pagi", "Siang", "Malam")
)

data class SessionSyncItem(
    val sessionId: String,
    val mode: String,
    val date: String,
    val sessionName: String,
    val operatorName: String,
    val observerName: String = "",
    val studyName: String,
    val line: String,
    val machine: String,
    val building: String,
    val notes: String,
    val stopwatchCount: Int,
    val lapCount: Int,
    val totalDurationSeconds: Double,
    val status: String,
    val createdAt: Long
)

data class LapSyncItem(
    val lapId: String,
    val sessionId: String,
    val mode: String,
    val date: String,
    val sessionName: String,
    val operatorName: String,
    val observerName: String = "",
    val studyName: String,
    val line: String,
    val machine: String,
    val building: String,
    val elementName: String,
    val lapNumber: Int,
    val lapDurationSeconds: Double,
    val cumulativeSeconds: Double,
    val label: String,
    val timestampFormatted: String
)
