package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiCoachRepository
import com.example.data.LiftLogRepository
import com.example.model.BodyScan
import com.example.model.GeneratedProgramPreview
import com.example.model.ProgramBlueprint
import com.example.model.TrainingDay
import com.example.model.UiState
import com.example.model.WorkoutExercise
import com.example.model.WorkoutSetLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface AiGenerationState {
    data object Idle : AiGenerationState
    data object Generating : AiGenerationState
    data class Preview(val program: GeneratedProgramPreview) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}

data class RestTimerState(
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 90,
    val isActive: Boolean = false
)

class LiftLogViewModel(
    private val repository: LiftLogRepository,
    private val aiCoachRepository: AiCoachRepository,
    private val currentUserId: String
) : ViewModel() {

    val exercisesState: StateFlow<UiState<List<WorkoutExercise>>> =
        repository.observeExercises(currentUserId)
            .map<List<WorkoutExercise>, UiState<List<WorkoutExercise>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing exercises", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load exercises"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val workoutLogsState: StateFlow<UiState<List<WorkoutSetLog>>> =
        repository.observeWorkoutLogs(currentUserId)
            .map<List<WorkoutSetLog>, UiState<List<WorkoutSetLog>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing workout logs", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load workout logs"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val bodyScansState: StateFlow<UiState<List<BodyScan>>> =
        repository.observeBodyScans(currentUserId)
            .map<List<BodyScan>, UiState<List<BodyScan>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing body scans", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load body scans"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _selectedDay = MutableStateFlow(defaultTodayTrainingDay())
    val selectedDay: StateFlow<TrainingDay> = _selectedDay.asStateFlow()

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private var timerJob: Job? = null

    fun selectDay(day: TrainingDay) {
        _selectedDay.value = day
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun todaySessionDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    fun applyBlueprint(blueprint: ProgramBlueprint) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.applyProgram(currentUserId, blueprint.title, blueprint.exercises)
            _isMutating.value = false
            _feedbackMessage.value = result.fold(
                onSuccess = { "${blueprint.title} loaded across Sunday–Thursday." },
                onFailure = { it.localizedMessage ?: "Failed to apply program." }
            )
        }
    }

    fun addCustomExercise(
        day: TrainingDay,
        name: String,
        target: String,
        targetSets: String,
        instructions: String,
        currentCountForDay: Int,
        programName: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addExercise(
                userId = currentUserId,
                programName = programName,
                day = day,
                name = name,
                target = target,
                targetSets = targetSets,
                instructions = instructions,
                sortOrder = currentCountForDay.toLong()
            )
            _isMutating.value = false
            _feedbackMessage.value = result.fold(
                onSuccess = { "Added $name to ${day.label}." },
                onFailure = { it.localizedMessage ?: "Could not add exercise." }
            )
        }
    }

    fun updateExercise(
        exerciseId: String,
        day: TrainingDay,
        name: String,
        target: String,
        targetSets: String,
        instructions: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateExercise(
                userId = currentUserId,
                exerciseId = exerciseId,
                day = day,
                name = name,
                target = target,
                targetSets = targetSets,
                instructions = instructions
            )
            _isMutating.value = false
            _feedbackMessage.value = result.fold(
                onSuccess = { "Updated $name." },
                onFailure = { it.localizedMessage ?: "Could not update exercise." }
            )
        }
    }

    fun deleteExercise(exerciseId: String, exerciseName: String) {
        viewModelScope.launch {
            val result = repository.deleteExercise(currentUserId, exerciseId)
            _feedbackMessage.value = result.fold(
                onSuccess = { "Removed $exerciseName." },
                onFailure = { it.localizedMessage ?: "Could not remove exercise." }
            )
        }
    }

    fun logWorkingSet(
        exercise: WorkoutExercise,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        rpe: Int,
        notes: String?,
        autoStartRestSeconds: Int = 90
    ) {
        viewModelScope.launch {
            val result = repository.logSet(
                userId = currentUserId,
                exercise = exercise,
                sessionDate = todaySessionDate(),
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                notes = notes
            )
            result.onSuccess {
                startRestTimer(autoStartRestSeconds)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to save set."
            }
        }
    }

    fun toggleSetCompleted(log: WorkoutSetLog) {
        viewModelScope.launch {
            val result = repository.toggleSetCompleted(currentUserId, log.id, !log.completed)
            result.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to update set."
            }
        }
    }

    fun updateSetLog(
        logId: String,
        weightKg: Double,
        reps: Int,
        rpe: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            val result = repository.updateSetLog(
                userId = currentUserId,
                logId = logId,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                notes = notes
            )
            _feedbackMessage.value = result.fold(
                onSuccess = { "Updated logged set." },
                onFailure = { err -> err.localizedMessage ?: "Failed to update set." }
            )
        }
    }

    fun deleteSetLog(logId: String) {
        viewModelScope.launch {
            val result = repository.deleteSetLog(currentUserId, logId)
            result.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to delete set."
            }
        }
    }

    fun generateAiProgram(
        weightText: String,
        muscleText: String,
        fatText: String,
        observations: String
    ) {
        val weight = weightText.toDoubleOrNull()
        val muscle = muscleText.toDoubleOrNull()
        val fat = fatText.toDoubleOrNull()

        if (weight == null || weight <= 0.0 || weight > 500.0) {
            _aiState.value = AiGenerationState.Error("Enter a valid body weight between 1 and 500 kg.")
            return
        }
        if (muscle == null || muscle <= 0.0 || muscle > 300.0) {
            _aiState.value = AiGenerationState.Error("Enter a valid muscle mass between 1 and 300 kg.")
            return
        }
        if (fat == null || fat < 1.0 || fat > 80.0) {
            _aiState.value = AiGenerationState.Error("Enter a valid body fat percentage between 1% and 80%.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AiGenerationState.Generating
            val aiResult = aiCoachRepository.generateProgramFromBoditrax(
                weightKg = weight,
                muscleMassKg = muscle,
                bodyFatPercent = fat,
                observations = observations
            )

            aiResult.fold(
                onSuccess = { preview ->
                    repository.saveBodyScan(
                        userId = currentUserId,
                        weightKg = weight,
                        muscleMassKg = muscle,
                        bodyFatPercent = fat,
                        observations = observations,
                        aiSummary = preview.summary
                    )
                    _aiState.value = AiGenerationState.Preview(preview)
                },
                onFailure = { error ->
                    _aiState.value = AiGenerationState.Error(
                        error.localizedMessage ?: "Failed to generate AI program."
                    )
                }
            )
        }
    }

    fun saveBodyScanOnly(
        weightText: String,
        muscleText: String,
        fatText: String,
        observations: String
    ) {
        val weight = weightText.toDoubleOrNull()
        val muscle = muscleText.toDoubleOrNull()
        val fat = fatText.toDoubleOrNull()
        if (weight == null || muscle == null || fat == null ||
            weight <= 0.0 || weight > 500.0 ||
            muscle <= 0.0 || muscle > 300.0 ||
            fat < 1.0 || fat > 80.0
        ) {
            _feedbackMessage.value = "Check your Boditrax values before saving."
            return
        }
        viewModelScope.launch {
            val result = repository.saveBodyScan(
                userId = currentUserId,
                weightKg = weight,
                muscleMassKg = muscle,
                bodyFatPercent = fat,
                observations = observations,
                aiSummary = null
            )
            _feedbackMessage.value = result.fold(
                onSuccess = { "Boditrax scan logged." },
                onFailure = { it.localizedMessage ?: "Could not save body scan." }
            )
        }
    }

    fun updateBodyScan(
        scanId: String,
        weightText: String,
        muscleText: String,
        fatText: String,
        observations: String
    ) {
        val weight = weightText.toDoubleOrNull()
        val muscle = muscleText.toDoubleOrNull()
        val fat = fatText.toDoubleOrNull()
        if (weight == null || muscle == null || fat == null ||
            weight <= 0.0 || weight > 500.0 ||
            muscle <= 0.0 || muscle > 300.0 ||
            fat < 1.0 || fat > 80.0
        ) {
            _feedbackMessage.value = "Check your Boditrax values before updating."
            return
        }
        viewModelScope.launch {
            val result = repository.updateBodyScan(
                userId = currentUserId,
                scanId = scanId,
                weightKg = weight,
                muscleMassKg = muscle,
                bodyFatPercent = fat,
                observations = observations
            )
            _feedbackMessage.value = result.fold(
                onSuccess = { "Updated Boditrax scan." },
                onFailure = { it.localizedMessage ?: "Could not update body scan." }
            )
        }
    }

    fun deleteBodyScan(scanId: String) {
        viewModelScope.launch {
            val result = repository.deleteBodyScan(currentUserId, scanId)
            _feedbackMessage.value = result.fold(
                onSuccess = { "Removed Boditrax scan." },
                onFailure = { it.localizedMessage ?: "Could not delete body scan." }
            )
        }
    }

    fun applyGeneratedProgram(preview: GeneratedProgramPreview, onApplied: () -> Unit) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.applyProgram(
                userId = currentUserId,
                programName = preview.programName,
                drafts = preview.exercises
            )
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Applied '${preview.programName}' to your 5-day split!"
                    onApplied()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to apply AI program."
                }
            )
        }
    }

    fun startRestTimer(seconds: Int = 90) {
        timerJob?.cancel()
        val total = seconds.coerceAtLeast(15)
        _restTimer.value = RestTimerState(
            remainingSeconds = total,
            totalSeconds = total,
            isActive = true
        )
        timerJob = viewModelScope.launch {
            while (_restTimer.value.remainingSeconds > 0) {
                delay(1000L)
                val next = (_restTimer.value.remainingSeconds - 1).coerceAtLeast(0)
                _restTimer.value = _restTimer.value.copy(
                    remainingSeconds = next,
                    isActive = next > 0
                )
            }
        }
    }

    fun addRestSeconds(delta: Int = 15) {
        val current = _restTimer.value
        if (!current.isActive) return
        val updated = (current.remainingSeconds + delta).coerceAtMost(300)
        val newTotal = maxOf(current.totalSeconds, updated)
        _restTimer.value = current.copy(remainingSeconds = updated, totalSeconds = newTotal)
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        _restTimer.value = RestTimerState(remainingSeconds = 0, totalSeconds = 90, isActive = false)
    }

    private fun defaultTodayTrainingDay(): TrainingDay {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> TrainingDay.SUNDAY
            Calendar.MONDAY -> TrainingDay.MONDAY
            Calendar.TUESDAY -> TrainingDay.TUESDAY
            Calendar.WEDNESDAY -> TrainingDay.WEDNESDAY
            Calendar.THURSDAY -> TrainingDay.THURSDAY
            else -> TrainingDay.SUNDAY
        }
    }

    private companion object {
        const val TAG = "LiftLogVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
