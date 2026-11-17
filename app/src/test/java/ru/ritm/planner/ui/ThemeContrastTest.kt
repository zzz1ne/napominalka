package ru.ritm.planner.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    @Test fun extremeAndPastelColorsRemainReadableInBothThemes() {
        val backgrounds = listOf(Color.White, Color(0xFF090A0B), Color(0xFFF2F4F7), Color(0xFF191A1E))
        val colors = listOf(Color.Black, Color.White, Color(0xFFBEF264), Color(0xFFA7C7FF), Color(0xFF6C5CE7), Color.Red)
        for (background in backgrounds) for (color in colors) {
            assertTrue("Contrast must stay readable: $color on $background", contrastRatio(readableTone(color, background), background) >= 4.5f)
        }
    }
    @Test fun readableColorsAreNotChanged() {
        assertEquals(Color.Black, readableTone(Color.Black, Color.White))
        assertEquals(Color.White, readableTone(Color.White, Color.Black))
    }
}
