package com.example.v2rayng.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animation progress values
    val strokeProgress = remember { Animatable(0f) }
    val glowOpacity = remember { Animatable(0f) }
    val titleBlurAndOffset = remember { Animatable(1f) } // 1 to 0
    val progressFill = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Draw SVG stroke paths
        launch {
            delay(250)
            strokeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing) // Draw lock, shield, letter A
            )
        }
        // Glow effect
        launch {
            delay(1050)
            glowOpacity.animateTo(targetValue = 1f, animationSpec = tween(500))
            glowOpacity.animateTo(targetValue = 0.65f, animationSpec = tween(400))
        }
        // Title and Subtitle blur & offset (mapped from 1 to 0)
        launch {
            delay(1000)
            titleBlurAndOffset.animateTo(targetValue = 0f, animationSpec = tween(850, easing = FastOutSlowInEasing))
        }
        // Progress bar at the bottom
        launch {
            delay(200)
            progressFill.animateTo(targetValue = 1f, animationSpec = tween(2400, easing = FastOutSlowInEasing))
        }

        delay(3300.milliseconds) // Total splash duration
        onSplashFinished()
    }

    val primaryCyan = Color(0xFF4EF2E8)
    val secondaryBlue = Color(0xFF60A5FA)
    val accentPurple = Color(0xFF9D7BF7)

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(primaryCyan, secondaryBlue, accentPurple)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09080F)), // Aether Deep Dark Bg
        contentAlignment = Alignment.Center
    ) {
        // Soft background glow
        Box(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .alpha(glowOpacity.value)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentPurple.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Vector Draw Tile
            Box(
                modifier = Modifier
                    .padding(bottom = 28.dp)
                    .size(92.dp)
                    .shadow(18.dp, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF322459), Color(0xFF231840), Color(0xFF1A1230))
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(56.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Mapping coordinates from 100x100 SVG to local size
                    fun rx(x: Float) = (x / 100f) * w
                    fun ry(y: Float) = (y / 100f) * h

                    val currentProgress = strokeProgress.value

                    // Shield background (static dark fill)
                    if (currentProgress > 0.5f) {
                        val shieldPath = Path().apply {
                            moveTo(rx(50f), ry(26f))
                            lineTo(rx(76f), ry(35.5f))
                            lineTo(rx(76f), ry(54.5f))
                            cubicTo(rx(76f), ry(72f), rx(65f), ry(84.5f), rx(50f), ry(91f))
                            cubicTo(rx(35f), ry(84.5f), rx(24f), ry(72f), rx(24f), ry(54.5f))
                            lineTo(rx(24f), ry(35.5f))
                            close()
                        }
                        drawPath(shieldPath, color = Color(0xFF130E24), alpha = ((currentProgress - 0.5f) * 2f).coerceIn(0f, 1f))
                    }

                    // Stroke parameters
                    val stroke = Stroke(
                        width = (5.5f / 100f) * w,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    // 1. Shackle (draws from 0 to 0.4 progress)
                    val shackleP = (currentProgress * 2.5f).coerceIn(0f, 1f)
                    if (shackleP > 0) {
                        val shacklePath = Path().apply {
                            moveTo(rx(36f), ry(33f))
                            lineTo(rx(36f), ry(24f))
                            cubicTo(rx(36f), ry(16.268f), rx(42.268f), ry(10f), rx(50f), ry(10f))
                            cubicTo(rx(57.732f), ry(10f), rx(64f), ry(16.268f), rx(64f), ry(24f))
                            lineTo(rx(64f), ry(33f))
                        }

                        val measure = PathMeasure()
                        measure.setPath(shacklePath, false)
                        val drawnPath = Path()
                        measure.getSegment(0f, measure.length * shackleP, drawnPath, true)
                        drawPath(drawnPath, gradientBrush, style = stroke)
                    }

                    // 2. Shield Outline (draws from 0.3 to 0.7 progress)
                    val shieldP = ((currentProgress - 0.3f) * 2.5f).coerceIn(0f, 1f)
                    if (shieldP > 0) {
                        val shieldPath = Path().apply {
                            moveTo(rx(50f), ry(26f))
                            lineTo(rx(76f), ry(35.5f))
                            lineTo(rx(76f), ry(54.5f))
                            cubicTo(rx(76f), ry(72f), rx(65f), ry(84.5f), rx(50f), ry(91f))
                            cubicTo(rx(35f), ry(84.5f), rx(24f), ry(72f), rx(24f), ry(54.5f))
                            lineTo(rx(24f), ry(35.5f))
                            close()
                        }
                        val measure = PathMeasure()
                        measure.setPath(shieldPath, false)
                        val drawnPath = Path()
                        measure.getSegment(0f, measure.length * shieldP, drawnPath, true)
                        drawPath(drawnPath, gradientBrush, style = stroke)
                    }

                    // 3. Letter A (draws from 0.6 to 1.0 progress)
                    val letterP = ((currentProgress - 0.6f) * 2.5f).coerceIn(0f, 1f)
                    if (letterP > 0) {
                        val letterPath1 = Path().apply {
                            moveTo(rx(38f), ry(71f))
                            lineTo(rx(50f), ry(42.5f))
                            lineTo(rx(62f), ry(71f))
                        }
                        val measure1 = PathMeasure()
                        measure1.setPath(letterPath1, false)
                        val drawnPath1 = Path()
                        measure1.getSegment(0f, measure1.length * letterP, drawnPath1, true)
                        drawPath(drawnPath1, gradientBrush, style = stroke)

                        val letterPath2 = Path().apply {
                            moveTo(rx(41.8f), ry(63f))
                            lineTo(rx(58.2f), ry(63f))
                        }
                        val measure2 = PathMeasure()
                        measure2.setPath(letterPath2, false)
                        val drawnPath2 = Path()
                        measure2.getSegment(0f, measure2.length * letterP, drawnPath2, true)
                        drawPath(drawnPath2, gradientBrush, style = stroke)
                    }
                }
            }

            // Typography
            Text(
                text = "AETHER",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = Color(0xFFCBB7FF),
                modifier = Modifier
                    .alpha(1f - titleBlurAndOffset.value)
                    .offset(y = (titleBlurAndOffset.value * 14).dp)
            )

            Text(
                text = "Безопасное и конфиденциальное соединение",
                fontSize = 11.sp,
                color = Color(0xFF857D99),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .alpha(1f - titleBlurAndOffset.value)
                    .offset(y = (titleBlurAndOffset.value * 10).dp)
            )
        }

        // Bottom Progress Thread
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFill.value)
                    .background(gradientBrush)
            )
        }
    }
}