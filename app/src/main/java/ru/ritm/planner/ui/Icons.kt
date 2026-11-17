package ru.ritm.planner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun Glyph(name: String, modifier: Modifier = Modifier, color: Color = Muted, label: String? = null) {
    val background = Background
    Canvas(modifier.semantics { if (label != null) contentDescription = label }) {
        val s = size.minDimension / 24f
        fun point(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, point(x, y), point(x2, y2), 1.7f * s, StrokeCap.Round)
        fun path(vararg p: Float) { drawPath(Path().apply { moveTo(p[0] * s, p[1] * s); for (i in 2 until p.size step 2) lineTo(p[i] * s, p[i + 1] * s) }, color, style = Stroke(1.7f * s, cap = StrokeCap.Round)) }
        when (name) {
            "plus" -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            "search" -> { drawCircle(color, 6.4f * s, point(10.5f, 10.5f), style = Stroke(1.7f * s)); line(15.5f, 15.5f, 20f, 20f) }
            "check" -> path(5f, 12f, 10f, 17f, 19f, 7f)
            "arrow" -> { line(5f, 12f, 19f, 12f); path(13f, 6f, 19f, 12f, 13f, 18f) }
            "clock" -> { drawCircle(color, 8.5f * s, point(12f, 12f), style = Stroke(1.7f * s)); path(12f, 7f, 12f, 12f, 15f, 14f) }
            "calendar" -> {
                drawRoundRect(color, point(4f, 5f), Size(16f * s, 16f * s), androidx.compose.ui.geometry.CornerRadius(3f * s), style = Stroke(1.7f * s))
                line(4f, 10f, 20f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
            }
            "bell" -> { path(5f, 17f, 7f, 14f, 7f, 9f, 8f, 6f, 12f, 4f, 16f, 6f, 17f, 9f, 17f, 14f, 19f, 17f, 5f, 17f); line(10f, 21f, 14f, 21f) }
            "settings" -> { listOf(6f, 12f, 18f).forEach { y -> line(4f, y, 20f, y) }; drawCircle(background, 2.6f*s, point(9f,6f)); drawCircle(color,2.6f*s,point(9f,6f),style=Stroke(1.7f*s)); drawCircle(background,2.6f*s,point(16f,12f)); drawCircle(color,2.6f*s,point(16f,12f),style=Stroke(1.7f*s)); drawCircle(background,2.6f*s,point(8f,18f)); drawCircle(color,2.6f*s,point(8f,18f),style=Stroke(1.7f*s)) }
            "leaf" -> { drawPath(Path().apply { moveTo(6f*s,18f*s); cubicTo(0f,8f*s,14f*s,2f*s,20f*s,4f*s); cubicTo(22f*s,16f*s,15f*s,22f*s,6f*s,18f*s) },color,style=Stroke(1.7f*s)); line(4f,21f,15f,10f) }
        }
    }
}
