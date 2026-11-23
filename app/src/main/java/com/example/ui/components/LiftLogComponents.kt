package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.BodyScan
import com.example.model.MachoMuscleV3Guide
import com.example.model.TrainingDay
import com.example.model.WorkoutExercise
import com.example.model.WorkoutSetLog
import com.example.ui.RestTimerState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.AchievementBragCardData
import com.example.util.AchievementShareHelper
import java.util.Locale

@Composable
fun UserProfileAvatar(
    photoUrl: String?,
    displayName: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = remember(displayName) {
        displayName?.trim()
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotEmpty() }
            ?.take(2)
            ?.joinToString("") { it.first().uppercaseChar().toString() }
            ?.takeIf { it.isNotEmpty() }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (initials != null) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.55f)
            )
        }

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = displayName?.let { "$it profile photo" } ?: "User profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun DaySelectorStrip(
    selectedDay: TrainingDay,
    exerciseCountByDay: Map<String, Int>,
    onSelectDay: (TrainingDay) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TrainingDay.entries, key = { it.name }) { day ->
            val isSelected = day == selectedDay
            val count = exerciseCountByDay[day.label] ?: 0
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectDay(day) }
                    .testTag("day_tab_${day.shortLabel.lowercase()}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (count > 0) "$count lifts" else "Rest",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            Color.White.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RestTimerBanner(
    timerState: RestTimerState,
    onAdd15s: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = timerState.isActive) {
        val progress = if (timerState.totalSeconds > 0) {
            timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
        } else {
            0f
        }
        val minutes = timerState.remainingSeconds / 60
        val seconds = timerState.remainingSeconds % 60
        val formattedTime = String.format(Locale.US, "%d:%02d", minutes, seconds)

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    RoundedCornerShape(18.dp)
                )
                .testTag("rest_timer_banner")
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = "Rest timer active",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "REST RECOVERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onAdd15s,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("rest_timer_add_15s"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("15s", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("rest_timer_skip")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss rest timer",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogSetDialog(
    exerciseName: String,
    setNumber: Int,
    initialWeightKg: Double?,
    initialReps: Int?,
    initialRpe: Int = 8,
    initialNotes: String? = null,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double, reps: Int, rpe: Int, notes: String?) -> Unit
) {
    var weightInput by remember(exerciseName, setNumber, initialWeightKg, isEditing) {
        mutableStateOf(
            if (initialWeightKg != null && initialWeightKg >= 0.0) {
                if (initialWeightKg % 1.0 == 0.0) initialWeightKg.toInt().toString()
                else String.format(Locale.US, "%.1f", initialWeightKg)
            } else {
                "60"
            }
        )
    }
    var repsInput by remember(exerciseName, setNumber, initialReps, isEditing) {
        mutableStateOf((initialReps ?: 8).toString())
    }
    var selectedRpe by remember(exerciseName, setNumber, initialRpe, isEditing) {
        mutableIntStateOf(initialRpe.coerceIn(6, 10))
    }
    var notesInput by remember(exerciseName, setNumber, initialNotes, isEditing) {
        mutableStateOf(initialNotes.orEmpty())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isEditing) "Edit Set #$setNumber" else "Log Set #$setNumber",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = {
                            weightInput = it
                            validationError = null
                        },
                        label = { Text("Load (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("log_set_weight_input")
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = {
                            repsInput = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Reps") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("log_set_reps_input")
                    )
                }

                Column {
                    Text(
                        text = "RPE Intensity: $selectedRpe / 10",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6, 7, 8, 9, 10).forEach { rpeOption ->
                            FilterChip(
                                selected = selectedRpe == rpeOption,
                                onClick = { selectedRpe = rpeOption },
                                label = {
                                    Text(
                                        text = "RPE $rpeOption",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("rpe_chip_$rpeOption")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = {
                        if (it.length <= 300) notesInput = it
                    },
                    label = { Text("Set notes (optional)") },
                    placeholder = { Text("Paused bottom, felt strong...") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_set_notes_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightInput.toDoubleOrNull()
                    val reps = repsInput.toIntOrNull()
                    when {
                        weight == null || weight < 0.0 || weight > 1000.0 -> {
                            validationError = "Enter weight between 0 and 1000 kg."
                        }
                        reps == null || reps !in 1..200 -> {
                            validationError = "Enter reps between 1 and 200."
                        }
                        else -> {
                            onConfirm(
                                weight,
                                reps,
                                selectedRpe,
                                notesInput.trim().takeIf { it.isNotEmpty() }
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_log_set_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEditing) "Update Set" else "Save Set")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorDialog(
    day: TrainingDay,
    existingExercise: WorkoutExercise?,
    onDismiss: () -> Unit,
    onConfirm: (selectedDay: TrainingDay, name: String, target: String, targetSets: String, instructions: String) -> Unit
) {
    var selectedDay by remember(existingExercise, day) {
        mutableStateOf(
            existingExercise?.let { TrainingDay.fromLabel(it.day) } ?: day
        )
    }
    var name by remember(existingExercise) { mutableStateOf(existingExercise?.name ?: "") }
    var target by remember(existingExercise) { mutableStateOf(existingExercise?.target ?: "") }
    var targetSets by remember(existingExercise) {
        mutableStateOf(existingExercise?.targetSets ?: "4 × 8–10")
    }
    var instructions by remember(existingExercise) {
        mutableStateOf(existingExercise?.instructions ?: "")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingExercise == null) {
                    "Add ${selectedDay.label} Exercise"
                } else {
                    "Edit Exercise"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = "Training Day",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TrainingDay.entries.forEach { d ->
                            FilterChip(
                                selected = selectedDay == d,
                                onClick = { selectedDay = d },
                                label = { Text(d.shortLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 100) {
                            name = it
                            validationError = null
                        }
                    },
                    label = { Text("Exercise Name") },
                    placeholder = { Text("e.g. Incline Dumbbell Press") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_name_input")
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = {
                        if (it.length <= 80) {
                            target = it
                            validationError = null
                        }
                    },
                    label = { Text("Target Muscle Group") },
                    placeholder = { Text("e.g. Upper Chest & Front Delts") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_target_input")
                )
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = {
                        if (it.length <= 40) {
                            targetSets = it
                            validationError = null
                        }
                    },
                    label = { Text("Sets × Reps") },
                    placeholder = { Text("e.g. 4 × 8–10") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_sets_input")
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = {
                        if (it.length <= 1000) {
                            instructions = it
                        }
                    },
                    label = { Text("Execution Cues (optional)") },
                    placeholder = { Text("2-second eccentric, pause cleanly on stretch") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_instructions_input")
                )
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanTarget = target.trim()
                    val cleanSets = targetSets.trim()
                    when {
                        cleanName.isEmpty() -> validationError = "Exercise name is required."
                        cleanTarget.isEmpty() -> validationError = "Target muscle is required."
                        cleanSets.isEmpty() -> validationError = "Sets and reps are required."
                        else -> onConfirm(selectedDay, cleanName, cleanTarget, cleanSets, instructions.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_exercise_button")
            ) {
                Text(if (existingExercise == null) "Add Lift" else "Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditBodyScanDialog(
    scan: BodyScan,
    onDismiss: () -> Unit,
    onConfirm: (weightText: String, muscleText: String, fatText: String, observations: String) -> Unit
) {
    var weightText by remember(scan.id) {
        mutableStateOf(String.format(Locale.US, "%.1f", scan.weightKg))
    }
    var muscleText by remember(scan.id) {
        mutableStateOf(String.format(Locale.US, "%.1f", scan.muscleMassKg))
    }
    var fatText by remember(scan.id) {
        mutableStateOf(String.format(Locale.US, "%.1f", scan.bodyFatPercent))
    }
    var observations by remember(scan.id) { mutableStateOf(scan.observations) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Boditrax Scan",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            validationError = null
                        },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_scan_weight_input")
                    )
                    OutlinedTextField(
                        value = muscleText,
                        onValueChange = {
                            muscleText = it
                            validationError = null
                        },
                        label = { Text("Muscle (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_scan_muscle_input")
                    )
                }
                OutlinedTextField(
                    value = fatText,
                    onValueChange = {
                        fatText = it
                        validationError = null
                    },
                    label = { Text("Body Fat %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_scan_fat_input")
                )
                OutlinedTextField(
                    value = observations,
                    onValueChange = {
                        if (it.length <= 500) observations = it
                    },
                    label = { Text("Observations / Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_scan_observations_input")
                )
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toDoubleOrNull()
                    val m = muscleText.toDoubleOrNull()
                    val f = fatText.toDoubleOrNull()
                    when {
                        w == null || w <= 0.0 || w > 500.0 ->
                            validationError = "Enter weight between 1 and 500 kg."
                        m == null || m <= 0.0 || m > 300.0 ->
                            validationError = "Enter muscle mass between 1 and 300 kg."
                        f == null || f < 1.0 || f > 80.0 ->
                            validationError = "Enter body fat between 1% and 80%."
                        else -> onConfirm(weightText, muscleText, fatText, observations)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_edit_scan_button")
            ) {
                Text("Update Scan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

private enum class BragMode(val label: String) {
    WORKOUT_GRIND("Volume & Grind"),
    BODY_SCAN("Boditrax Recomp"),
    BEST_LIFT("Top Lift PR")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BragAchievementDialog(
    athleteName: String,
    logs: List<WorkoutSetLog>,
    scans: List<BodyScan>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(BragMode.WORKOUT_GRIND) }

    val completedLogs = remember(logs) { logs.filter { it.completed } }
    val totalVolumeKg = remember(completedLogs) {
        completedLogs.sumOf { it.weightKg * it.reps }
    }
    val totalSessions = remember(logs) {
        logs.map { "${it.sessionDate}_${it.day}" }.distinct().size
    }
    val topSet = remember(completedLogs) {
        completedLogs.maxByOrNull { it.weightKg * (1.0 + it.reps / 30.0) }
    }
    val latestScan = scans.firstOrNull()
    val firstScan = scans.lastOrNull()

    val cardData = remember(selectedMode, athleteName, completedLogs, totalVolumeKg, totalSessions, topSet, latestScan, firstScan) {
        when (selectedMode) {
            BragMode.WORKOUT_GRIND -> {
                val volFormatted = String.format(Locale.US, "%,.0f kg", totalVolumeKg)
                AchievementBragCardData(
                    categoryTitle = "Training Grind",
                    headline = "$totalSessions Sessions Crushed",
                    athleteName = athleteName,
                    programName = MachoMuscleV3Guide.PROGRAM_NAME,
                    stat1Label = "Total Volume Moved",
                    stat1Value = volFormatted,
                    stat2Label = "Completed Working Sets",
                    stat2Value = "${completedLogs.size} sets",
                    stat3Label = "Training Consistency",
                    stat3Value = "$totalSessions days logged",
                    shareMessage = "Just logged $totalSessions training sessions ($volFormatted total volume across ${completedLogs.size} sets) on LiftLog — Macho Muscle v3! 💪🔥"
                )
            }

            BragMode.BODY_SCAN -> {
                val weightVal = latestScan?.weightKg ?: 75.5
                val muscleVal = latestScan?.muscleMassKg ?: 57.9
                val fatVal = latestScan?.bodyFatPercent ?: 19.1
                val fatDelta = if (latestScan != null && firstScan != null && scans.size > 1) {
                    latestScan.bodyFatPercent - firstScan.bodyFatPercent
                } else {
                    null
                }
                val headline = if (fatDelta != null && fatDelta < 0) {
                    String.format(Locale.US, "Down %.1f%% Body Fat", -fatDelta)
                } else {
                    String.format(Locale.US, "%.1f kg Muscle Mass", muscleVal)
                }
                AchievementBragCardData(
                    categoryTitle = "Boditrax Recomp",
                    headline = headline,
                    athleteName = athleteName,
                    programName = "Dec 31 Scan Push • PureGym Khobar",
                    stat1Label = "Skeletal Muscle Mass",
                    stat1Value = String.format(Locale.US, "%.1f kg", muscleVal),
                    stat2Label = "Body Fat %",
                    stat2Value = String.format(Locale.US, "%.1f%%", fatVal),
                    stat3Label = "Body Weight",
                    stat3Value = String.format(Locale.US, "%.1f kg", weightVal),
                    shareMessage = "Boditrax check-in on LiftLog: ${String.format(Locale.US, "%.1f", weightVal)} kg | ${String.format(Locale.US, "%.1f", muscleVal)} kg muscle | ${String.format(Locale.US, "%.1f", fatVal)}% body fat. Dec 31 Scan Push in motion! 📈🔥"
                )
            }

            BragMode.BEST_LIFT -> {
                val liftName = topSet?.exerciseName ?: "Barbell Bench Press"
                val w = topSet?.weightKg ?: 0.0
                val r = topSet?.reps ?: 0L
                val rpe = topSet?.rpe ?: 8L
                val wLabel = if (w % 1.0 == 0.0) "${w.toInt()} kg" else String.format(Locale.US, "%.1f kg", w)
                val est1Rm = if (w > 0.0 && r > 0L) w * (1.0 + r / 30.0) else 0.0
                AchievementBragCardData(
                    categoryTitle = "Top Lift Milestone",
                    headline = liftName,
                    athleteName = athleteName,
                    programName = MachoMuscleV3Guide.PROGRAM_NAME,
                    stat1Label = "Top Working Set",
                    stat1Value = if (topSet != null) "$wLabel × $r" else "Log sets to track PR",
                    stat2Label = "Estimated 1RM",
                    stat2Value = if (est1Rm > 0.0) String.format(Locale.US, "%.1f kg", est1Rm) else "--",
                    stat3Label = "Intensity",
                    stat3Value = if (topSet != null) "RPE $rpe / 10" else "Ready",
                    shareMessage = if (topSet != null) {
                        "New top set on LiftLog: $liftName — $wLabel × $r reps (est. 1RM ${String.format(Locale.US, "%.1f kg", est1Rm)})! 🏋️🔥"
                    } else {
                        "Training on Macho Muscle v3 in LiftLog! 💪🔥"
                    }
                )
            }
        }
    }

    var customCaption by remember(cardData) { mutableStateOf(cardData.shareMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Brag to Friends", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BragMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("brag_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                // Visual Card Preview
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cardData.categoryTitle.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cardData.headline,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cardData.programName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PreviewStatPill(cardData.stat1Label, cardData.stat1Value)
                            PreviewStatPill(cardData.stat2Label, cardData.stat2Value)
                            PreviewStatPill(cardData.stat3Label, cardData.stat3Value)
                        }
                    }
                }

                OutlinedTextField(
                    value = customCaption,
                    onValueChange = { customCaption = it },
                    label = { Text("Brag Message") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brag_caption_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPayload = cardData.copy(
                        shareMessage = customCaption.trim().ifBlank { cardData.shareMessage }
                    )
                    AchievementShareHelper.shareAchievement(context, finalPayload)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_brag_share_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Card", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PreviewStatPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun BodyCompositionChart(
    scans: List<BodyScan>,
    modifier: Modifier = Modifier
) {
    val orderedScans = remember(scans) { scans.reversed().takeLast(10) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Body Composition Trend",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last ${orderedScans.size} Boditrax scans",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = primaryColor, label = "Weight")
                    ChartLegendDot(color = tertiaryColor, label = "Muscle")
                    ChartLegendDot(color = secondaryColor, label = "Fat %")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (orderedScans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log a Boditrax scan in AI Coach to track trends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                ) {
                    val padH = 16.dp.toPx()
                    val padV = 14.dp.toPx()
                    val plotW = (size.width - padH * 2).coerceAtLeast(1f)
                    val plotH = (size.height - padV * 2).coerceAtLeast(1f)

                    for (i in 0..3) {
                        val y = padV + plotH * (i / 3f)
                        drawLine(
                            color = gridColor.copy(alpha = 0.6f),
                            start = Offset(padH, y),
                            end = Offset(padH + plotW, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val allValues = orderedScans.flatMap {
                        listOf(it.weightKg, it.muscleMassKg, it.bodyFatPercent)
                    }
                    val minVal = (allValues.minOrNull() ?: 0.0) - 5.0
                    val maxVal = (allValues.maxOrNull() ?: 100.0) + 5.0
                    val span = (maxVal - minVal).coerceAtLeast(1.0)

                    fun pointFor(index: Int, value: Double): Offset {
                        val x = if (orderedScans.size == 1) {
                            padH + plotW / 2f
                        } else {
                            padH + (index.toFloat() / (orderedScans.size - 1).toFloat()) * plotW
                        }
                        val normY = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
                        val y = padV + plotH * (1f - normY)
                        return Offset(x, y)
                    }

                    fun drawMetricLine(values: List<Double>, color: Color, fillArea: Boolean = false) {
                        val points = values.mapIndexed { idx, v -> pointFor(idx, v) }
                        if (points.size >= 2) {
                            val linePath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            if (fillArea) {
                                val fillPath = Path().apply {
                                    addPath(linePath)
                                    lineTo(points.last().x, padV + plotH)
                                    lineTo(points.first().x, padV + plotH)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                                        startY = padV,
                                        endY = padV + plotH
                                    )
                                )
                            }
                            drawPath(
                                path = linePath,
                                color = color,
                                style = Stroke(
                                    width = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        points.forEach { pt ->
                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }

                    drawMetricLine(orderedScans.map { it.weightKg }, primaryColor, fillArea = true)
                    drawMetricLine(orderedScans.map { it.muscleMassKg }, tertiaryColor)
                    drawMetricLine(orderedScans.map { it.bodyFatPercent }, secondaryColor)
                }

                val latest = orderedScans.last()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Latest: ${String.format(Locale.US, "%.1f", latest.weightKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                    Text(
                        text = "Muscle: ${String.format(Locale.US, "%.1f", latest.muscleMassKg)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryColor
                    )
                    Text(
                        text = "Fat: ${String.format(Locale.US, "%.1f", latest.bodyFatPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
