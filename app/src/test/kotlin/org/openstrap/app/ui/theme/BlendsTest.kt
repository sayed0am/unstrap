package org.openstrap.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendsTest {
    @Test
    fun everyChartToneReadsOnDarkGlass() {
        Blends.all.forEach { (name, b) ->
            assertTrue("$name chart ${contrast(b.chart, ChartGlass)}", contrast(b.chart, ChartGlass) >= 4.5f)
            assertTrue("$name second ${contrast(b.chartSecond, ChartGlass)}", contrast(b.chartSecond, ChartGlass) >= 4.5f)
        }
    }

    @Test
    fun screenLightIsMutedAsTheMatrixBoardDraws() {
        // Matrix board: Recovery #349875, second stop #408C75.
        assertClose(Color(0xFF349875), Blends.Recovery.light)
        assertClose(Color(0xFF408C75), Blends.Recovery.lightSecond)
    }

    @Test
    fun whiteReadsOnEveryScreenLight() {
        // The light is drawn at 60% over the ground at full strength (Matrix board), with clear glass on top.
        Blends.all.forEach { (name, b) ->
            val lit = b.light.copy(alpha = 0.6f).compositeOver(Ground)
            val glass = Color.White.copy(alpha = CLEAR_GLASS_FILL).compositeOver(lit)
            assertTrue("$name ${contrast(Color.White, glass)}", contrast(Color.White, glass) >= 4.5f)
        }
    }

    private fun assertClose(want: Color, got: Color) {
        val w = want.toArgb(); val g = got.toArgb()
        for (shift in listOf(16, 8, 0)) {
            assertEquals("channel $shift of ${Integer.toHexString(g)}", (w shr shift) and 0xFF, (g shr shift) and 0xFF, 3)
        }
    }

    private fun assertEquals(msg: String, a: Int, b: Int, tol: Int) = assertTrue("$msg: $a vs $b", kotlin.math.abs(a - b) <= tol)
}
