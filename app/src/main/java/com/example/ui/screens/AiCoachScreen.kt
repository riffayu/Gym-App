package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BodyScan
import com.example.model.GeneratedProgramPreview
import com.example.model.TrainingDay
import com.example.model.UiState
import com.example.ui.AiGenerationState
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    bodyScansState: UiState<List<BodyScan>>,
    aiState: AiGenerationState,
    isMutating: Boolean,
    onGenerateProgram: (weightText: String, muscleText: String, fatText: String, observations: String) -> Unit,
    onSaveScanOnly: (weightText: String, muscleText: String, fatText: String, observations: String) -> Unit,
    onApplyProgram: (GeneratedProgramPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestScan = (bodyScansState as? UiState.Success)?.data?.firstOrNull()

    var weightText by rememberSaveable { mutableStateOf("75.5") }
    var muscleText by rememberSaveable { mutableStateOf("57.9") }
    var fatText by rememberSaveable { mutableStateOf("19.1") }
    var observations by rememberSaveable {
        mutableStateOf("Left arm (2.7 vs 3.0 kg) & legs (10.0/10.2 kg) priority, 2x/wk")
    }
    var initializedFromScan by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(latestScan?.id) {
        if (!initializedFromScan && latestScan != null) {
            weightText = String.format(Locale.US, "%.1f", latestScan.weightKg)
            muscleText = String.format(Locale.US, "%.1f", latestScan.muscleMassKg)
            fatText = String.format(Locale.US, "%.1f", latestScan.bodyFatPercent)
            if (latestScan.observations.isNotBlank()) {
                observations = latestScan.observations
            }
            initializedFromScan = true
        }
    }

    val weightVal = weightText.toDoubleOrNull()
    val muscleVal = muscleText.toDoubleOrNull()
    val fatVal = fatText.toDoubleOrNull()
    val leanMassKg = if (weightVal != null && fatVal != null) {
        weightVal * (1.0 - (fatVal / 100.0))
    } else {
        null
    }
    val muscleSharePercent = if (weightVal != null && weightVal > 0 && muscleVal != null) {
        (muscleVal / weightVal) * 100.0
    } else {
        null
    }

    val focusPresets = listOf(
        "Upper chest & side delts",
        "Lat width & arm thickness",
        "Quad sweep & hamstrings",
        "Recomp & athletic conditioning"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MonitorWeight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "BODITRAX AI COACH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Custom 5-Day Program Architect",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Enter your latest Boditrax scan metrics and weak-point notes. Gemini synthesizes a balanced Sun–Thu split calibrated to your muscle-to-fat ratio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("Weight (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("boditrax_weight_input")
                            )
                            OutlinedTextField(
                                value = muscleText,
                                onValueChange = { muscleText = it },
                                label = { Text("Muscle (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("boditrax_muscle_input")
                            )
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("Body Fat %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("boditrax_fat_input")
                            )
                        }

                        if (leanMassKg != null || muscleSharePercent != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "EST. LEAN MASS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = leanMassKg?.let {
                                                String.format(Locale.US, "%.1f kg", it)
                                            } ?: "--",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "SKELETAL MUSCLE RATIO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = muscleSharePercent?.let {
                                                String.format(Locale.US, "%.1f%%", it)
                                            } ?: "--",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = observations,
                            onValueChange = {
                                if (it.length <= 500) observations = it
                            },
                            label = { Text("Weak Points & Coaching Observations") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("boditrax_observations_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            focusPresets.forEach { preset ->
                                AssistChip(
                                    onClick = { observations = preset },
                                    label = {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSaveScanOnly(weightText, muscleText, fatText, observations)
                                },
                                enabled = aiState !is AiGenerationState.Generating && !isMutating,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .heightIn(min = 52.dp)
                                    .testTag("save_scan_only_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Scan")
                            }

                            Button(
                                onClick = {
                                    onGenerateProgram(weightText, muscleText, fatText, observations)
                                },
                                enabled = aiState !is AiGenerationState.Generating && !isMutating,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .heightIn(min = 52.dp)
                                    .testTag("generate_ai_program_button")
                            ) {
                                if (aiState is AiGenerationState.Generating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Building...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Plan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            when (aiState) {
                is AiGenerationState.Idle -> Unit

                is AiGenerationState.Generating -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Synthesizing Sunday–Thursday split...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Calibrating volume and exercise selection to your Boditrax profile.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                is AiGenerationState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_error_banner")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Program Generation Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is AiGenerationState.Preview -> {
                    item {
                        GeneratedProgramPreviewCard(
                            preview = aiState.program,
                            isMutating = isMutating,
                            onApply = { onApplyProgram(aiState.program) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedProgramPreviewCard(
    preview: GeneratedProgramPreview,
    isMutating: Boolean,
    onApply: () -> Unit
) {
    val groupedByDay = remember(preview.exercises) {
        TrainingDay.entries.map { day ->
            day to preview.exercises.filter { TrainingDay.fromLabel(it.day) == day }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
            .testTag("ai_program_preview_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "READY TO DEPLOY • ${preview.exercises.size} LIFTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preview.programName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preview.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onApply,
                enabled = !isMutating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("apply_ai_program_button")
            ) {
                if (isMutating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving to Cloud Split...")
                } else {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Program to Split", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groupedByDay.forEach { (day, lifts) ->
                    if (lifts.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${day.label.uppercase(Locale.US)} • ${lifts.size} EXERCISES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                lifts.forEachIndexed { idx, lift ->
                                    if (idx > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lift.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${lift.target} — ${lift.instructions}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = lift.targetSets,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
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
