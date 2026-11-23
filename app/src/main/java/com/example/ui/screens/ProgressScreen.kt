package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BodyScan
import com.example.model.UiState
import com.example.model.WorkoutSetLog
import com.example.ui.components.BodyCompositionChart
import com.example.ui.components.BragAchievementDialog
import com.example.ui.components.EditBodyScanDialog
import com.example.ui.components.LogSetDialog
import com.example.ui.components.UserProfileAvatar
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Locale

private data class WorkoutSessionSummary(
    val sessionDate: String,
    val day: String,
    val logs: List<WorkoutSetLog>,
    val totalVolumeKg: Double
)

@Composable
fun ProgressScreen(
    userEmail: String?,
    userDisplayName: String?,
    userPhotoUrl: String?,
    workoutLogsState: UiState<List<WorkoutSetLog>>,
    bodyScansState: UiState<List<BodyScan>>,
    onUpdateSetLog: (logId: String, weightKg: Double, reps: Int, rpe: Int, notes: String?) -> Unit,
    onDeleteSetLog: (logId: String) -> Unit,
    onUpdateBodyScan: (scanId: String, weightText: String, muscleText: String, fatText: String, observations: String) -> Unit,
    onDeleteBodyScan: (scanId: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs = (workoutLogsState as? UiState.Success)?.data.orEmpty()
    val scans = (bodyScansState as? UiState.Success)?.data.orEmpty()

    var editingSetLog by remember { mutableStateOf<WorkoutSetLog?>(null) }
    var editingScan by remember { mutableStateOf<BodyScan?>(null) }
    var showBragDialog by remember { mutableStateOf(false) }

    val completedLogs = remember(logs) { logs.filter { it.completed } }
    val totalVolumeKg = remember(completedLogs) {
        completedLogs.sumOf { it.weightKg * it.reps }
    }
    val sessionSummaries = remember(logs) {
        logs.groupBy { it.sessionDate to it.day }
            .entries
            .sortedByDescending { it.key.first }
            .map { (key, sessionLogs) ->
                WorkoutSessionSummary(
                    sessionDate = key.first,
                    day = key.second,
                    logs = sessionLogs.sortedBy { it.setNumber },
                    totalVolumeKg = sessionLogs.filter { it.completed }
                        .sumOf { it.weightKg * it.reps }
                )
            }
    }

    val latestScan = scans.firstOrNull()
    val muscleToFatRatio = remember(latestScan) {
        if (latestScan != null && latestScan.bodyFatPercent > 0.0) {
            val fatKg = latestScan.weightKg * (latestScan.bodyFatPercent / 100.0)
            if (fatKg > 0.0) latestScan.muscleMassKg / fatKg else null
        } else {
            null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AccountSummaryCard(
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    onSignOut = onSignOut
                )
            }

            item {
                BragBannerCard(
                    sessionsCount = sessionSummaries.size,
                    totalVolumeKg = totalVolumeKg,
                    onBrag = { showBragDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        label = "SESSIONS",
                        value = sessionSummaries.size.toString(),
                        sub = "${completedLogs.size} completed sets",
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        label = "TOTAL VOLUME",
                        value = formatVolume(totalVolumeKg),
                        sub = "kg moved",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        label = "MUSCLE / FAT",
                        value = muscleToFatRatio?.let {
                            String.format(Locale.US, "%.2fx", it)
                        } ?: "--",
                        sub = latestScan?.let { "${String.format(Locale.US, "%.1f", it.muscleMassKg)} kg muscle" }
                            ?: "No scan yet",
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        label = "BODITRAX SCANS",
                        value = scans.size.toString(),
                        sub = latestScan?.let { "${String.format(Locale.US, "%.1f", it.bodyFatPercent)}% fat" }
                            ?: "Log in AI Coach",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                BodyCompositionChart(scans = scans)
            }

            if (workoutLogsState is UiState.Loading || bodyScansState is UiState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (scans.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Boditrax Scan Log",
                        subtitle = "${scans.size} saved checkpoints"
                    )
                }
                items(scans.take(8), key = { "scan_${it.id}" }) { scan ->
                    BodyScanHistoryCard(
                        scan = scan,
                        onEdit = { editingScan = scan },
                        onDelete = { onDeleteBodyScan(scan.id) }
                    )
                }
            }

            item {
                SectionHeading(
                    title = "Workout History",
                    subtitle = "${sessionSummaries.size} logged sessions"
                )
            }

            if (sessionSummaries.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Log working sets from the Training tab to build your session history and volume totals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessionSummaries, key = { "${it.sessionDate}_${it.day}" }) { session ->
                    SessionHistoryCard(
                        session = session,
                        onEditSet = { log -> editingSetLog = log },
                        onDeleteSet = { logId -> onDeleteSetLog(logId) }
                    )
                }
            }
        }
    }

    editingSetLog?.let { targetLog ->
        LogSetDialog(
            exerciseName = targetLog.exerciseName,
            setNumber = targetLog.setNumber.toInt(),
            initialWeightKg = targetLog.weightKg,
            initialReps = targetLog.reps.toInt(),
            initialRpe = targetLog.rpe.toInt(),
            initialNotes = targetLog.notes,
            isEditing = true,
            onDismiss = { editingSetLog = null },
            onConfirm = { weightKg, reps, rpe, notes ->
                editingSetLog = null
                onUpdateSetLog(targetLog.id, weightKg, reps, rpe, notes)
            }
        )
    }

    editingScan?.let { targetScan ->
        EditBodyScanDialog(
            scan = targetScan,
            onDismiss = { editingScan = null },
            onConfirm = { weightText, muscleText, fatText, observations ->
                editingScan = null
                onUpdateBodyScan(targetScan.id, weightText, muscleText, fatText, observations)
            }
        )
    }

    if (showBragDialog) {
        BragAchievementDialog(
            athleteName = userDisplayName?.takeIf { it.isNotBlank() } ?: "Riffayu",
            logs = logs,
            scans = scans,
            onDismiss = { showBragDialog = false }
        )
    }
}

