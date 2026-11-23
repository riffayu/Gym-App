package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class AchievementBragCardData(
    val categoryTitle: String,
    val headline: String,
    val athleteName: String,
    val programName: String,
    val stat1Label: String,
    val stat1Value: String,
    val stat2Label: String,
    val stat2Value: String,
    val stat3Label: String,
    val stat3Value: String,
    val shareMessage: String
)

object AchievementShareHelper {

    fun shareAchievement(context: Context, data: AchievementBragCardData) {
        val imageUri = runCatching {
            val bitmap = renderBragCardBitmap(data)
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val outFile = File(sharedDir, "liftlog_achievement.png")
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            }
            bitmap.recycle()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
        }.getOrNull()

        val sendIntent = if (imageUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, data.shareMessage)
                putExtra(Intent.EXTRA_SUBJECT, data.headline)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, data.shareMessage)
                putExtra(Intent.EXTRA_SUBJECT, data.headline)
            }
        }

        val chooser = Intent.createChooser(sendIntent, "Brag to Friends")
        context.startActivity(chooser)
    }

    private fun renderBragCardBitmap(data: AchievementBragCardData): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0B0E14"),
                    Color.parseColor("#1D130D"),
                    Color.parseColor("#0B0E14")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer border card
        val cardRect = RectF(48f, 48f, width - 48f, height - 48f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#131822")
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5A26")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardFill)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorder)

        // Accent pill
        val pillRect = RectF(104f, 116f, 520f, 178f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5A26")
        }
        canvas.drawRoundRect(pillRect, 20f, 20f, pillPaint)

        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(data.categoryTitle.uppercase(), 132f, 157f, pillText)

        // Brand / Athlete header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("LIFTLOG  •  ${data.athleteName.uppercase()}", 104f, 260f, brandPaint)

        // Main Headline (wrap manually if needed)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F7FA")
            textSize = 72f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val lines = wrapText(data.headline, titlePaint, width - 208f)
        var yPos = 360f
        lines.take(3).forEach { line ->
            canvas.drawText(line, 104f, yPos, titlePaint)
            yPos += 86f
        }

        // Program subhead
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22D3EE")
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(data.programName, 104f, yPos + 16f, subPaint)

        // 3 KPI Stat Cards
        val statsTop = 670f
        val statItems = listOf(
            data.stat1Label to data.stat1Value,
            data.stat2Label to data.stat2Value,
            data.stat3Label to data.stat3Value
        )
        val statCardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C2331")
        }
        val statCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B3548")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val statValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 54f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        statItems.forEachIndexed { index, (label, value) ->
            val top = statsTop + index * 175f
            val rect = RectF(104f, top, width - 104f, top + 148f)
            canvas.drawRoundRect(rect, 28f, 28f, statCardFill)
            canvas.drawRoundRect(rect, 28f, 28f, statCardBorder)
            canvas.drawText(label.uppercase(), 144f, top + 52f, statLabelPaint)
            canvas.drawText(value, 144f, top + 114f, statValuePaint)
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        canvas.drawText("DEC 31 SCAN PUSH  •  LOGGED IN LIFTLOG", 104f, height - 110f, footerPaint)

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }
}
