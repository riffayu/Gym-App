package com.example.data

import android.util.Log
import com.example.model.GeneratedExerciseDraft
import com.example.model.GeneratedProgramPreview
import com.example.model.TrainingDay
import com.example.model.buildYoutubeSearchUrl
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiCoachRepo"

class AiCoachRepository {

    private val programSchema = Schema.obj(
        properties = mapOf(
            "programName" to Schema.string("Short athletic title for this tailored 5-day program"),
            "summary" to Schema.string("2-sentence coaching rationale addressing the Boditrax scan and weak points"),
            "exercises" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string("Exercise name"),
                        "day" to Schema.enumeration(
                            values = TrainingDay.entries.map { it.label },
                            description = "Training day between Sunday and Thursday"
                        ),
                        "target" to Schema.string("Primary target muscle group"),
                        "targetSets" to Schema.string("Prescribed sets and rep range, e.g. 4 x 8-10"),
                        "instructions" to Schema.string("Concise execution and tempo cues")
                    )
                ),
                description = "20 to 25 exercises total (4 to 5 per day across Sunday through Thursday)"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = programSchema
                temperature = 0.6f
            }
        )
    }

    suspend fun generateProgramFromBoditrax(
        weightKg: Double,
        muscleMassKg: Double,
        bodyFatPercent: Double,
        observations: String
    ): Result<GeneratedProgramPreview> {
        return try {
            val cleanObs = observations.trim().ifBlank { "Balanced hypertrophy, 60 minutes per session" }
            val prompt = """
                You are an expert strength and hypertrophy coach. Based on the following Boditrax body scan metrics,
                generate a custom 5-day workout program (Sunday to Thursday) designed to correct imbalances,
                rebuild weak spots, and maximize hypertrophy.

                Boditrax Scan Metrics:
                - Total Body Weight: $weightKg kg
                - Skeletal Muscle Mass: $muscleMassKg kg
                - Body Fat Percentage: $bodyFatPercent %
                - Special Weak Points / Observations: $cleanObs

                Generate 4 to 5 high-impact exercises per day for each day: Sunday, Monday, Tuesday, Wednesday, and Thursday.
                Keep instructions crisp and actionable for gym floor execution.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty program response."))

            val json = JSONObject(rawJson)
            val programName = json.optString("programName", "Boditrax AI Split").trim()
                .ifBlank { "Boditrax AI Split" }
            val summary = json.optString("summary", "Custom 5-day hypertrophy program tailored to your body scan.").trim()
            val array = json.getJSONArray("exercises")

            val drafts = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val exName = obj.optString("name", "Exercise ${i + 1}").trim()
                    val day = TrainingDay.fromLabel(obj.optString("day", "Sunday")).label
                    val target = obj.optString("target", "Full Body").trim()
                    val targetSets = obj.optString("targetSets", "4 x 8-10").trim()
                    val instructions = obj.optString("instructions", "Control eccentric and focus on mind-muscle connection.").trim()
                    add(
                        GeneratedExerciseDraft(
                            name = exName,
                            day = day,
                            target = target,
                            targetSets = targetSets,
                            instructions = instructions,
                            youtubeUrl = buildYoutubeSearchUrl(exName)
                        )
                    )
                }
            }

            if (drafts.isEmpty()) {
                Result.failure(IllegalStateException("AI returned zero exercises. Please try again."))
            } else {
                Result.success(
                    GeneratedProgramPreview(
                        programName = programName,
                        summary = summary,
                        exercises = drafts
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Program generation timed out", e)
            Result.failure(Exception("AI generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate Boditrax AI program", e)
            Result.failure(e)
        }
    }
}
