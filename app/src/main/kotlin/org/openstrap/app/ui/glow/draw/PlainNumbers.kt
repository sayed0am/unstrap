package org.openstrap.app.ui.glow.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import org.openstrap.app.R
import org.openstrap.app.presentation.Units

/*
 * The big numbers as plain digits, for whoever turns "Plain numbers" on: Funnel Display Light, tabular, in the box the
 * dots would take, so no layout moves. The app and the widgets both draw them here.
 */

/** Whether the big numbers are drawn as plain digits rather than dots, in the app and in a widget. */
// A setting every big number reads, many layers down in both Compose and Glance.
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage")
val LocalPlainNumbers = staticCompositionLocalOf { false }

/** How far a Funnel Display digit reaches over the baseline, and a round one under it, as shares of the text size. */
private const val DIGIT_TOP_EM = 0.685f
private const val OVERSHOOT_EM = 0.01f

/**
 * [text] as plain digits draw it: Funnel has no thin space, and the phone's stand-in all but closes the gap that groups
 * thousands, so Funnel's own space takes its place.
 */
fun plainText(text: String): String = text.replace(Units.DOT_GROUP_SPACE, ' ')

/** The baseline that puts [paint]'s digits, round ones too, inside a box [height] px high. */
fun plainBaseline(paint: Paint, height: Float): Float = height - paint.textSize * OVERSHOOT_EM

/** The paint that draws [text] ([plainText]) with digits [height] px high, shrunk where wider than [maxWidth] px. */
fun plainNumberPaint(context: Context, text: String, height: Float, maxWidth: Float, color: Color = Color.White): Paint =
    // Linear and subpixel, so the width follows the size exactly and shrinking to [maxWidth] lands inside it.
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.funnel_display)
        fontVariationSettings = "'wght' 300"
        fontFeatureSettings = "tnum"
        this.color = color.toArgb()
        textSize = height / (DIGIT_TOP_EM + OVERSHOOT_EM)
        val width = measureText(text)
        if (width > maxWidth) textSize *= maxWidth / width
    }

/**
 * [text] as plain digits on a bitmap no wider than [layout]'s dots and as high. A bitmap cuts off what hangs under the
 * baseline, so a number with a comma is drawn smaller, with room for its tail.
 */
fun renderPlainNumber(context: Context, text: String, layout: DotLayout, density: Float): android.graphics.Bitmap {
    val shown = plainText(text)
    val height = layout.height * density
    val maxWidth = layout.width * density
    var paint = plainNumberPaint(context, shown, height, maxWidth)
    val bounds = Rect().also { paint.getTextBounds(shown, 0, shown.length, it) }
    // Round figures overshoot the baseline by a hair; only a real tail needs the room.
    val tail = bounds.bottom.takeIf { it > height * TAIL_SHARE } ?: 0
    val digits = if (tail > 0) height * height / (height + tail) else height
    if (tail > 0) paint = plainNumberPaint(context, shown, digits, maxWidth)
    val baseline = if (tail > 0) digits else plainBaseline(paint, height)
    return renderToBitmap(paint.measureText(shown), height) { c: Canvas -> c.drawText(shown, 0f, baseline, paint) }
}

/** The most a figure may reach under the baseline, as a share of its height, before it counts as a tail. */
private const val TAIL_SHARE = 0.05f