@Composable
private fun AccountSummaryCard(
    userDisplayName: String?,
    userEmail: String?,
    userPhotoUrl: String?,
    onSignOut: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
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
                UserProfileAvatar(
                    photoUrl = userPhotoUrl,
                    displayName = userDisplayName ?: userEmail,
                    size = 48.dp,
                    modifier = Modifier.testTag("progress_profile_avatar")
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = userDisplayName?.takeIf { it.isNotBlank() } ?: "Athlete",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = userEmail ?: "Google Account Synced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun BragBannerCard(
    sessionsCount: Int,
    totalVolumeKg: Double,
    onBrag: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
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
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Flex Your Progress",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${formatVolume(totalVolumeKg)} kg moved across $sessionsCount sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onBrag,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("brag_friends_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Brag", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(18.dp)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BodyScanHistoryCard(
    scan: BodyScan,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(scan.createdAt) {
        scan.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } ?: "Recent Scan"
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f kg • %.1f kg muscle • %.1f%% fat",
                                scan.weightKg,
                                scan.muscleMassKg,
                                scan.bodyFatPercent
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_scan_button_${scan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit body scan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete body scan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (scan.observations.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scan.observations,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: WorkoutSessionSummary,
    onEditSet: (WorkoutSetLog) -> Unit,
    onDeleteSet: (String) -> Unit
) {
    val groupedExercises = remember(session.logs) {
        session.logs.groupBy { it.exerciseName }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${session.day} • ${session.sessionDate}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.logs.size} sets logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${formatVolume(session.totalVolumeKg)} kg",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            groupedExercises.forEach { (exerciseName, exerciseLogs) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    exerciseLogs.forEach { log ->
                        val w = if (log.weightKg % 1.0 == 0.0) {
                            log.weightKg.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.1f", log.weightKg)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Set ${log.setNumber}: ${w}kg × ${log.reps} (RPE ${log.rpe})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = { onEditSet(log) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit set ${log.setNumber} of $exerciseName",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSet(log.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete set ${log.setNumber} of $exerciseName",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatVolume(volumeKg: Double): String {
    return if (volumeKg >= 1000.0) {
        String.format(Locale.US, "%,.0f", volumeKg)
    } else {
        String.format(Locale.US, "%.0f", volumeKg)
    }
}
