package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BodyScan
import com.example.model.MachoMuscleV3Guide
import com.example.model.PresetBlueprints
import com.example.model.ProgramBlueprint
import com.example.model.RoutineStep
import com.example.model.TrainingDay
import com.example.model.UiState
import com.example.model.WorkoutExercise
import com.example.model.WorkoutSetLog
import com.example.model.buildYoutubeSearchUrl
import com.example.ui.RestTimerState
import com.example.ui.components.BragAchievementDialog
import com.example.ui.components.DaySelectorStrip
import com.example.ui.components.ExerciseEditorDialog
import com.example.ui.components.LogSetDialog
import com.example.ui.components.RestTimerBanner
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun SplitScreen(
    userDisplayName: String?,
    exercisesState: UiState<List<WorkoutExercise>>,
    workoutLogsState: UiState<List<WorkoutSetLog>>,
    bodyScansState: UiState<List<BodyScan>>,
    selectedDay: TrainingDay,
    todaySessionDate: String,
    restTimerState: RestTimerState,
    isMutating: Boolean,
    onSelectDay: (TrainingDay) -> Unit,
    onApplyBlueprint: (ProgramBlueprint) -> Unit,
    onAddExercise: (day: TrainingDay, name: String, target: String, targetSets: String, instructions: String, currentCount: Int, programName: String) -> Unit,
    onUpdateExercise: (exerciseId: String, day: TrainingDay, name: String, target: String, targetSets: String, instructions: String) -> Unit,
    onDeleteExercise: (exerciseId: String, name: String) -> Unit,
    onLogWorkingSet: (exercise: WorkoutExercise, setNumber: Int, weightKg: Double, reps: Int, rpe: Int, notes: String?) -> Unit,
    onUpdateSetLog: (logId: String, weightKg: Double, reps: Int, rpe: Int, notes: String?) -> Unit,
    onToggleSetCompleted: (WorkoutSetLog) -> Unit,
    onDeleteSetLog: (String) -> Unit,
    onAddRest15s: () -> Unit,
    onSkipRest: () -> Unit,
    onNavigateToCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBlueprintDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showBragDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<WorkoutExercise?>(null) }
    var loggingExercise by remember { mutableStateOf<WorkoutExercise?>(null) }
    var editingSetLog by remember { mutableStateOf<WorkoutSetLog?>(null) }
    var warmUpExpanded by remember { mutableStateOf(false) }
    var stretchExpanded by remember { mutableStateOf(false) }
    val expandedExercises = remember { mutableStateMapOf<String, Boolean>() }

    val allExercises = (exercisesState as? UiState.Success)?.data.orEmpty()
    val allLogs = (workoutLogsState as? UiState.Success)?.data.orEmpty()
    val allScans = (bodyScansState as? UiState.Success)?.data.orEmpty()

    val countByDay = remember(allExercises) {
        allExercises.groupingBy { TrainingDay.fromLabel(it.day).label }.eachCount()
    }
    val dayExercises = remember(allExercises, selectedDay) {
        allExercises.filter { TrainingDay.fromLabel(it.day) == selectedDay }
    }
    val allDayExpanded = remember(dayExercises, expandedExercises.toMap()) {
        dayExercises.isNotEmpty() && dayExercises.all { expandedExercises[it.id] == true }
    }
    val activeProgramName = remember(allExercises) {
        allExercises.firstOrNull()?.programName?.takeIf { it.isNotBlank() }
            ?: MachoMuscleV3Guide.PROGRAM_NAME
    }
    val isUsingV3Program = remember(allExercises) {
        allExercises.any { it.programName == MachoMuscleV3Guide.PROGRAM_NAME }
    }
    val dayMeta = MachoMuscleV3Guide.dayRoutines[selectedDay]

    val todayLogsByExercise = remember(allLogs, todaySessionDate) {
        allLogs.filter { it.sessionDate == todaySessionDate }
            .groupBy { it.exerciseId }
            .mapValues { entry -> entry.value.sortedBy { it.setNumber } }
    }
    val latestWeightByExercise = remember(allLogs) {
        allLogs.groupBy { it.exerciseId }
            .mapValues { entry -> entry.value.firstOrNull()?.weightKg }
    }
    val latestRepsByExercise = remember(allLogs) {
        allLogs.groupBy { it.exerciseId }
            .mapValues { entry -> entry.value.firstOrNull()?.reps?.toInt() }
    }

    fun openYoutubeQuery(queryOrUrl: String, isUrl: Boolean = false) {
        val url = if (isUrl) queryOrUrl else buildYoutubeSearchUrl(queryOrUrl)
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SplitHeaderCard(
                    programName = activeProgramName,
                    selectedDay = selectedDay,
                    whyNote = dayMeta?.whyNote,
                    todaySessionDate = todaySessionDate,
                    dayExerciseCount = dayExercises.size,
                    completedSetsToday = todayLogsByExercise.values.flatten().count { it.completed },
                    isMutating = isMutating,
                    onOpenBlueprints = { showBlueprintDialog = true },
                    onAddExercise = { showAddExerciseDialog = true },
                    onBragClick = { showBragDialog = true }
                )
            }

            if (exercisesState is UiState.Success && allExercises.isNotEmpty() && !isUsingV3Program) {
                item {
                    V3UpgradeBannerCard(
                        isMutating = isMutating,
                        onLoadV3 = { onApplyBlueprint(PresetBlueprints.machoMuscleV3) }
                    )
                }
            }

            item {
                DaySelectorStrip(
                    selectedDay = selectedDay,
                    exerciseCountByDay = countByDay,
                    onSelectDay = onSelectDay
                )
            }

            if (restTimerState.isActive) {
                item {
                    RestTimerBanner(
                        timerState = restTimerState,
                        onAdd15s = onAddRest15s,
                        onSkip = onSkipRest
                    )
                }
            }

            if (dayMeta != null) {
                item {
                    RoutineAccordionCard(
                        title = "5-Min Warm-Up",
                        subtitle = "${dayMeta.warmUp.size} drills + ramp-up sets",
                        icon = Icons.Rounded.LocalFireDepartment,
                        accentColor = MaterialTheme.colorScheme.primary,
                        steps = dayMeta.warmUp,
                        expanded = warmUpExpanded,
                        onToggle = { warmUpExpanded = !warmUpExpanded },
                        onOpenVideo = { stepName -> openYoutubeQuery(stepName) },
                        testTag = "warmup_accordion"
                    )
                }
            }

            when (exercisesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = exercisesState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (allExercises.isEmpty()) {
                        item {
                            EmptySplitOnboardingCard(
                                onLoadMachoV3 = { onApplyBlueprint(PresetBlueprints.machoMuscleV3) },
                                onChooseBlueprint = { showBlueprintDialog = true },
                                onGenerateWithAi = onNavigateToCoach,
                                onAddManualLift = { showAddExerciseDialog = true }
                            )
                        }
                    } else if (dayExercises.isEmpty()) {
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
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No lifts scheduled for ${selectedDay.label}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${selectedDay.splitTitle} • ${selectedDay.focusHint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { showAddExerciseDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add ${selectedDay.shortLabel} Lift")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Main Workout (${dayExercises.size} lifts)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(
                                    onClick = {
                                        val targetState = !allDayExpanded
                                        dayExercises.forEach { ex ->
                                            expandedExercises[ex.id] = targetState
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("toggle_all_exercises_button")
                                ) {
                                    Text(if (allDayExpanded) "Collapse all" else "Expand all")
                                }
                            }
                        }

                        itemsIndexed(dayExercises, key = { _, ex -> ex.id }) { index, exercise ->
                            val setsToday = todayLogsByExercise[exercise.id].orEmpty()
                            val isExpanded = expandedExercises[exercise.id] ?: false
                            ExerciseCard(
                                index = index + 1,
                                exercise = exercise,
                                todayLogs = setsToday,
                                expanded = isExpanded,
                                onToggleExpand = {
                                    expandedExercises[exercise.id] = !isExpanded
                                },
                                onLogSet = { loggingExercise = exercise },
                                onEditExercise = { editingExercise = exercise },
                                onDeleteExercise = { onDeleteExercise(exercise.id, exercise.name) },
                                onEditSetLog = { setLog -> editingSetLog = setLog },
                                onToggleSetCompleted = onToggleSetCompleted,
                                onDeleteSetLog = onDeleteSetLog,
                                onOpenYoutube = {
                                    val url = exercise.youtubeUrl.ifBlank {
                                        buildYoutubeSearchUrl(exercise.name)
                                    }
                                    openYoutubeQuery(url, isUrl = true)
                                }
                            )
                        }
                    }
                }
            }

            if (dayMeta != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "POST-WORKOUT CARDIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = dayMeta.cardioNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    RoutineAccordionCard(
                        title = "5-Min Post-Workout Stretch",
                        subtitle = "5 min mobility — don't skip",
                        icon = Icons.Rounded.SelfImprovement,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        steps = dayMeta.stretch,
                        expanded = stretchExpanded,
                        onToggle = { stretchExpanded = !stretchExpanded },
                        onOpenVideo = { stepName -> openYoutubeQuery(stepName) },
                        testTag = "stretch_accordion"
                    )
                }
            }
        }
    }

    if (showBlueprintDialog) {
        BlueprintPickerDialog(
            onDismiss = { showBlueprintDialog = false },
            onSelect = { blueprint ->
                showBlueprintDialog = false
                onApplyBlueprint(blueprint)
            }
        )
    }

    if (showAddExerciseDialog) {
        ExerciseEditorDialog(
            day = selectedDay,
            existingExercise = null,
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { chosenDay, name, target, targetSets, instructions ->
                showAddExerciseDialog = false
                onAddExercise(
                    chosenDay,
                    name,
                    target,
                    targetSets,
                    instructions,
                    dayExercises.size,
                    activeProgramName
                )
            }
        )
    }

    editingExercise?.let { targetExercise ->
        ExerciseEditorDialog(
            day = selectedDay,
            existingExercise = targetExercise,
            onDismiss = { editingExercise = null },
            onConfirm = { chosenDay, name, target, targetSets, instructions ->
                editingExercise = null
                onUpdateExercise(
                    targetExercise.id,
                    chosenDay,
                    name,
                    target,
                    targetSets,
                    instructions
                )
            }
        )
    }

    loggingExercise?.let { targetExercise ->
        val currentLogs = todayLogsByExercise[targetExercise.id].orEmpty()
        val nextSetNumber = (currentLogs.maxOfOrNull { it.setNumber.toInt() } ?: 0) + 1
        val prefillWeight = currentLogs.lastOrNull()?.weightKg
            ?: latestWeightByExercise[targetExercise.id]
        val prefillReps = currentLogs.lastOrNull()?.reps?.toInt()
            ?: latestRepsByExercise[targetExercise.id]

        LogSetDialog(
            exerciseName = targetExercise.name,
            setNumber = nextSetNumber,
            initialWeightKg = prefillWeight,
            initialReps = prefillReps,
            isEditing = false,
            onDismiss = { loggingExercise = null },
            onConfirm = { weightKg, reps, rpe, notes ->
                expandedExercises[targetExercise.id] = true
                loggingExercise = null
                onLogWorkingSet(targetExercise, nextSetNumber, weightKg, reps, rpe, notes)
            }
        )
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

    if (showBragDialog) {
        BragAchievementDialog(
            athleteName = userDisplayName?.takeIf { it.isNotBlank() } ?: "Riffayu",
            logs = allLogs,
            scans = allScans,
            onDismiss = { showBragDialog = false }
        )
    }
}

