package org.openstrap.app.ui.glow

import org.openstrap.app.ui.theme.Greys
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.openstrap.app.ui.glow.draw.DotLayout
import org.openstrap.app.ui.glow.draw.LocalPlainNumbers
import org.openstrap.app.ui.glow.draw.plainBaseline
import org.openstrap.app.ui.glow.draw.plainNumberPaint
import org.openstrap.app.ui.glow.draw.plainText
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openstrap.app.ui.glow.draw.DotFont
import org.openstrap.app.ui.glow.draw.dotLayout
import org.openstrap.app.ui.glow.draw.drawDots
import org.openstrap.app.ui.glow.draw.DotSize
import org.openstrap.app.ui.theme.Ink

/* ------------------------------------------------------------------------------------------------ */
/* Dot-matrix numerals                                                                               */
/* ------------------------------------------------------------------------------------------------ */

/**
 * A number drawn as round dots, like an LED matrix (the "3.3" on the reference cards), in one of the six [DotSize]s
 * for where it sits. Digits, `.`, `,`, `:`, `-`, `−`, `+` and `%` are supported.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dot: DotSize,
    color: Color = Color.White,
    /** Shrinks the dots below [dot] where the number must fit a space (the live workout's arc), 0..1. */
    scale: Float = 1f,
) {
    // The shared dot geometry ([dotLayout]), as a widget's number draws it.
    val dotDp = (dot.dot * scale).value
    val box = dotLayout(text, dotDp, 1f)
    if (LocalPlainNumbers.current) {
        PlainNumber(text, box, color, modifier)
        return
    }
    Canvas(
        modifier
            .size(box.width.dp, box.height.dp)
            .semantics { contentDescription = text },
    ) {
        drawDots(dotLayout(text, dotDp, density), color)
    }
}

/** [text] as plain digits in [box]'s height and at most its width ([LocalPlainNumbers]). */
@Composable
private fun PlainNumber(text: String, box: DotLayout, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val shown = plainText(text)
    val paint = remember(context, shown, box.width, box.height, color, density) {
        plainNumberPaint(context, shown, box.height * density, box.width * density, color)
    }
    Canvas(
        modifier
            .size((paint.measureText(shown) / density).dp, box.height.dp)
            .semantics { contentDescription = text },
    ) {
        drawIntoCanvas { it.nativeCanvas.drawText(shown, 0f, plainBaseline(paint, size.height), paint) }
    }
}

/* ------------------------------------------------------------------------------------------------ */
/* Switch                                                                                            */
/* ------------------------------------------------------------------------------------------------ */

/**
 * The platform's switch (Material 3), in the app's colours: a white track with a black knob when on, [Greys.Control]
 * with a grey knob when off. Never a colour: a switch track is white when on, not a sport's (workout rules section 5,
 * guarded by HaloRulesTest).
 */
@Composable
fun GlowSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked, onCheckedChange, modifier, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.Black, checkedTrackColor = Color.White, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Ink.Tertiary, uncheckedTrackColor = Greys.Control, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** How wide a number of [digits] digits is in [dot] dots, so a space can be sized for the widest it will show. */
internal fun dotNumberWidth(digits: Int, dot: DotSize): Dp = dotTextWidth("0".repeat(digits), dot)

/** How wide [text] is in [dot] dots at full size, as [DotMatrixText] lays it out. */
internal fun dotTextWidth(text: String, dot: DotSize): Dp = (dot.dot + dot.gap) * DotFont.columns(text) - dot.gap

/** The [DotMatrixText] scale that fits [text] in [dot] dots into [room]: 1 when it fits already, never larger. */
internal fun dotFitScale(text: String, dot: DotSize, room: Dp): Float = (room / dotTextWidth(text, dot)).coerceAtMost(1f)

/**
 * A card's one dotted number with the word after it on the digits' baseline (spec 2.3), in Body at 72%: "42 min",
 * "78 bpm". [gap] is the room between them, which differs a little by card. [maxWidth] shrinks a long number to fit
 * (a share card's distance). A blank [unit] leaves the number alone. The word takes what the number leaves and wraps
 * there, so a long one ("bpm, live from your strap") stays on the card.
 */
@Composable
internal fun HeroValue(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    dot: DotSize = DotSize.Hero,
    gap: Dp = 8.dp,
    color: Color = Color.White,
    unitStyle: TextStyle = GlowType.body,
    maxWidth: Dp = Dp.Infinity,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        DotMatrixText(value, dot = dot, color = color, scale = dotFitScale(value, dot, maxWidth))
        if (unit.isNotBlank()) Text(
            unit, Modifier.padding(start = gap).weight(1f, fill = false),
            color = Ink.Secondary, style = unitStyle,
        )
    }
}
