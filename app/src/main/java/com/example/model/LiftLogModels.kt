package com.example.model

import android.net.Uri
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TrainingDay(
    val label: String,
    val shortLabel: String,
    val splitTitle: String,
    val focusHint: String
) {
    SUNDAY("Sunday", "SUN", "Upper — Strength", "Chest + Back heavy · low reps"),
    MONDAY("Monday", "MON", "Lower — Strength", "Squat focus · quads + hamstrings"),
    TUESDAY("Tuesday", "TUE", "Push — Hypertrophy", "Shoulders · chest · triceps"),
    WEDNESDAY("Wednesday", "WED", "Pull + Arms", "Back thickness · LEFT ARM PRIORITY"),
    THURSDAY("Thursday", "THU", "Legs — Hypertrophy", "Unilateral fix · glutes · calves");

    companion object {
        fun fromLabel(raw: String): TrainingDay =
            entries.firstOrNull { it.label.equals(raw.trim(), ignoreCase = true) } ?: SUNDAY
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class WorkoutExercise(
    @DocumentId val id: String = "",
    val userId: String = "",
    val programName: String = "",
    val day: String = TrainingDay.SUNDAY.label,
    val name: String = "",
    val target: String = "",
    val targetSets: String = "",
    val instructions: String = "",
    val youtubeUrl: String = "",
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WorkoutSetLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val day: String = TrainingDay.SUNDAY.label,
    val target: String = "",
    val sessionDate: String = "",
    val setNumber: Long = 1L,
    val weightKg: Double = 0.0,
    val reps: Long = 8L,
    val rpe: Long = 8L,
    val completed: Boolean = true,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BodyScan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val muscleMassKg: Double = 0.0,
    val bodyFatPercent: Double = 0.0,
    val observations: String = "",
    val aiSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GeneratedExerciseDraft(
    val name: String,
    val day: String,
    val target: String,
    val targetSets: String,
    val instructions: String,
    val youtubeUrl: String
)

data class GeneratedProgramPreview(
    val programName: String,
    val summary: String,
    val exercises: List<GeneratedExerciseDraft>
)

data class ProgramBlueprint(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val exercises: List<GeneratedExerciseDraft>
)

data class RoutineStep(
    val name: String,
    val duration: String
)

data class DayRoutineMeta(
    val day: TrainingDay,
    val whyNote: String,
    val warmUp: List<RoutineStep>,
    val cardioNote: String,
    val stretch: List<RoutineStep>
)

data class RoadmapWeek(
    val week: String,
    val dates: String,
    val effort: String,
    val details: String,
    val highlight: Boolean = false
)

data class ScanTargetMetric(
    val metric: String,
    val baselineNov19: String,
    val targetDec31: String
)

data class DietMeal(
    val slot: String,
    val timing: String?,
    val food: String,
    val macros: String
)

fun buildYoutubeSearchUrl(exerciseName: String): String {
    val cleanName = exerciseName.replace(Regex("\\(.*?\\)"), "").trim()
    val encoded = Uri.encode("$cleanName proper form")
    return "https://www.youtube.com/results?search_query=$encoded"
}

object MachoMuscleV3Guide {
    const val PROGRAM_NAME = "Macho Muscle v3 — Dec 31 Scan Push"

    private val warmChest = listOf(
        RoutineStep("Jumping Jacks", "60 sec"),
        RoutineStep("Arm Circles (both directions)", "30 sec"),
        RoutineStep("Cross-Body Shoulder Swings", "30 sec"),
        RoutineStep("Push-Up (light, slow)", "10 reps"),
        RoutineStep("Band Pull-Apart", "15 reps"),
        RoutineStep("High Knees", "45 sec")
    )
    private val warmBack = listOf(
        RoutineStep("Light Jumping Jacks", "60 sec"),
        RoutineStep("Cat-Cow Back Mobilization", "10 reps"),
        RoutineStep("Hip Hinge (bodyweight)", "10 reps"),
        RoutineStep("Scapula Pull-Ups / Dead Hang", "30 sec"),
        RoutineStep("Light Cable Row", "15 reps"),
        RoutineStep("Glute Bridge", "15 reps")
    )
    private val warmShoulder = listOf(
        RoutineStep("Neck Rolls (slow)", "30 sec"),
        RoutineStep("Shoulder Rolls (fwd & back)", "30 sec"),
        RoutineStep("Giant Arm Circles", "30 sec"),
        RoutineStep("Light Cable Face Pull", "15 reps"),
        RoutineStep("Bretzel Thoracic Stretch", "30 sec/side"),
        RoutineStep("Y-T-W Band Activation", "10 each")
    )
    private val warmLegs = listOf(
        RoutineStep("Leg Swings (front-back)", "20/leg"),
        RoutineStep("Leg Swings (side-to-side)", "20/leg"),
        RoutineStep("Bodyweight Squat (full depth)", "15 reps"),
        RoutineStep("Hip Circles", "10/direction"),
        RoutineStep("Walking Lunges (bodyweight)", "10/leg"),
        RoutineStep("Glute Bridge", "20 reps")
    )

    private val stretchChest = listOf(
        RoutineStep("Doorway Chest Stretch", "40 sec/side"),
        RoutineStep("Cross-Body Shoulder Stretch", "30 sec/side"),
        RoutineStep("Overhead Tricep Stretch", "30 sec/side"),
        RoutineStep("Lat Stretch (side lean)", "40 sec/side"),
        RoutineStep("Foam Roll Upper Back", "60 sec"),
        RoutineStep("Deep Belly Breathing", "4 breaths")
    )
    private val stretchBack = listOf(
        RoutineStep("Lat Stretch (arms overhead, side lean)", "40 sec/side"),
        RoutineStep("Child's Pose (wide knees)", "45 sec"),
        RoutineStep("Bicep Wall Stretch", "30 sec/side"),
        RoutineStep("Forearm Flexor Stretch", "30 sec/side"),
        RoutineStep("Thread the Needle", "30 sec/side"),
        RoutineStep("Seated Forward Fold", "40 sec")
    )
    private val stretchShoulder = listOf(
        RoutineStep("Sleeper Stretch", "40 sec/side"),
        RoutineStep("Overhead Shoulder Stretch", "30 sec/side"),
        RoutineStep("Chest Opener (hands behind)", "45 sec"),
        RoutineStep("Overhead Tricep Stretch", "30 sec/side"),
        RoutineStep("Supine Spinal Twist", "30 sec/side"),
        RoutineStep("4-7-8 Breathing", "4 breaths")
    )
    private val stretchLegs = listOf(
        RoutineStep("Standing Quad Stretch", "40 sec/leg"),
        RoutineStep("Kneeling Hip Flexor Stretch", "45 sec/side"),
        RoutineStep("Seated Hamstring Stretch", "40 sec/leg"),
        RoutineStep("Pigeon Pose (glutes)", "40 sec/side"),
        RoutineStep("Calf Wall Stretch", "30 sec/side"),
        RoutineStep("Deep Belly Breathing", "4 breaths")
    )

    val dayRoutines: Map<TrainingDay, DayRoutineMeta> = mapOf(
        TrainingDay.SUNDAY to DayRoutineMeta(
            day = TrainingDay.SUNDAY,
            whyNote = "Keeps the trunk gain going (31.6 → 32.0 kg). Heavy bench + row are what moved it.",
            warmUp = warmChest,
            cardioNote = "15 min incline walk · 10–12% · 5.0–5.5 km/h",
            stretch = stretchChest
        ),
        TrainingDay.MONDAY to DayRoutineMeta(
            day = TrainingDay.MONDAY,
            whyNote = "Legs fell 10.4 → 10.0/10.2 kg on one leg day/week. Legs are now trained twice a week.",
            warmUp = warmLegs,
            cardioNote = "No cardio today — save the legs. 8k+ steps through the day.",
            stretch = stretchLegs
        ),
        TrainingDay.TUESDAY to DayRoutineMeta(
            day = TrainingDay.TUESDAY,
            whyNote = "Second chest + shoulder hit of the week. Single-arm triceps added for left arm symmetry.",
            warmUp = warmShoulder,
            cardioNote = "20 min incline walk · 10–12% · 5.0–5.5 km/h",
            stretch = stretchShoulder
        ),
        TrainingDay.WEDNESDAY to DayRoutineMeta(
            day = TrainingDay.WEDNESDAY,
            whyNote = "Left arm slipped 2.8 → 2.7 kg while right stayed 3.0. Every unilateral arm move starts LEFT + 1 extra left set.",
            warmUp = warmBack,
            cardioNote = "15 min incline walk · 10–12% · 5.0–5.5 km/h",
            stretch = stretchBack
        ),
        TrainingDay.THURSDAY to DayRoutineMeta(
            day = TrainingDay.THURSDAY,
            whyNote = "Second leg day. Bulgarian split squats start LEFT + 1 extra left set to close the 10.0 vs 10.2 kg gap.",
            warmUp = warmLegs,
            cardioNote = "Optional 10 min easy walk to cool down.",
            stretch = stretchLegs
        )
    )

    val roadmap: List<RoadmapWeek> = listOf(
        RoadmapWeek("W1 · Base", "22–26 Nov", "RIR 2–3", "Learn the new moves, find working weights, log everything in LiftLog."),
        RoadmapWeek("W2 · Build", "29 Nov–3 Dec", "RIR 2", "Beat W1 on every lift by 1 rep or 2.5 kg."),
        RoadmapWeek("W3 · Push", "6–10 Dec", "RIR 1–2", "+1 set on the first two exercises of each day."),
        RoadmapWeek("W4 · Peak", "13–17 Dec", "RIR 0–1", "Hardest week. Last set of each isolation move goes to failure."),
        RoadmapWeek("W5 · Peak", "20–24 Dec", "RIR 1", "Keep W4 volume. Try for top-set PRs."),
        RoadmapWeek(
            "W6 · Deload + Scan",
            "27–31 Dec",
            "RIR 3",
            "Sun–Tue: same weights, half the sets. Wed 30: rest. Thu 31: scan first thing in the morning, before training.",
            highlight = true
        )
    )

    val scanTargets: List<ScanTargetMetric> = listOf(
        ScanTargetMetric("Fat Mass", "14.4 kg (19.1%)", "≤ 13.3 kg (≈ 17.8%)"),
        ScanTargetMetric("Muscle Mass", "57.9 kg", "58.2–58.5 kg"),
        ScanTargetMetric("Body Weight", "75.5 kg", "74.8–75.3 kg"),
        ScanTargetMetric("Visceral Fat", "8", "7"),
        ScanTargetMetric("Trunk Fat", "8.4 kg", "≤ 7.9 kg"),
        ScanTargetMetric("Left Arm Muscle", "2.7 kg", "2.8+ kg"),
        ScanTargetMetric("Left / Right Leg", "10.0 / 10.2 kg", "10.2 / 10.3 kg"),
        ScanTargetMetric("Boditrax Score", "810", "820+ (beats 813 best)"),
        ScanTargetMetric("Metabolic Age", "27", "≤ 25")
    )

    val meals: List<DietMeal> = listOf(
        DietMeal("Breakfast", null, "3 whole eggs + 2 whites (omelette with onion, chilli, spinach) + 2 ragi dosa or 2 pesarattu + mint chutney", "P 32g · 420 kcal"),
        DietMeal("Pre-Workout", "60–90 min before", "50 g oats cooked in 250 ml low-fat milk + 1 banana, or 3 idli + sambar", "P 15g · 400 kcal"),
        DietMeal("Post-Workout", "Within 1 hr", "1 scoop whey in water + 1 banana, or 1 cup chana sundal + 1 glass buttermilk", "P 26g · 250 kcal"),
        DietMeal("Lunch", null, "150 g chicken (curry or pepper fry, light oil) + 1 cup red matta / brown rice / millet (thinai, varagu) + poriyal + small bowl curd", "P 45g · 650 kcal"),
        DietMeal("Snack", null, "200 g Greek yogurt or 250 ml laban + 30 g roasted chana, or 2 boiled eggs + cucumber", "P 22g · 250 kcal"),
        DietMeal("Dinner", null, "150 g fish (mackerel / sardine / tilapia, tawa-fried) + 2 wheat chapati + keerai or dal + rasam", "P 40g · 550 kcal")
    )

    val scanDayChecklist: List<String> = listOf(
        "Scan on Thu 31 Dec in the morning, same as your 19 Nov scan, and keep every future scan at the same time of day.",
        "Wed 30 Dec is a full rest day. Hard training in the 24–48 hrs before inflates water readings and blurs the result.",
        "Normal dinner on the 30th. No extra-salty meal (biryani, mandi, shawarma) the night before.",
        "Drink your normal 3 L on the 30th. On the morning of the scan: ~300 ml water on waking, nothing else.",
        "Scan on an empty stomach (at least 3–4 hrs after eating) and empty your bladder just before.",
        "Same light clothing as before, no shoes or socks, feet and hands clean and dry.",
        "No sauna, steam room, or heavy sweating on the morning of the scan.",
        "If you take creatine, don't stop or start it in the final week."
    )
}

object PresetBlueprints {
    private fun ex(
        day: TrainingDay,
        name: String,
        target: String,
        targetSets: String,
        instructions: String
    ) = GeneratedExerciseDraft(
        name = name,
        day = day.label,
        target = target,
        targetSets = targetSets,
        instructions = instructions,
        youtubeUrl = buildYoutubeSearchUrl(name)
    )

    val machoMuscleV3 = ProgramBlueprint(
        id = "macho_muscle_v3",
        title = MachoMuscleV3Guide.PROGRAM_NAME,
        subtitle = "PureGym Khobar v3 · 2x/week frequency + LEFT FIRST unilateral fixes for Dec 31 Boditrax push.",
        badge = "V3 · ACTIVE",
        exercises = listOf(
            // SUNDAY — Upper Strength
            ex(
                TrainingDay.SUNDAY,
                "Barbell Bench Press",
                "Chest · Compound",
                "4 × 5–6",
                "[COMPOUND] 2 ramp-up sets first (bar × 10, 60% × 5). Shoulder blades pinned, elbows ~45°. Progression: +2.5 kg when all 4 sets hit 6."
            ),
            ex(
                TrainingDay.SUNDAY,
                "Chest-Supported T-Bar or Barbell Row",
                "Back · Compound",
                "4 × 6–8",
                "[COMPOUND] Pull to lower ribs. Pause 1 sec. No torso swing. Progression: +2.5 kg when all sets hit 8."
            ),
            ex(
                TrainingDay.SUNDAY,
                "Incline DB Press (30°)",
                "Upper Chest · Compound",
                "3 × 8–10",
                "[COMPOUND] Full stretch at the bottom, 2-sec lowering. Progression: +2 kg DBs at 10 reps."
            ),
            ex(
                TrainingDay.SUNDAY,
                "Wide-Grip Lat Pulldown",
                "Lats & Upper Back",
                "3 × 8–10",
                "Chest up, pull bar to collarbone. Progression: +5 kg at 10 reps."
            ),
            ex(
                TrainingDay.SUNDAY,
                "Single-Arm Cable Lateral Raise",
                "Side Delts · Left First",
                "3 × 12–15 /arm",
                "[LEFT FIRST] LEFT first. Cable behind body for constant tension. Progression: Add reps, then weight."
            ),
            ex(
                TrainingDay.SUNDAY,
                "Superset: EZ-Bar Curl + Rope Pushdown",
                "Biceps & Triceps",
                "2 × 10 + 2 × 12",
                "Back-to-back, 60 sec rest after the pair."
            ),

            // MONDAY — Lower Strength
            ex(
                TrainingDay.MONDAY,
                "Barbell Back Squat",
                "Quads · Compound",
                "4 × 5–6",
                "[COMPOUND] 2–3 ramp-up sets. Brace, below parallel, drive up hard. Progression: +2.5 kg when all sets hit 6."
            ),
            ex(
                TrainingDay.MONDAY,
                "Romanian Deadlift (barbell)",
                "Hamstrings · Compound",
                "3 × 8",
                "[COMPOUND] Soft knees, hips back, bar slides down thighs. Stop at hamstring stretch. Progression: +2.5 kg at 8 clean reps."
            ),
            ex(
                TrainingDay.MONDAY,
                "Leg Press",
                "Quads & Glutes",
                "3 × 10–12",
                "Feet mid-platform, deep range, no knee lockout. Progression: +10 kg at 12 reps."
            ),
            ex(
                TrainingDay.MONDAY,
                "Single-Leg Leg Extension",
                "Quads · Left First +1",
                "3 × 12 /leg (+1 left)",
                "[LEFT FIRST] [NEW] LEFT first, then an extra left set. Squeeze 1 sec at top. Progression: Left leg sets the weight."
            ),
            ex(
                TrainingDay.MONDAY,
                "Lying Leg Curl",
                "Hamstrings",
                "3 × 10–12",
                "Hips down. Slow 3-sec lowering. Progression: +1 plate at 12 reps."
            ),
            ex(
                TrainingDay.MONDAY,
                "Standing Calf Raise (heavy)",
                "Calves",
                "4 × 10–12",
                "Full stretch at the bottom, 1-sec hold at top. Progression: Add weight weekly."
            ),
            ex(
                TrainingDay.MONDAY,
                "Hanging Leg Raise",
                "Lower Abs & Core",
                "3 × 10–12",
                "No swinging. Curl the pelvis up."
            ),

            // TUESDAY — Push Hypertrophy
            ex(
                TrainingDay.TUESDAY,
                "Seated DB Shoulder Press",
                "Shoulders · Compound",
                "4 × 8–10",
                "[COMPOUND] Lower to ear height, no low-back arch. Progression: +2 kg DBs at 10 reps."
            ),
            ex(
                TrainingDay.TUESDAY,
                "Machine Chest Press (or Smith Incline)",
                "Chest · Compound",
                "3 × 10–12",
                "[COMPOUND] Controlled 2-sec lowering, chase the pump. Progression: +5 kg at 12 reps."
            ),
            ex(
                TrainingDay.TUESDAY,
                "Low-to-High Cable Fly",
                "Upper Chest",
                "3 × 12–15",
                "Upper-chest bias. Hold the squeeze 1 sec."
            ),
            ex(
                TrainingDay.TUESDAY,
                "DB Lateral Raise",
                "Side Delts",
                "4 × 12–15",
                "Last set = drop set (drop 30%, go to failure). Lead with elbows. Progression: Add reps first."
            ),
            ex(
                TrainingDay.TUESDAY,
                "Single-Arm Overhead Cable Tricep Ext.",
                "Triceps · Left First +1",
                "3 × 12 /arm (+1 left)",
                "[LEFT FIRST] [NEW] LEFT first, then an extra left set. Deep stretch behind the head. Progression: Left arm sets the weight."
            ),
            ex(
                TrainingDay.TUESDAY,
                "Dips (weighted when ready)",
                "Lower Chest & Triceps",
                "3 × 8–10",
                "[COMPOUND] Slight forward lean. Add a belt once 3 × 10 is easy. Progression: +2.5 kg on the belt."
            ),
            ex(
                TrainingDay.TUESDAY,
                "Cable Crunch",
                "Core & Abs",
                "3 × 12–15",
                "Ribs to hips. Round the spine, don't hip-hinge."
            ),

            // WEDNESDAY — Pull + Arms
            ex(
                TrainingDay.WEDNESDAY,
                "Conventional Deadlift",
                "Posterior · Compound",
                "3 × 4–5",
                "[COMPOUND] 2–3 ramp-up sets. Brace hard, bar over mid-foot. Form first, weight second. Progression: +2.5–5 kg when all sets clean."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Pull-Ups (assisted or weighted)",
                "Lats · Compound",
                "4 × 6–10",
                "[COMPOUND] Full hang at bottom, chest to bar. Use the assisted machine if needed. Progression: +1 rep/set each week."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Single-Arm DB Row",
                "Lats · Left First +1",
                "3 × 10 /arm (+1 left)",
                "[LEFT FIRST] [COMPOUND] LEFT first. Elbow to hip, full stretch at bottom. Progression: Left arm sets the weight."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Close-Grip Seated Cable Row",
                "Mid Back",
                "3 × 10–12",
                "Pause and squeeze at the chest. Progression: +5 kg at 12 reps."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Cable Face Pull",
                "Rear Delts & Rotator Cuff",
                "3 × 15",
                "Rope to forehead, elbows high. Shoulder health."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Single-Arm Incline DB Curl",
                "Biceps · Left First +1",
                "3 × 10 /arm (+1 left)",
                "[LEFT FIRST] [NEW] LEFT first. Bench at 45° for a long-head stretch. 3-sec lowering. Progression: Left arm sets the weight."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "DB Hammer Curl",
                "Brachialis · Left First",
                "3 × 10–12",
                "[LEFT FIRST] Neutral grip. Start each set with the LEFT arm."
            ),
            ex(
                TrainingDay.WEDNESDAY,
                "Ab Wheel Rollout (kneeling)",
                "Core Stability",
                "3 × 10",
                "Brace, roll only as far as you can control."
            ),

            // THURSDAY — Legs Hypertrophy
            ex(
                TrainingDay.THURSDAY,
                "Hack Squat (or Smith Squat)",
                "Quads · Compound",
                "4 × 8–10",
                "[COMPOUND] Deep and controlled. Light on the spine after Wednesday's deadlift. Progression: +5–10 kg at 10 reps."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Bulgarian Split Squat (DB)",
                "Quads & Glutes · Left First +1",
                "3 × 8–10 /leg (+1 left)",
                "[LEFT FIRST] [COMPOUND] LEFT first. Front shin roughly vertical, drop straight down. Progression: Left leg sets the weight."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Barbell Hip Thrust",
                "Glutes · Compound",
                "3 × 8–10",
                "[COMPOUND] Chin tucked, ribs down, pause 1 sec at lockout. Progression: +5 kg at 10 reps."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Leg Extension",
                "Quads",
                "3 × 12–15",
                "Last set: 15 reps + 5 partials."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Seated Leg Curl",
                "Hamstrings",
                "3 × 12–15",
                "Lean forward slightly for a deeper hamstring stretch."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Seated Calf Raise",
                "Soleus & Calves",
                "4 × 15",
                "Pause at the bottom stretch."
            ),
            ex(
                TrainingDay.THURSDAY,
                "Pallof Press",
                "Anti-Rotation Core",
                "3 × 12 /side",
                "[NEW] Anti-rotation core. Arms long, don't let the cable twist you."
            )
        )
    )

    val all: List<ProgramBlueprint> = listOf(
        machoMuscleV3,
        ProgramBlueprint(
            id = "strength_foundation",
            title = "Strength & Density Foundation",
            subtitle = "Heavy compound progression (RPE 8–9) paired with targeted hypertrophy accessories.",
            badge = "STRENGTH",
            exercises = listOf(
                ex(TrainingDay.SUNDAY, "Barbell Bench Press", "Chest & Triceps", "5 x 5", "Explosive concentric drive; pause 1 second on chest for strict power output."),
                ex(TrainingDay.SUNDAY, "Weighted Dip", "Lower Chest & Triceps", "3 x 8-10", "Lean torso slightly forward and descend until shoulders drop below elbows."),
                ex(TrainingDay.SUNDAY, "Incline Machine Press", "Upper Chest", "3 x 10-12", "Smooth piston-like reps stopping just short of lockout."),
                ex(TrainingDay.SUNDAY, "Single-Arm Tricep Pushdown", "Triceps", "3 x 12", "Control every eccentric rep to maximize elbow tendon stability."),
                ex(TrainingDay.MONDAY, "Barbell Pendlay Row", "Upper & Mid Back", "4 x 6-8", "Pull explosively from a dead stop on the floor each rep with flat spine."),
                ex(TrainingDay.MONDAY, "Neutral-Grip Lat Pulldown", "Lats & Biceps", "4 x 8-10", "Drive elbows down and slightly forward to isolate lower lats."),
                ex(TrainingDay.MONDAY, "Dumbbell Shrug", "Upper Traps", "3 x 12", "Pause 1 second at peak elevation without rolling shoulders."),
                ex(TrainingDay.MONDAY, "Hammer Curl", "Brachialis & Forearms", "3 x 10-12", "Strict torso posture; squeeze forearm flexors at top."),
                ex(TrainingDay.TUESDAY, "Barbell Back Squat", "Quads & Posterior Core", "5 x 5", "Create full-body intra-abdominal brace before descending."),
                ex(TrainingDay.TUESDAY, "Leg Press", "Quads", "3 x 10-12", "Place feet lower on sled for maximum knee flexion; never lock knees harshly."),
                ex(TrainingDay.TUESDAY, "Cable Crunch", "Rectus Abdominis", "4 x 12-15", "Flex thoracic spine downward rather than hinging at hips."),
                ex(TrainingDay.TUESDAY, "Seated Calf Raise", "Soleus", "4 x 15", "Full ankle dorsiflexion stretch at bottom of each rep."),
                ex(TrainingDay.WEDNESDAY, "Standing Overhead Press", "Shoulders & Core", "4 x 6-8", "Squeeze glutes and quads tight; press bar straight overhead and lock out."),
                ex(TrainingDay.WEDNESDAY, "Dumbbell Lateral Raise", "Lateral Delts", "4 x 12-15", "Raise slightly forward in the scapular plane with controlled tempo."),
                ex(TrainingDay.WEDNESDAY, "Close-Grip Bench Press", "Triceps & Inner Chest", "3 x 8", "Keep elbows tucked around 30 degrees for tricep dominance."),
                ex(TrainingDay.WEDNESDAY, "Barbell Curl", "Biceps", "3 x 8-10", "Minimize body sway and lower over 2 full seconds."),
                ex(TrainingDay.THURSDAY, "Conventional Deadlift", "Posterior Chain", "4 x 5", "Pull slack out of bar before leg drive; lock hips cleanly without hyperextending."),
                ex(TrainingDay.THURSDAY, "Seated Leg Curl", "Hamstrings", "3 x 10-12", "Lock thigh pad firmly to maximize hamstring tension."),
                ex(TrainingDay.THURSDAY, "45-Degree Back Extension", "Glutes & Erectors", "3 x 12", "Round slightly or hinge cleanly through hips for posterior chain hypertrophy."),
                ex(TrainingDay.THURSDAY, "Farmer's Carry", "Grip, Traps & Core", "3 x 40m", "Walk tall with packed shoulders and brisk, controlled steps.")
            )
        )
    )
}
