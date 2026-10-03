package com.tomaesseblock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.tomaesseblock.BuildConfig
import com.tomaesseblock.data.BackupCodec
import com.tomaesseblock.domain.SpamCategory
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
    // O usuário escolhe onde salvar/abrir o arquivo (Storage Access Framework); nada de permissão de armazenamento.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupCodec.MIME_TYPE),
    ) { uri -> uri?.let { vm.exportBackup(context.contentResolver, it) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importBackup(context.contentResolver, it) }
    }
    SettingsContent(
        s = settings,
        onUpdate = vm::updateSettings,
        onOpenPrivacyPolicy = {
            // Sem navegador instalado, simplesmente não abre.
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
        },
        onExport = { exportLauncher.launch("tomaesseblock-backup.json") },
        // Alguns gerenciadores de arquivos não marcam .json como application/json.
        onImport = { importLauncher.launch(arrayOf(BackupCodec.MIME_TYPE, "text/plain", "application/octet-stream")) },
        versionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    )
}

/** Visual dos ajustes, sem ViewModel (usado também nas capturas de tela). */
@Composable
fun SettingsContent(
    s: BlockSettings,
    onUpdate: ((BlockSettings) -> BlockSettings) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    versionLabel: String = "",
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
                Text("Categorias que bloqueiam", style = MaterialTheme.typography.bodyMedium)
                SpamCategory.entries.forEach { category ->
                    val checked = category in s.blockedSpamCategories
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox) { on ->
                                onUpdate {
                                    val set = if (on) it.blockedSpamCategories + category else it.blockedSpamCategories - category
                                    it.copy(blockedSpamCategories = set)
                                }
                            },
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(category.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    "Denúncias das categorias desmarcadas só identificam o número, sem bloquear.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        SwitchRow("Ligações internacionais", "Números de fora do Brasil (+1, +44, +62…), comuns em golpes", s.blockInternational) { v ->
            onUpdate { it.copy(blockInternational = v) }
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
        SectionHeader("Segurança")
        SwitchRow(
            "Deixar tocar se ligar de novo",
            "Se alguém barrado por fora dos contatos, internacional ou prefixo ligar de novo em até 5 minutos, " +
                "a segunda chamada toca. Spam raramente insiste assim; urgências sim.",
            s.allowRepeatedCalls,
        ) { v -> onUpdate { it.copy(allowRepeatedCalls = v) } }

        HorizontalDivider()
        SectionHeader("Avisos")
        SwitchRow("Identificador de chamadas", "Notificação de possível spam para chamadas suspeitas que não foram bloqueadas", s.showCallerId) { v ->
            onUpdate { it.copy(showCallerId = v) }
        }
        SwitchRow("Notificar bloqueios", "Aviso a cada bloqueio. Desligado, a chamada só aparece no histórico do telefone", s.notifyBlocked) { v ->
            onUpdate { it.copy(notifyBlocked = v) }
        }

        HorizontalDivider()
        SectionHeader("Backup")
        Text(
            "Salve suas listas de bloqueio, permitidos e denúncias num arquivo para levar a outro celular. " +
                "O backup automático do Android fica desligado para proteger esses dados.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Exportar") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Importar") }
        }

        HorizontalDivider()
        SectionHeader("Sobre")
        ListItem(
            headlineContent = { Text("Política de privacidade") },
            supportingContent = { Text("Nenhum dado sai do seu aparelho") },
            modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy),
        )
        if (versionLabel.isNotEmpty()) {
            ListItem(
                headlineContent = { Text("Versão") },
                supportingContent = { Text(versionLabel) },
            )
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
