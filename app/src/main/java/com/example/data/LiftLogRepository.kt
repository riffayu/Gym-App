package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.BodyScan
import com.example.model.GeneratedExerciseDraft
import com.example.model.TrainingDay
import com.example.model.WorkoutExercise
import com.example.model.WorkoutSetLog
import com.example.model.buildYoutubeSearchUrl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LiftLogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUid(expectedUserId: String): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(currentUid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return currentUid
    }

    fun observeExercises(userId: String): Flow<List<WorkoutExercise>> = flow {
        val uid = requireAuthorizedUid(userId)
        val path = "users/$uid/exercises"
        emitAll(
            db.collection("users").document(uid).collection("exercises")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutExercise::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<WorkoutExercise>(
                            { TrainingDay.fromLabel(it.day).ordinal },
                            { it.sortOrder }
                        )
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeWorkoutLogs(userId: String): Flow<List<WorkoutSetLog>> = flow {
        val uid = requireAuthorizedUid(userId)
        val path = "users/$uid/workoutLogs"
        emitAll(
            db.collection("users").document(uid).collection("workoutLogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutSetLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<WorkoutSetLog> { it.sessionDate }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                            .thenBy { it.setNumber }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBodyScans(userId: String): Flow<List<BodyScan>> = flow {
        val uid = requireAuthorizedUid(userId)
        val path = "users/$uid/bodyScans"
        emitAll(
            db.collection("users").document(uid).collection("bodyScans")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BodyScan::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun applyProgram(
        userId: String,
        programName: String,
        drafts: List<GeneratedExerciseDraft>
    ): Result<Unit> {
        val colPath = "users/$userId/exercises"
        return try {
            val uid = requireAuthorizedUid(userId)
            val safeProgramName = programName.trim().ifBlank { "Custom 5-Day Split" }.take(80)
            require(drafts.isNotEmpty()) { "Program must contain at least one exercise." }

            val colRef = db.collection("users").document(uid).collection("exercises")
            val existingDocs = colRef.get().await().documents
            val batch = db.batch()

            existingDocs.forEach { doc ->
                batch.delete(doc.reference)
            }

            val dayCounters = mutableMapOf<String, Long>()
            drafts.forEach { draft ->
                val validDay = TrainingDay.fromLabel(draft.day).label
                val order = dayCounters.getOrDefault(validDay, 0L)
                dayCounters[validDay] = order + 1L

                val name = draft.name.trim().ifBlank { "Exercise" }.take(100)
                val target = draft.target.trim().ifBlank { "Full Body" }.take(80)
                val targetSets = draft.targetSets.trim().ifBlank { "3 x 10" }.take(40)
                val instructions = draft.instructions.trim().take(1000)
                val youtubeUrl = draft.youtubeUrl.trim()
                    .ifBlank { buildYoutubeSearchUrl(name) }
                    .take(500)

                val docRef = colRef.document()
                val payload = mapOf(
                    "userId" to uid,
                    "programName" to safeProgramName,
                    "day" to validDay,
                    "name" to name,
                    "target" to target,
                    "targetSets" to targetSets,
                    "instructions" to instructions,
                    "youtubeUrl" to youtubeUrl,
                    "sortOrder" to order.coerceIn(0L, 100L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun addExercise(
        userId: String,
        programName: String,
        day: TrainingDay,
        name: String,
        target: String,
        targetSets: String,
        instructions: String,
        sortOrder: Long
    ): Result<Unit> {
        var docPath = "users/$userId/exercises"
        return try {
            val uid = requireAuthorizedUid(userId)
            val safeName = name.trim().take(100)
            val safeTarget = target.trim().take(80)
            val safeSets = targetSets.trim().take(40)
            require(safeName.isNotEmpty()) { "Exercise name is required." }
            require(safeTarget.isNotEmpty()) { "Target muscle is required." }
            require(safeSets.isNotEmpty()) { "Target sets are required." }

            val docRef = db.collection("users").document(uid).collection("exercises").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "programName" to programName.trim().ifBlank { "Custom 5-Day Split" }.take(80),
                "day" to day.label,
                "name" to safeName,
                "target" to safeTarget,
                "targetSets" to safeSets,
                "instructions" to instructions.trim().take(1000),
                "youtubeUrl" to buildYoutubeSearchUrl(safeName).take(500),
                "sortOrder" to sortOrder.coerceIn(0L, 100L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateExercise(
        userId: String,
        exerciseId: String,
        day: TrainingDay,
        name: String,
        target: String,
        targetSets: String,
        instructions: String
    ): Result<Unit> {
        var docPath = "users/$userId/exercises/$exerciseId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(exerciseId.isNotBlank()) { "Exercise ID cannot be blank." }
            val safeName = name.trim().take(100)
            val safeTarget = target.trim().take(80)
            val safeSets = targetSets.trim().take(40)
            require(safeName.isNotEmpty()) { "Exercise name is required." }
            require(safeTarget.isNotEmpty()) { "Target muscle is required." }
            require(safeSets.isNotEmpty()) { "Target sets are required." }

            val docRef = db.collection("users").document(uid).collection("exercises").document(exerciseId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "day" to day.label,
                    "name" to safeName,
                    "target" to safeTarget,
                    "targetSets" to safeSets,
                    "instructions" to instructions.trim().take(1000),
                    "youtubeUrl" to buildYoutubeSearchUrl(safeName).take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExercise(userId: String, exerciseId: String): Result<Unit> {
        var docPath = "users/$userId/exercises/$exerciseId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(exerciseId.isNotBlank()) { "Exercise ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("exercises").document(exerciseId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logSet(
        userId: String,
        exercise: WorkoutExercise,
        sessionDate: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        rpe: Int,
        notes: String?
    ): Result<Unit> {
        var docPath = "users/$userId/workoutLogs"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(exercise.id.isNotBlank()) { "Exercise ID must not be blank." }
            require(sessionDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                "Session date must be YYYY-MM-DD."
            }
            val safeWeight = weightKg.coerceIn(0.0, 1000.0)
            val safeReps = reps.coerceIn(1, 200).toLong()
            val safeRpe = rpe.coerceIn(1, 10).toLong()
            val safeSetNumber = setNumber.coerceIn(1, 30).toLong()
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300)

            val docRef = db.collection("users").document(uid).collection("workoutLogs").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "exerciseId" to exercise.id.take(128),
                "exerciseName" to exercise.name.trim().ifBlank { "Exercise" }.take(100),
                "day" to TrainingDay.fromLabel(exercise.day).label,
                "target" to exercise.target.trim().ifBlank { "Strength" }.take(80),
                "sessionDate" to sessionDate,
                "setNumber" to safeSetNumber,
                "weightKg" to safeWeight,
                "reps" to safeReps,
                "rpe" to safeRpe,
                "completed" to true,
                "notes" to cleanNotes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSetCompleted(
        userId: String,
        logId: String,
        completed: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/workoutLogs/$logId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(logId.isNotBlank()) { "Log ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("workoutLogs").document(logId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSetLog(
        userId: String,
        logId: String,
        weightKg: Double,
        reps: Int,
        rpe: Int,
        notes: String?
    ): Result<Unit> {
        var docPath = "users/$userId/workoutLogs/$logId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(logId.isNotBlank()) { "Log ID cannot be blank." }
            val safeWeight = weightKg.coerceIn(0.0, 1000.0)
            val safeReps = reps.coerceIn(1, 200).toLong()
            val safeRpe = rpe.coerceIn(1, 10).toLong()
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300)

            val docRef = db.collection("users").document(uid).collection("workoutLogs").document(logId)
            docPath = docRef.path
            val updates = mapOf(
                "weightKg" to safeWeight,
                "reps" to safeReps,
                "rpe" to safeRpe,
                "notes" to (cleanNotes ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSetLog(userId: String, logId: String): Result<Unit> {
        var docPath = "users/$userId/workoutLogs/$logId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(logId.isNotBlank()) { "Log ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("workoutLogs").document(logId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveBodyScan(
        userId: String,
        weightKg: Double,
        muscleMassKg: Double,
        bodyFatPercent: Double,
        observations: String,
        aiSummary: String?
    ): Result<Unit> {
        var docPath = "users/$userId/bodyScans"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(weightKg > 0.0 && weightKg <= 500.0) { "Weight must be between 1 and 500 kg." }
            require(muscleMassKg > 0.0 && muscleMassKg <= 300.0) { "Muscle mass must be between 1 and 300 kg." }
            require(bodyFatPercent in 1.0..80.0) { "Body fat % must be between 1 and 80%." }

            val cleanSummary = aiSummary?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
            val docRef = db.collection("users").document(uid).collection("bodyScans").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "weightKg" to weightKg,
                "muscleMassKg" to muscleMassKg,
                "bodyFatPercent" to bodyFatPercent,
                "observations" to observations.trim().take(500),
                "aiSummary" to cleanSummary,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBodyScan(
        userId: String,
        scanId: String,
        weightKg: Double,
        muscleMassKg: Double,
        bodyFatPercent: Double,
        observations: String
    ): Result<Unit> {
        var docPath = "users/$userId/bodyScans/$scanId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(scanId.isNotBlank()) { "Scan ID cannot be blank." }
            require(weightKg > 0.0 && weightKg <= 500.0) { "Weight must be between 1 and 500 kg." }
            require(muscleMassKg > 0.0 && muscleMassKg <= 300.0) { "Muscle mass must be between 1 and 300 kg." }
            require(bodyFatPercent in 1.0..80.0) { "Body fat % must be between 1 and 80%." }

            val docRef = db.collection("users").document(uid).collection("bodyScans").document(scanId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "weightKg" to weightKg,
                    "muscleMassKg" to muscleMassKg,
                    "bodyFatPercent" to bodyFatPercent,
                    "observations" to observations.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBodyScan(userId: String, scanId: String): Result<Unit> {
        var docPath = "users/$userId/bodyScans/$scanId"
        return try {
            val uid = requireAuthorizedUid(userId)
            require(scanId.isNotBlank()) { "Scan ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("bodyScans").document(scanId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
