package com.example.agendavalenciana.ios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import com.example.agendavalenciana.ios.resources.Res
import com.example.agendavalenciana.ios.resources.logo_lagenda_horizontal

/** Shared Android/iOS visual constants. The PNG is the ORIGINAL Android logo. */
internal val AgendaGold = Color(0xFFF4B31B)
internal val AgendaBlack = Color(0xFF191919)
internal val AgendaLavender = Color(0xFFE9DEF9)
internal val AgendaChrome = Color(0xFFF1F1F1)

@Composable
internal fun AgendaBrandLogo(modifier: Modifier = Modifier) {
    Image(painter = painterResource(Res.drawable.logo_lagenda_horizontal),
        contentDescription = "L'Agenda Cultural Valenciana",
        modifier = modifier, contentScale = ContentScale.Fit)
}

/** Draw icons locally: the iPhone doesn't need an emoji font or a native symbol fallback. */
@Composable
internal fun AgendaGlyph(name: String, modifier: Modifier = Modifier.size(24.dp), color: Color = AgendaBlack) {
    Canvas(modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun line(x: Float, y: Float, x2: Float, y2: Float, w: Float = 2f) =
            drawLine(color, p(x,y), p(x2,y2), strokeWidth = w * sx, cap = StrokeCap.Round)
        fun outlineCircle(x: Float, y: Float, r: Float, w: Float = 2f) =
            drawCircle(color, r * sx, p(x,y), style = Stroke(width = w * sx))
        fun outlineRect(x: Float, y: Float, w: Float, h: Float, sw: Float = 2f) =
            drawRoundRect(color, topLeft=p(x,y), size=Size(w*sx,h*sy),
                cornerRadius=androidx.compose.ui.geometry.CornerRadius(1.8f*sx), style=Stroke(sw*sx))
        when(name) {
            "plus" -> { line(12f,4f,12f,20f,2.7f); line(4f,12f,20f,12f,2.7f) }
            "edit" -> { outlineRect(3f,4f,18f,16f); line(7f,17f,16f,8f); line(14f,7f,17f,10f) }
            "campaign" -> { line(4f,10f,12f,7f); line(12f,7f,19f,4f); line(19f,4f,19f,17f); line(19f,17f,12f,14f); line(12f,14f,4f,14f); line(4f,10f,4f,14f); line(7f,15f,9f,21f) }
            "disc" -> { outlineCircle(12f,12f,9f); outlineCircle(12f,12f,2.3f) }
            "check" -> { outlineCircle(12f,12f,9f); line(7f,12f,11f,16f); line(11f,16f,18f,8f) }
            "close" -> { line(5f,5f,19f,19f,2.2f); line(19f,5f,5f,19f,2.2f) }
            "person" -> {
                outlineCircle(12f,7f,3.6f,2.2f)
                val path=Path().apply { moveTo(4f*sx,21f*sy); lineTo(4f*sx,18f*sy)
                    cubicTo(4f*sx,12f*sy,20f*sx,12f*sy,20f*sx,18f*sy); lineTo(20f*sx,21f*sy); close() }
                drawPath(path,color,style=Stroke(2.1f*sx))
            }
            "bell" -> {
                val path=Path().apply { moveTo(5f*sx,18f*sy); lineTo(7f*sx,15f*sy)
                    lineTo(7f*sx,10f*sy); cubicTo(7f*sx,3f*sy,17f*sx,3f*sy,17f*sx,10f*sy)
                    lineTo(17f*sx,15f*sy); lineTo(19f*sx,18f*sy); close() }
                drawPath(path,color,style=Stroke(2f*sx)); line(10f,21f,14f,21f)
            }
            "sliders" -> {
                line(3f,5f,21f,5f); line(3f,12f,21f,12f); line(3f,19f,21f,19f)
                drawCircle(Color.White,3.1f*sx,p(9f,5f)); outlineCircle(9f,5f,2.6f)
                drawCircle(Color.White,3.1f*sx,p(16f,12f)); outlineCircle(16f,12f,2.6f)
                drawCircle(Color.White,3.1f*sx,p(9f,19f)); outlineCircle(9f,19f,2.6f)
            }
            "bookmark" -> {
                val path=Path().apply { moveTo(6f*sx,3f*sy); lineTo(18f*sx,3f*sy)
                    lineTo(18f*sx,21f*sy); lineTo(12f*sx,17f*sy); lineTo(6f*sx,21f*sy); close() }
                drawPath(path,color,style=Stroke(2f*sx))
            }
            "home" -> { line(3f,11f,12f,3.5f); line(12f,3.5f,21f,11f)
                line(5f,10f,5f,21f); line(19f,10f,19f,21f); line(5f,21f,19f,21f) }
            "search" -> { outlineCircle(10f,10f,6f,2.2f); line(14.5f,14.5f,21f,21f,2.2f) }
            "clock" -> { outlineCircle(12f,12f,9f); line(12f,6f,12f,12f); line(12f,12f,16f,14f) }
            "calendar" -> { outlineRect(3f,5f,18f,16f); line(3f,10f,21f,10f); line(8f,2f,8f,7f); line(16f,2f,16f,7f) }
            "pin" -> { val path=Path().apply { moveTo(12f*sx,22f*sy)
                cubicTo(2f*sx,12f*sy,5f*sx,3f*sy,12f*sx,3f*sy)
                cubicTo(19f*sx,3f*sy,22f*sx,12f*sy,12f*sx,22f*sy); close() }
                drawPath(path,color,style=Stroke(2f*sx)); outlineCircle(12f,10f,2.4f)
            }
            "share" -> { line(7f,12f,17f,6f); line(7f,12f,17f,18f)
                outlineCircle(5f,12f,2.5f); outlineCircle(19f,5f,2.5f); outlineCircle(19f,19f,2.5f) }
            "ticket" -> { outlineRect(3f,6f,18f,12f); line(12f,7f,12f,9f); line(12f,12f,12f,14f); line(12f,17f,12f,18f) }
            "people" -> { outlineCircle(10f,7f,3f); outlineCircle(19f,9f,2.4f)
                line(3f,20f,3f,18f); line(3f,18f,6f,14.5f); line(6f,14.5f,14f,14.5f)
                line(14f,14.5f,17f,18f); line(17f,18f,17f,20f); line(17f,20f,3f,20f)
                line(18.5f,15f,21f,15f); line(21f,15f,22f,20f) }
            "star" -> {
                val path=Path().apply {
                    moveTo(12f*sx,2.5f*sy);lineTo(15f*sx,9f*sy);lineTo(22f*sx,9.5f*sy)
                    lineTo(16.5f*sx,14.4f*sy);lineTo(18.2f*sx,21.5f*sy)
                    lineTo(12f*sx,17.8f*sy);lineTo(5.8f*sx,21.5f*sy)
                    lineTo(7.5f*sx,14.4f*sy);lineTo(2f*sx,9.5f*sy)
                    lineTo(9f*sx,9f*sy);close()
                }
                drawPath(path,color,style=Stroke(1.8f*sx))
            }
            "info" -> { outlineCircle(12f,12f,9f); line(12f,11f,12f,17f); drawCircle(color,1.3f*sx,p(12f,7f)) }
            else -> outlineCircle(12f,12f,8f)
        }
    }
}

@Composable
internal fun AgendaTopAction(icon: String, color: Color = AgendaBlack, onClick: () -> Unit, badge: Int = 0) {
    Box(Modifier.size(41.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        AgendaGlyph(icon, Modifier.size(23.dp), color)
        if (badge > 0) Box(Modifier.align(Alignment.TopEnd).size(15.dp).background(Color(0xFFC42525),CircleShape),
            contentAlignment=Alignment.Center) {
            Text(if (badge>9) "9+" else badge.toString(), color=Color.White, fontSize=8.sp)
        }
    }
}
