package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MachoMuscleV3Guide
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.ceil

private enum class PlaybookSection(val label: String) {
    OVERVIEW("Roadmap & Targets"),
    RECOVERY("Fri–Sat Rest"),
    DIET("Lean Recomp Diet"),
    SCAN_DAY("Dec 31 Scan")
}

@Composable
fun PlaybookScreen(
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableStateOf(PlaybookSection.OVERVIEW) }
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("macho_muscle_v3_prefs", Context.MODE_PRIVATE)
    }

    val daysToScan = remember {
        val targetCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh")).apply {
            set(2026, Calendar.DECEMBER, 31, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - System.currentTimeMillis()
        ceil(diffMs / 86_400_000.0).toInt()
    }

    val checklistStates = remember {
        mutableStateListOf<Boolean>().apply {
            MachoMuscleV3Guide.scanDayChecklist.indices.forEach { idx ->
                add(prefs.getBoolean("scan_chk_$idx", false))
            }
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
                PlaybookHeroCard(daysToScan = daysToScan)
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PlaybookSection.entries, key = { it.name }) { section ->
                        val active = section == selectedSection
                        Surface(
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedSection = section }
                                .testTag("playbook_tab_${section.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            when (selectedSection) {
                PlaybookSection.OVERVIEW -> {
                    item {
                        InsightGridCards()
                    }
                    item {
                        SectionTitle("5.5-Week Roadmap", "22 NOV → 31 DEC")
                    }
                    items(MachoMuscleV3Guide.roadmap, key = { it.week }) { week ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (week.highlight) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${week.week} • ${week.dates}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (week.highlight) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = week.effort,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = week.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        SectionTitle("Dec 31 Scan Targets", "19 NOV VS TARGET")
                    }
                    items(MachoMuscleV3Guide.scanTargets, key = { it.metric }) { row ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.metric,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = row.baselineNov19,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = row.targetDec31,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                        }
                    }
                }

                PlaybookSection.RECOVERY -> {
                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.DirectionsWalk,
                            accent = MaterialTheme.colorScheme.primary,
                            title = "Friday — Active Rest",
                            bullets = listOf(
                                "10,000 steps — a 40-min Corniche walk covers most of it.",
                                "10-min mobility: run the leg stretch + back stretch routines.",
                                "Eat at rest-day calories (~2,050 kcal) while keeping protein at 160 g."
                            )
                        )
                    }
                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.Bedtime,
                            accent = MaterialTheme.colorScheme.secondary,
                            title = "Saturday — Full Rest & Prep",
                            bullets = listOf(
                                "8,000 steps, no gym training.",
                                "Meal-prep for Sun–Tue: cook chicken, boil eggs, make sundal and sambar.",
                                "Get to bed early so Sunday's heavy Upper Strength session starts fresh."
                            )
                        )
                    }
                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.TrackChanges,
                            accent = MaterialTheme.colorScheme.tertiary,
                            title = "Sleep & Daily Steps",
                            bullets = listOf(
                                "Sleep 7–8 hours every night. Short sleep during fat loss costs muscle.",
                                "Hit 8,000–10,000 steps daily (even on training days) instead of extra HIIT."
                            )
                        )
                    }
                }

                PlaybookSection.DIET -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalorieTargetCard(
                                label = "TRAINING DAYS (SUN–THU)",
                                kcal = "~2,300 kcal",
                                macros = "P 160g · C ~270g · F ~65g",
                                modifier = Modifier.weight(1f)
                            )
                            CalorieTargetCard(
                                label = "REST DAYS (FRI–SAT)",
                                kcal = "~2,050 kcal",
                                macros = "P 160g · C ~195g · F ~70g",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        SectionTitle("Low-GI Tamil Menu", "≤ 10 SAR / MEAL · KHOBAR")
                    }
                    items(MachoMuscleV3Guide.meals, key = { it.slot }) { meal ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (meal.timing != null) {
                                            "${meal.slot} (${meal.timing})"
                                        } else {
                                            meal.slot
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = meal.macros,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = meal.food,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.Restaurant,
                            accent = MaterialTheme.colorScheme.tertiary,
                            title = "5.5-Week Diet Rules",
                            bullets = listOf(
                                "30–45 g protein at every main meal (4–5 feeds/day).",
                                "Hydration: 3–3.5 L water daily (BIA scans track hydration).",
                                "Half your plate vegetables (poriyal, keerai, salad).",
                                "Limit parotta, biryani, fried snacks to max 1 meal/week."
                            )
                        )
                    }
                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.Medication,
                            accent = MaterialTheme.colorScheme.secondary,
                            title = "Supplements (Optional)",
                            bullets = listOf(
                                "Whey protein: 1–2 scoops/day only to hit 160 g protein.",
                                "Creatine monohydrate 3–5 g/day: pulls water into muscle (+0.5–1 kg on scan). Start now and keep consistent through Dec 31."
                            )
                        )
                    }
                }

                PlaybookSection.SCAN_DAY -> {
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
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Checklist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Thu 31 Dec Scan Protocol",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Control hydration, timing, and rest so the Dec 31 Boditrax scan compares cleanly against 19 Nov.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(MachoMuscleV3Guide.scanDayChecklist) { index, itemText ->
                        val checked = checklistStates.getOrElse(index) { false }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    if (checked) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    val next = !checked
                                    checklistStates[index] = next
                                    prefs.edit().putBoolean("scan_chk_$index", next).apply()
                                }
                                .testTag("scan_checklist_item_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { next ->
                                        checklistStates[index] = next
                                        prefs.edit().putBoolean("scan_chk_$index", next).apply()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.tertiary
                                    )
                                )
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        RecoveryCard(
                            icon = Icons.Rounded.TrackChanges,
                            accent = MaterialTheme.colorScheme.secondary,
                            title = "Friday Morning Weekly Check-In",
                            bullets = listOf(
                                "Fasted morning weight (on track: −0.2 to −0.4 kg/week).",
                                "Waist at belly button (on track: −0.5 cm/week).",
                                "Top set on bench, squat, deadlift, pull-ups + L vs R reps."
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybookHeroCard(daysToScan: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PUREGYM KHOBAR · V3.0 · TARGET: THU 31 DEC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Macho Muscle v3 · Scan Push",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (daysToScan > 0) {
                    "$daysToScan days to Dec 31 scan • 19 Nov baseline: 75.5 kg · 57.9 kg muscle · 19.1% fat"
                } else {
                    "Dec 31 Scan Window Active • Follow the Scan Protocol checklist"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun InsightGridCards() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TRUNK AT ALL-TIME HIGH (32.0 KG)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fat dropped 20.7 → 14.4 kg, visceral 11 → 8, Boditrax 632 → 810. v3 shifts every muscle group to 2x/week to push total muscle past 58.2 kg.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ASYMMETRY PRIORITY: LEFT ARM & LEGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Legs dropped 10.4 → 10.0/10.2 kg and left arm slipped 2.8 → 2.7 kg (vs 3.0 right). Every unilateral lift starts LEFT with +1 extra left set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecoveryCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    bullets: List<String>
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CalorieTargetCard(
    label: String,
    kcal: String,
    macros: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(16.dp)
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
                text = kcal,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = macros,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
