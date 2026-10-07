package com.tomaesseblock.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomaesseblock.ui.theme.Extrato

/** Título da tela em caixa-alta com a régua grossa embaixo, como o cabeçalho de um extrato. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 36.sp),
                modifier = Modifier.weight(1f).padding(bottom = 6.dp),
            )
            action?.invoke()
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Rótulo pequeno de seção, em caixa-alta e cor de lápis, com a régua fina embaixo. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Extrato.colors.pencil,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Etiqueta de resultado: BLOQ em vermelho, TOCOU/OK em tinta. */
@Composable
fun StatusTag(text: String, blocked: Boolean, modifier: Modifier = Modifier) {
    val color = if (blocked) Extrato.colors.blocked else MaterialTheme.colorScheme.onSurface
    Text(
        text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier
            .border(1.5.dp, color)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** Linha tracejada embaixo de cada item, como as divisas de um extrato. */
fun Modifier.dashedBottom(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )
}
