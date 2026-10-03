package com.tomaesseblock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tomaesseblock.domain.BlockSettings
import com.tomaesseblock.ui.MainViewModel
import kotlin.math.roundToInt

const val PRIVACY_POLICY_URL = "https://daltonfontes.github.io/tomaesseblock/privacidade.html"

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    SettingsContent(
        s = settings,
        onUpdate = vm::updateSettings,
        onOpenPrivacyPolicy = {
            // Sem navegador instalado, simplesmente não abre.
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
        },
    )
}

/** Visual dos ajustes, sem ViewModel (usado também nas capturas de tela). */
@Composable
fun SettingsContent(
    s: BlockSettings,
    onUpdate: ((BlockSettings) -> BlockSettings) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle("Ajustes")

        SwitchRow("Bloqueio ativado", "Liga/desliga todo o bloqueio automático", s.blockingEnabled) { v ->
            onUpdate { it.copy(blockingEnabled = v) }
        }
        HorizontalDivider()
        SectionHeader("O que bloquear")
        SwitchRow("Telemarketing (0303)", "Prefixo obrigatório da Anatel para telemarketing", s.blockTelemarketing0303) { v ->
            onUpdate { it.copy(blockTelemarketing0303 = v) }
        }
        SwitchRow("Números denunciados", "Bloqueia números com denúncias de spam", s.blockReportedSpam) { v ->
            onUpdate { it.copy(blockReportedSpam = v) }
        }
        if (s.blockReportedSpam) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Bloquear a partir de ${s.spamThreshold} denúncia(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = s.spamThreshold.toFloat(),
                    onValueChange = { v -> onUpdate { it.copy(spamThreshold = v.roundToInt()) } },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }
        }
        SwitchRow("Números ocultos", "Chamadas sem identificação", s.blockHidden) { v ->
            onUpdate { it.copy(blockHidden = v) }
        }
        SwitchRow(
            "Quem não está nos contatos",
            "Modo rigoroso: só seus contatos conseguem ligar (requer permissão de contatos)",
            s.blockNotInContacts,
        ) { v -> onUpdate { it.copy(blockNotInContacts = v) } }

        HorizontalDivider()
        SectionHeader("Avisos")
        SwitchRow("Identificador de chamadas", "Notificação de possível spam para chamadas suspeitas que não foram bloqueadas", s.showCallerId) { v ->
            onUpdate { it.copy(showCallerId = v) }
        }
        SwitchRow("Notificar bloqueios", "Aviso a cada bloqueio. Desligado, a chamada só aparece no histórico do telefone", s.notifyBlocked) { v ->
            onUpdate { it.copy(notifyBlocked = v) }
        }

        HorizontalDivider()
        SectionHeader("Sobre")
        ListItem(
            headlineContent = { Text("Política de privacidade") },
            supportingContent = { Text("Nenhum dado sai do seu aparelho") },
            modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy),
        )
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
