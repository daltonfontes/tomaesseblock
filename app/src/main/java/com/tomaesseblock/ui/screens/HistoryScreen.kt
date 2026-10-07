package com.tomaesseblock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.domain.SpamCategory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomaesseblock.data.CallEventEntity
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.ui.MainViewModel
import com.tomaesseblock.ui.theme.Extrato
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: MainViewModel, onOpenNumber: (String) -> Unit) {
    val history by vm.history.collectAsState()
    val rules by vm.rules.collectAsState()
    HistoryContent(
        history = history,
        rules = rules,
        onClear = vm::clearHistory,
        onOpenNumber = onOpenNumber,
        onBlock = vm::blockNumber,
        onUnblock = vm::unblockNumber,
        onAllow = vm::allowNumber,
        onRemoveAllowed = vm::removeAllowed,
        onReport = vm::reportNumber,
    )
}

/** Visual do histórico, sem ViewModel (usado também nas capturas de tela). */
@Composable
fun HistoryContent(
    history: List<CallEventEntity>,
    rules: List<BlockRule>,
    onClear: () -> Unit,
    onOpenNumber: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onAllow: (String) -> Unit,
    onRemoveAllowed: (String) -> Unit,
    onReport: (String, SpamCategory) -> Unit,
) {
    val blocked = rules.filter { it.type == RuleType.EXACT && it.action == RuleAction.BLOCK }.map { it.pattern }.toSet()
    val allowed = rules.filter { it.type == RuleType.EXACT && it.action == RuleAction.ALLOW }.map { it.pattern }.toSet()
    var reportFor by remember { mutableStateOf<String?>(null) }
    val formatter = remember { SimpleDateFormat("dd/MM\nHH:mm", Locale("pt", "BR")) }

    val dash = Extrato.colors.dash
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Registro") {
            if (history.isNotEmpty()) TextButton(onClick = onClear) { Text("LIMPAR") }
        }
        if (history.isEmpty()) {
            Text(
                "As chamadas analisadas pelo app aparecerão aqui.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            items(history, key = { it.id }) { event ->
                val suspicious = !event.blocked && event.label?.startsWith("Possível spam") == true
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashedBottom(dash)
                        .clickable(enabled = event.number.isNotEmpty()) { onOpenNumber(event.number) }
                        .padding(top = 10.dp, bottom = 6.dp),
                ) {
                    Text(
                        formatter.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Extrato.colors.pencil,
                        modifier = Modifier.width(64.dp).padding(top = 2.dp),
                    )
                    Column(Modifier.weight(1f).padding(top = 1.dp)) {
                        Text(PhoneNumbers.format(event.number), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            event.label?.takeIf { it.isNotBlank() } ?: if (event.blocked) "Bloqueada" else "Permitida",
                            style = MaterialTheme.typography.bodySmall,
                            color = Extrato.colors.pencil,
                        )
                    }
                    StatusTag(
                        text = when {
                            event.blocked -> "Bloq"
                            suspicious -> "Suspeita"
                            else -> "Tocou"
                        },
                        blocked = event.blocked || suspicious,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    if (event.number.isNotEmpty()) {
                        NumberActionsMenu(
                            isBlocked = event.number in blocked,
                            isAllowed = event.number in allowed,
                            onBlock = { onBlock(event.number) },
                            onUnblock = { onUnblock(event.number) },
                            onAllow = { onAllow(event.number) },
                            onRemoveAllowed = { onRemoveAllowed(event.number) },
                            onReport = { reportFor = event.number },
                        )
                    }
                }
            }
        }
    }

    reportFor?.let { number ->
        ReportDialog(
            onDismiss = { reportFor = null },
            onConfirm = { category ->
                onReport(number, category)
                reportFor = null
            },
        )
    }
}

/** Menu "⋮" de cada ligação: bloquear, desbloquear, sempre permitir e denunciar. */
@Composable
private fun NumberActionsMenu(
    isBlocked: Boolean,
    isAllowed: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onAllow: () -> Unit,
    onRemoveAllowed: () -> Unit,
    onReport: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Ações")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (isBlocked) {
                DropdownMenuItem(text = { Text("Desbloquear") }, onClick = { open = false; onUnblock() })
            } else {
                DropdownMenuItem(text = { Text("Bloquear") }, onClick = { open = false; onBlock() })
            }
            if (isAllowed) {
                DropdownMenuItem(text = { Text("Remover dos permitidos") }, onClick = { open = false; onRemoveAllowed() })
            } else {
                DropdownMenuItem(text = { Text("Sempre permitir") }, onClick = { open = false; onAllow() })
            }
            DropdownMenuItem(text = { Text("Denunciar") }, onClick = { open = false; onReport() })
        }
    }
}
