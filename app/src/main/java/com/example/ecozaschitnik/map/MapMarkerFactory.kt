package com.example.ecozaschitnik.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.ecozaschitnik.ui.ThemeManager

object MapMarkerFactory {

    private fun markerColor(context: Context): Int =
        if (ThemeManager.isNight(context)) 0xFFB7F36B.toInt() else 0xFF5E8C61.toInt()

    fun dotIcon(context: Context, selected: Boolean): Drawable {
        val density = context.resources.displayMetrics.density
        val sizeDp = if (selected) 20f else 14f
        val sizePx = sizeDp * density
        val glowPx = if (selected) 4f * density else 2f * density
        val canvasSize = (sizePx + glowPx * 2).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = canvasSize / 2f
        val cy = canvasSize / 2f
        val radius = sizePx / 2f

        if (selected) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(128, 183, 243, 107)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, radius + glowPx, glowPaint)
        } else {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(89, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy + density, radius, shadowPaint)
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = markerColor(context)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(cx, cy, radius - density * 0.5f, strokePaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun youAreHereIcon(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val sizePx = 12f * density
        val canvasSize = (sizePx + 6f * density).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = canvasSize / 2f
        val cy = canvasSize / 2f
        val radius = sizePx / 2f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(89, 51, 136, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3388FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(cx, cy, radius - density * 0.3f, strokePaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun clusterIcon(context: Context, count: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val sizePx = 28f * density
        val canvasSize = sizePx.toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = canvasSize / 2f
        val cy = canvasSize / 2f
        val radius = sizePx / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (ThemeManager.isNight(context)) {
                Color.argb(89, 127, 182, 63)
            } else {
                Color.argb(89, 94, 140, 97)
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, haloPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (ThemeManager.isNight(context)) 0xFF7FB63F.toInt() else 0xFF5E8C61.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 0.72f, fillPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (ThemeManager.isNight(context)) 0xFF1A1D1A.toInt() else Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 11f * density
            isFakeBoldText = true
        }
        val label = if (count > 999) "999+" else count.toString()
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
