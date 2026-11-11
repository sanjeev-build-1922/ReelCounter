package com.nothing.reelcounter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothing.reelcounter.ui.theme.NothingDotUnlit
import com.nothing.reelcounter.ui.theme.NothingWhite
import java.util.Locale

/**
 * Authentic 5x7 dot-matrix LED number renderer inspired by the Nothing OS glyph/dot-matrix
 * typography. Renders zero-padded digits using crisp circular LED nodes on an unlit grid.
 */
@Composable
fun DotMatrixNumber(
    value: Int,
    minDigits: Int = 3,
    dotSize: Dp = 6.dp,
    dotSpacing: Dp = 3.dp,
    digitSpacing: Dp = 14.dp,
    activeColor: Color = NothingWhite,
    inactiveColor: Color = NothingDotUnlit,
    modifier: Modifier = Modifier
) {
    val formatted = String.format(Locale.US, "%0${minDigits}d", value.coerceAtLeast(0))

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(digitSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        formatted.forEach { char ->
            DotMatrixGlyph(
                char = char,
                dotSize = dotSize,
                dotSpacing = dotSpacing,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    }
}

@Composable
fun DotMatrixGlyph(
    char: Char,
    dotSize: Dp,
    dotSpacing: Dp,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val matrix = DIGIT_PATTERNS[char] ?: DIGIT_PATTERNS['0']!!
    val cols = 5
    val rows = 7

    val totalWidth = (dotSize * cols) + (dotSpacing * (cols - 1))
    val totalHeight = (dotSize * rows) + (dotSpacing * (rows - 1))

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(totalHeight)
    ) {
        val radius = dotSize.toPx() / 2f
        val step = dotSize.toPx() + dotSpacing.toPx()

        for (r in 0 until rows) {
            val rowMask = matrix[r]
            for (c in 0 until cols) {
                val isLit = rowMask[c] == '1'
                val cx = radius + c * step
                val cy = radius + r * step
                drawCircle(
                    color = if (isLit) activeColor else inactiveColor,
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Draws a subtle industrial dot-matrix background grid inside any container.
 */
@Composable
fun DotMatrixProgressBar(
    progress: Float,
    totalColumns: Int = 24,
    activeColor: Color = NothingWhite,
    inactiveColor: Color = NothingDotUnlit,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val litColumns = (clamped * totalColumns).toInt()

    Canvas(
        modifier = modifier.height(14.dp)
    ) {
        val rows = 2
        val dotRadius = 2.4.dp.toPx()
        val horizontalSpacing = size.width / totalColumns.toFloat()
        val verticalSpacing = size.height / rows.toFloat()

        for (c in 0 until totalColumns) {
            val isLit = c < litColumns
            for (r in 0 until rows) {
                val cx = (c * horizontalSpacing) + (horizontalSpacing / 2f)
                val cy = (r * verticalSpacing) + (verticalSpacing / 2f)
                drawCircle(
                    color = if (isLit) activeColor else inactiveColor,
                    radius = dotRadius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

private val DIGIT_PATTERNS: Map<Char, List<String>> = mapOf(
    '0' to listOf(
        "01110",
        "10001",
        "10011",
        "10101",
        "11001",
        "10001",
        "01110"
    ),
    '1' to listOf(
        "00100",
        "01100",
        "00100",
        "00100",
        "00100",
        "00100",
        "01110"
    ),
    '2' to listOf(
        "01110",
        "10001",
        "00001",
        "00110",
        "01000",
        "10000",
        "11111"
    ),
    '3' to listOf(
        "11110",
        "00001",
        "00001",
        "01110",
        "00001",
        "00001",
        "11110"
    ),
    '4' to listOf(
        "00010",
        "00110",
        "01010",
        "10010",
        "11111",
        "00010",
        "00010"
    ),
    '5' to listOf(
        "11111",
        "10000",
        "11110",
        "00001",
        "00001",
        "10001",
        "01110"
    ),
    '6' to listOf(
        "00110",
        "01000",
        "10000",
        "11110",
        "10001",
        "10001",
        "01110"
    ),
    '7' to listOf(
        "11111",
        "00001",
        "00010",
        "00100",
        "01000",
        "01000",
        "01000"
    ),
    '8' to listOf(
        "01110",
        "10001",
        "10001",
        "01110",
        "10001",
        "10001",
        "01110"
    ),
    '9' to listOf(
        "01110",
        "10001",
        "10001",
        "01111",
        "00001",
        "00010",
        "01100"
    )
)
