package org.openstrap.app.ui.glow

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Shadow
import org.openstrap.app.ui.theme.Blends

/**
 * The app's name as its logo (docs/design/rebrand-unstrap.md): "un" in the heart red, "strap" in white, lowercase.
 * A faint glow round the red, as the Share board draws it. Placed on dark
 * ground only, where the red reads. A brand, not a word to translate.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, style: TextStyle = GlowType.storyBrand, strap: Color = Color.White) {
    val text = buildAnnotatedString {
        // The board's soft glow round the red, 0.25 em wide (Share board).
        withStyle(SpanStyle(color = Blends.Heart.first, shadow = Shadow(Blends.Heart.first.copy(alpha = 0.35f), blurRadius = style.fontSize.value * 0.25f))) { append("un") }
        withStyle(SpanStyle(color = strap)) { append("strap") }
    }
    Text(text, style = style, maxLines = 1, modifier = modifier)
}
