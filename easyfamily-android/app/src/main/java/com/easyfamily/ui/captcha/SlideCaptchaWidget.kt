package com.easyfamily.ui.captcha

import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.easyfamily.data.repository.CaptchaRepository
import com.easyfamily.ui.theme.AppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SlideTrackPoint(val x: Int, val y: Int, val t: Int)

@Composable
fun SlideCaptchaWidget(
    onVerified: (captchaToken: String) -> Unit,
    captchaRepository: CaptchaRepository,
    modifier: Modifier = Modifier
) {
    var challengeId by remember { mutableStateOf("") }
    var targetX by remember { mutableStateOf(0) }
    var currentOffsetPx by remember { mutableStateOf(0f) }
    var trackWidthPx by remember { mutableStateOf(1f) }
    var status by remember { mutableStateOf("loading") }
    var info by remember { mutableStateOf("正在加载验证码...") }
    val tracks = remember { mutableStateListOf<SlideTrackPoint>() }
    var dragStartTime by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val knobSizeDp = 44.dp
    val knobSizePx = with(density) { knobSizeDp.toPx() }
    val puzzleSvgWidth = 320f

    fun currentSvgX(): Float {
        val maxDrag = trackWidthPx - knobSizePx
        return if (maxDrag > 0) (currentOffsetPx / maxDrag) * puzzleSvgWidth else 0f
    }

    fun loadChallenge() {
        status = "loading"
        info = "正在加载验证码..."
        currentOffsetPx = 0f
        tracks.clear()
        scope.launch {
            captchaRepository.initSlideCaptcha()
                .onSuccess { result ->
                    challengeId = result.challengeId
                    targetX = parseTargetXFromDataUrl(result.backgroundImageUrl)
                    status = "ready"
                    info = "向右拖动滑块完成验证"
                }
                .onFailure { e ->
                    status = "error"
                    info = "加载失败：${e.message ?: "unknown"}"
                }
        }
    }

    LaunchedEffect(Unit) { loadChallenge() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEEF3FF), Color(0xFFDCE7FF)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            val scale = size.width / puzzleSvgWidth
            if (status != "loading") {
                drawCircle(
                    color = Color(0xFFB9C7EA).copy(alpha = 0.75f),
                    radius = 20f * scale,
                    center = Offset(targetX * scale, size.height / 2f)
                )
            }
            if (status == "dragging" || status == "ready") {
                drawCircle(
                    color = Color(0xFF6E86C8),
                    radius = 20f * scale,
                    center = Offset(currentSvgX() * scale, size.height / 2f)
                )
            }
            if (status == "success") {
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = 0.85f),
                    radius = 20f * scale,
                    center = Offset(targetX * scale, size.height / 2f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(knobSizeDp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .background(Color(0xFFE8ECF4), shape = RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            if (status == "ready") {
                                dragStartTime = System.currentTimeMillis()
                                tracks.clear()
                                tracks.add(SlideTrackPoint(0, 0, 0))
                                status = "dragging"
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (status == "dragging") {
                                change.consume()
                                val maxDrag = trackWidthPx - knobSizePx
                                currentOffsetPx =
                                    (currentOffsetPx + dragAmount.x).coerceIn(0f, maxDrag)
                                val svgX = currentSvgX().toInt()
                                val t = (System.currentTimeMillis() - dragStartTime).toInt()
                                if (tracks.isEmpty() || t > tracks.last().t) {
                                    tracks.add(SlideTrackPoint(svgX, 0, t))
                                }
                            }
                        },
                        onDragEnd = {
                            if (status == "dragging") {
                                val totalTimeMs =
                                    (System.currentTimeMillis() - dragStartTime).toInt()
                                val finalSvgX = currentSvgX().toInt()
                                if (tracks.isEmpty() || totalTimeMs > tracks.last().t) {
                                    tracks.add(SlideTrackPoint(finalSvgX, 0, totalTimeMs))
                                }
                                status = "verifying"
                                info = "验证中..."
                                scope.launch {
                                    val trackMaps = tracks.map { p ->
                                        mapOf("x" to p.x, "y" to p.y, "t" to p.t)
                                    }
                                    captchaRepository.verifySlideCaptcha(
                                        challengeId = challengeId,
                                        offsetX = finalSvgX,
                                        totalTimeMs = totalTimeMs,
                                        tracks = trackMaps
                                    )
                                        .onSuccess { token ->
                                            status = "success"
                                            info = "验证通过"
                                            onVerified(token)
                                        }
                                        .onFailure {
                                            status = "error"
                                            info = "验证失败，请重试"
                                            delay(1500)
                                            loadChallenge()
                                        }
                                }
                            }
                        },
                        onDragCancel = {
                            if (status == "dragging") {
                                status = "ready"
                                currentOffsetPx = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentOffsetPx.roundToInt(), 0) }
                    .size(knobSizeDp)
                    .background(
                        color = when (status) {
                            "success" -> Color(0xFF4CAF50)
                            "error" -> Color(0xFFE53935)
                            "verifying" -> Color(0xFF9E9E9E)
                            else -> AppPalette.Coral
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (status) {
                        "success" -> "✓"
                        "verifying" -> "…"
                        else -> "→"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentOffsetPx < 10f && (status == "ready" || status == "loading")) {
                Text(
                    text = if (status == "loading") "正在加载..." else "向右拖动滑块",
                    modifier = Modifier.padding(start = 56.dp),
                    color = Color(0xFF9EA7BA),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(info, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)

        if (status == "error") {
            TextButton(onClick = { loadChallenge() }) {
                Text("重新加载", color = AppPalette.Coral)
            }
        }
    }
}

private fun parseTargetXFromDataUrl(dataUrl: String): Int {
    return try {
        val base64Part = dataUrl.substringAfter("base64,")
        val svgBytes = Base64.decode(base64Part, Base64.DEFAULT)
        val svg = String(svgBytes, Charsets.UTF_8)
        val match = Regex("cx='(\\d+)'").find(svg)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 160
    } catch (_: Exception) {
        160
    }
}
