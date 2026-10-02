package com.tomaesseblock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tomaesseblock.domain.AlertLevel
import com.tomaesseblock.overlay.CallerIdOverlay
import androidx.compose.ui.unit.dp
import com.tomaesseblock.ui.MainViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    val context = LocalContext.current
    var canDraw by remember { mutableStateOf(CallerIdOverlay.canDrawOverlays(context)) }
    LifecycleResumeEffect(Unit) {
        canDraw = CallerIdOverlay.canDrawOverlays(context)
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle("Ajustes")

        SwitchRow("Bloqueio ativado", "Liga/desliga todo o bloqueio automático", s.blockingEnabled) { v ->
            vm.updateSettings { it.copy(blockingEnabled = v) }
        }
        HorizontalDivider()
        SectionHeader("O que bloquear")
        SwitchRow("Telemarketing (0303)", "Prefixo obrigatório da Anatel para telemarketing", s.blockTelemarketing0303) { v ->
            vm.updateSettings { it.copy(blockTelemarketing0303 = v) }
        }
        SwitchRow("Números denunciados", "Bloqueia números com denúncias de spam", s.blockReportedSpam) { v ->
            vm.updateSettings { it.copy(blockReportedSpam = v) }
        }
        if (s.blockReportedSpam) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Bloquear a partir de ${s.spamThreshold} denúncia(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = s.spamThreshold.toFloat(),
                    onValueChange = { v -> vm.updateSettings { it.copy(spamThreshold = v.roundToInt()) } },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }
        }
        SwitchRow("Números ocultos", "Chamadas sem identificação", s.blockHidden) { v ->
            vm.updateSettings { it.copy(blockHidden = v) }
        }
        SwitchRow(
            "Quem não está nos contatos",
            "Modo rigoroso: só seus contatos conseguem ligar (requer permissão de contatos)",
            s.blockNotInContacts,
        ) { v -> vm.updateSettings { it.copy(blockNotInContacts = v) } }

        HorizontalDivider()
        SectionHeader("Avisos")
        SwitchRow("Identificador de chamadas", "Alerta de possível spam enquanto o telefone toca", s.showCallerId) { v ->
            vm.updateSettings { it.copy(showCallerId = v) }
        }
        SwitchRow(
            "Aviso por cima da chamada",
            if (canDraw) {
                "Mostra quem está ligando sobre a tela de chamada, com botão para bloquear"
            } else {
                "Toque para permitir \"Exibir sobre outros apps\""
            },
            s.showOverlay && canDraw,
        ) { v ->
            if (v && !canDraw) context.startActivity(overlayPermissionIntent(context))
            vm.updateSettings { it.copy(showOverlay = v) }
        }
        if (canDraw && s.showOverlay) {
            OutlinedButton(
                onClick = {
                    CallerIdOverlay.show(
                        context,
                        number = "11999998888",
                        identification = "Possível spam: Golpe / Fraude (3 denúncias)",
                        level = AlertLevel.DANGER,
                        preview = true,
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) { Text("Ver exemplo do aviso") }
        }
        SwitchRow("Notificar bloqueios", "Mostra uma notificação a cada chamada bloqueada", s.notifyBlocked) { v ->
            vm.updateSettings { it.copy(notifyBlocked = v) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