@Composable
private fun V3UpgradeBannerCard(
    isMutating: Boolean,
    onLoadV3: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Macho Muscle v3 Available",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Dec 31 Scan Push · 2x/wk + Left-First unilateral fixes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLoadV3,
                enabled = !isMutating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("upgrade_v3_button")
            ) {
                Text("Load v3", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SplitHeaderCard(
    programName: String,
    selectedDay: TrainingDay,
    whyNote: String?,
    todaySessionDate: String,
    dayExerciseCount: Int,
    completedSetsToday: Int,
    isMutating: Boolean,
    onOpenBlueprints: () -> Unit,
    onAddExercise: () -> Unit,
    onBragClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = programName.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${selectedDay.label} • ${selectedDay.splitTitle}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedDay.focusHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (!whyNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "↳ $whyNote",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Session $todaySessionDate • $dayExerciseCount lifts • $completedSetsToday sets logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMutating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onBragClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("split_brag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Brag to friends",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenBlueprints,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_blueprints_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Blueprints", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onAddExercise,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("add_exercise_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Lift", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RoutineAccordionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    steps: List<RoutineStep>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenVideo: (String) -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(testTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = step.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = step.duration,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onOpenVideo(step.name) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircleOutline,
                                        contentDescription = "Watch ${step.name}",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
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

@Composable
private fun EmptySplitOnboardingCard(
    onLoadMachoV3: () -> Unit,
    onChooseBlueprint: () -> Unit,
    onGenerateWithAi: () -> Unit,
    onAddManualLift: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Start Macho Muscle v3",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Load your Dec 31 Scan Push split (Upper, Lower, Push, Pull+Arms, Legs with LEFT-FIRST symmetry work), choose another blueprint, or generate with AI Coach.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLoadMachoV3,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("empty_load_blueprint_button")
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Macho Muscle v3 Split", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChooseBlueprint,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presets", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onGenerateWithAi,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("empty_ai_coach_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Coach", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddManualLift,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("empty_add_manual_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom", maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    index: Int,
    exercise: WorkoutExercise,
    todayLogs: List<WorkoutSetLog>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onLogSet: () -> Unit,
    onEditExercise: () -> Unit,
    onDeleteExercise: () -> Unit,
    onEditSetLog: (WorkoutSetLog) -> Unit,
    onToggleSetCompleted: (WorkoutSetLog) -> Unit,
    onDeleteSetLog: (String) -> Unit,
    onOpenYoutube: () -> Unit
) {
    val parsedMeta = remember(exercise.instructions) {
        val raw = exercise.instructions
        val isCompound = raw.contains("[COMPOUND]")
        val isLeftFirst = raw.contains("[LEFT FIRST]")
        val isNew = raw.contains("[NEW]")
        val stripped = raw
            .replace("[COMPOUND]", "")
            .replace("[LEFT FIRST]", "")
            .replace("[NEW]", "")
            .trim()
        val progSplit = stripped.split("Progression:", limit = 2)
        val mainCue = progSplit.firstOrNull()?.trim().orEmpty()
        val progressionCue = progSplit.getOrNull(1)?.trim()
        Triple(Triple(isCompound, isLeftFirst, isNew), mainCue, progressionCue)
    }
    val (tags, mainCue, progressionCue) = parsedMeta

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (expanded) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                RoundedCornerShape(18.dp)
            )
            .testTag("exercise_card_${exercise.id}")
    ) {
        Column {
            // Dropdown Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (todayLogs.isNotEmpty()) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", index),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (todayLogs.isNotEmpty()) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (expanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = exercise.targetSets,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• ${exercise.target}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todayLogs.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${todayLogs.size} set${if (todayLogs.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exercise_dropdown_${exercise.id}")
                    ) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                            contentDescription = if (expanded) {
                                "Collapse ${exercise.name}"
                            } else {
                                "Expand ${exercise.name}"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable Exercise Details & Set Logger
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tags.first || tags.second || tags.third) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (tags.first) {
                                    ExerciseTagChip("COMPOUND", MaterialTheme.colorScheme.primary)
                                }
                                if (tags.second) {
                                    ExerciseTagChip("LEFT FIRST", MaterialTheme.colorScheme.secondary)
                                }
                                if (tags.third) {
                                    ExerciseTagChip("NEW", MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row {
                            IconButton(
                                onClick = onEditExercise,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("edit_exercise_button_${exercise.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit ${exercise.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteExercise,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${exercise.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    if (mainCue.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mainCue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!progressionCue.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Progression: $progressionCue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (todayLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            todayLogs.forEach { setLog ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            IconButton(
                                                onClick = { onToggleSetCompleted(setLog) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (setLog.completed) {
                                                        Icons.Rounded.CheckCircle
                                                    } else {
                                                        Icons.Rounded.RadioButtonUnchecked
                                                    },
                                                    contentDescription = if (setLog.completed) {
                                                        "Set ${setLog.setNumber} completed"
                                                    } else {
                                                        "Mark set ${setLog.setNumber} completed"
                                                    },
                                                    tint = if (setLog.completed) {
                                                        MaterialTheme.colorScheme.tertiary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            Column {
                                                val wStr = if (setLog.weightKg % 1.0 == 0.0) {
                                                    setLog.weightKg.toInt().toString()
                                                } else {
                                                    String.format(Locale.US, "%.1f", setLog.weightKg)
                                                }
                                                Text(
                                                    text = "SET ${setLog.setNumber} • $wStr kg × ${setLog.reps}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontFamily = JetBrainsMonoFamily
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val meta = buildString {
                                                    append("RPE ${setLog.rpe}")
                                                    if (!setLog.notes.isNullOrBlank()) {
                                                        append(" • ${setLog.notes}")
                                                    }
                                                }
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { onEditSetLog(setLog) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("edit_set_button_${setLog.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = "Edit set ${setLog.setNumber}",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteSetLog(setLog.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete logged set ${setLog.setNumber}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenYoutube,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Form", maxLines = 1)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Button(
                            onClick = onLogSet,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("log_set_button_${exercise.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Set #${todayLogs.size + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseTagChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BlueprintPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (ProgramBlueprint) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Training Blueprint",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Applying a blueprint replaces your current Sunday–Thursday split exercises while preserving your logged sets and scans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PresetBlueprints.all.forEach { blueprint ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(blueprint) }
                            .testTag("blueprint_option_${blueprint.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = blueprint.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = blueprint.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = blueprint.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${blueprint.exercises.size} lifts across Sunday–Thursday",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Close")
            }
        }
    )
}
