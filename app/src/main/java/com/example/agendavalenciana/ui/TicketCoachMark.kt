package com.example.agendavalenciana.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal data class TicketHelp(val title: String, val body: String)

/**
 * Tutor contextual real: deixa il·luminat el camp al qual fa referència el text.
 * El tel es retira amb «Entés» per a deixar interactuar lliurement amb el control.
 * Es pot ometre i reiniciar sense modificar dades ni publicar res.
 */
@Composable
internal fun TicketCoachMark(
    targetInRoot: Rect?,
    help: TicketHelp,
    index: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    var overlayRoot by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().onGloballyPositioned { overlayRoot = it.boundsInRoot().topLeft }
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val pad = with(density) { 7.dp.toPx() }
        val rect = targetInRoot?.let {
            Rect(
                (it.left - overlayRoot.x - pad).coerceIn(0f, w),
                (it.top - overlayRoot.y - pad).coerceIn(0f, h),
                (it.right - overlayRoot.x + pad).coerceIn(0f, w),
                (it.bottom - overlayRoot.y + pad).coerceIn(0f, h)
            )
        }?.takeIf { it.right > it.left && it.bottom > it.top }
        Canvas(Modifier.fillMaxSize()) {
            val dim = Color.Black.copy(alpha = .78f)
            if (rect == null) drawRect(dim)
            else {
                drawRect(dim, topLeft = Offset.Zero, size = Size(w, rect.top))
                drawRect(dim, topLeft = Offset(0f, rect.bottom), size = Size(w, h - rect.bottom))
                drawRect(dim, topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
                drawRect(dim, topLeft = Offset(rect.right, rect.top), size = Size(w - rect.right, rect.height))
                drawRoundRect(Color.White, topLeft = rect.topLeft, size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()))
            }
        }
        val inset = with(density) { 12.dp.toPx() }
        val bubbleWidthPx = (w - with(density) { 32.dp.toPx() }).coerceAtMost(with(density) { 390.dp.toPx() })
        val bubbleHeightPx = with(density) { 216.dp.toPx() }
        val below = rect == null || rect.bottom + bubbleHeightPx + with(density) { 18.dp.toPx() } < h
        val topPx = if (below) ((rect?.bottom ?: with(density) { 60.dp.toPx() }) + inset).coerceIn(0f, (h - bubbleHeightPx).coerceAtLeast(0f))
                    else ((rect?.top ?: 0f) - bubbleHeightPx - with(density) { 16.dp.toPx() }).coerceAtLeast(8f)
        val leftPx = ((rect?.center?.x ?: w / 2f) - bubbleWidthPx / 2f)
            .coerceIn(inset, (w - bubbleWidthPx - inset).coerceAtLeast(inset))
        Box(Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { })
        Column(
            Modifier.offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                .width(with(density) { bubbleWidthPx.toDp() }),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (below && rect != null) Text("↑", color = Color.White, fontSize = 21.sp)
            Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 11.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("AJUDA GUIADA · ${index + 1}/$total", fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(help.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(help.body, fontSize = 13.sp, lineHeight = 19.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onSkip) { Text("Saltar ajuda") }
                        Button(onClick = onNext) { Text(if (index + 1 == total) "Entés" else "Següent") }
                    }
                }
            }
            if (!below && rect != null) Text("↓", color = Color.White, fontSize = 21.sp)
        }
    }
}
