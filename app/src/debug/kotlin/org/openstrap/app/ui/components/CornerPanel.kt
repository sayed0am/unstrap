package org.openstrap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openstrap.app.ui.theme.CornerTuning
import kotlin.math.roundToInt

private const val RADIUS_MAX = 48f
private const val SMOOTHING_MAX = 100f
private const val PANEL_ALPHA = 0.88f
/** Smoothing moves in fives. */
private const val SMOOTHING_STEPS = 19

/**
 * Debug only: two sliders over the real screens for the card corner radius and the squircle smoothing
 * ([CornerTuning]). Shown with `adb shell am start -n org.openstrap.app/.MainActivity --ez corners true`.
 */
@Composable
@Suppress("ModifierMissing", "ComposeModifierMissing") // an overlay over the whole app, placed by itself
fun CornerPanel() {
    if (!CornerTuning.panel) return
    var top by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp, vertical = if (top) 8.dp else 96.dp)) {
        Column(
            Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = PANEL_ALPHA), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Corners", color = Color.White, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { CornerTuning.reset() }) { Text("Reset") }
                TextButton(onClick = { top = !top }) { Text(if (top) "Down" else "Up") }
                TextButton(onClick = { CornerTuning.panel = false }) { Text("Close") }
            }
            val radius = CornerTuning.cardRadius
            Tuner("Card radius", "${radius.roundToInt()} dp", radius, 0f..RADIUS_MAX, RADIUS_MAX.toInt() - 1) {
                CornerTuning.cardRadius = it.roundToInt().toFloat()
            }
            val smoothing = CornerTuning.smoothing
            Tuner("Smoothing", "$smoothing", smoothing.toFloat(), 0f..SMOOTHING_MAX, SMOOTHING_STEPS) {
                CornerTuning.smoothing = it.roundToInt()
            }
        }
    }
}

@Composable
private fun Tuner(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.width(96.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 16.sp)
        }
        Slider(
            value = current, onValueChange = onChange, valueRange = range, steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
        )
    }
}
