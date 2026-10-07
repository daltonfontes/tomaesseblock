package com.tomaesseblock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.domain.SpamCategory
import com.tomaesseblock.ui.LookupResult
import com.tomaesseblock.ui.MainViewModel
import com.tomaesseblock.ui.theme.Extrato
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LookupScreen(vm: MainViewModel) {
    val result by vm.lookup.collectAsState()
    LookupContent(
        result = result,
        onLookup = vm::lookup,
        onBlock = vm::blockFromLookup,
        onUnblock = vm::unblockFromLookup,
        onReport = vm::report,
        onClearReports = vm::clearReports,
        onAllow = vm::allowFromLookup,
        onRemoveAllowed = vm::removeAllowedFromLookup,
    )
}

/** Visual da busca de número, sem ViewModel (usado também nas capturas de tela). */
@Composable
fun LookupContent(
    result: LookupResult?,
    onLookup: (String) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onReport: (SpamCategory) -> Unit,
    onClearReports: () -> Unit,
    onAllow: () -> Unit,
    onRemoveAllowed: () -> Unit,
) {
    var query by remember { mutableStateOf(result?.number.orEmpty()) }
    var showReport by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle("Quem ligou?")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Número de telefone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onLookup(query) }),
            trailingIcon = {
                IconButton(onClick = { onLookup(query) }) { Icon(Icons.Filled.Search, contentDescription = "Buscar") }
            },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )

        result?.let { r ->
            ResultCard(r)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (r.isBlockedExact) {
                    OutlinedButton(onClick = onUnblock, modifier = Modifier.weight(1f)) { Text("Desbloquear") }
                } else {
                    Button(
                        onClick = onBlock,
                        colors = ButtonDefaults.buttonColors(containerColor = Extrato.colors.blocked, contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                    ) { Text("Bloquear") }
                }
                OutlinedButton(onClick = { showReport = true }, modifier = Modifier.weight(1f)) { Text("Denunciar") }
            }
            if (r.isAllowedExact) {
                TextButton(onClick = onRemoveAllowed, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Remover dos permitidos")
                }
            } else {
                TextButton(onClick = onAllow, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Sempre permitir este número")
                }
            }
            if (r.spam != null) {
                TextButton(onClick = onClearReports, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Não é spam (remover denúncias)")
                }
            }
        }
    }

    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onConfirm = {
                onReport(it)
                showReport = false
            },
        )
    }
}

@Composable
private fun ResultCard(r: LookupResult) {
    val blocked = r.decision is CallDecision.Block && !r.isAllowedExact
    val verdict = when {
        r.isAllowedExact -> "SEMPRE TOCA"
        r.decision is CallDecision.Block -> "SERIA BLOQUEADA"
        r.spam != null -> "TOCA, COM AVISO"
        else -> "TOCA"
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val dash = Extrato.colors.dash
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        HorizontalDivider(thickness = 2.dp, color = ink)
        Text(
            PhoneNumbers.format(r.number),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        LedgerLine("Contato", r.contactName ?: "não está na agenda", dash)
        LedgerLine(
            "Denúncias",
            r.spam?.let { "${it.reports} · ${it.topCategory.label.lowercase()}" } ?: "nenhuma",
            dash,
        )
        LedgerLine(
            "Suas listas",
            when {
                r.isBlockedExact -> "bloqueados"
                r.isAllowedExact -> "permitidos"
                else -> "nenhuma"
            },
            dash,
        )
        (r.decision as? CallDecision.Block)?.let { LedgerLine("Motivo", it.label, dash) }
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (blocked) Extrato.colors.blocked else ink)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Text("Resultado", color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f))
            Text(verdict, color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LedgerLine(label: String, value: String, dash: Color) {
    Row(Modifier.fillMaxWidth().dashedBottom(dash).padding(vertical = 10.dp)) {
        Text(label, color = Extrato.colors.pencil, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ReportDialog(onDismiss: () -> Unit, onConfirm: (SpamCategory) -> Unit) {
    var selected by remember { mutableStateOf(SpamCategory.TELEMARKETING) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Denunciar número") },
        text = {
            Column {
                SpamCategory.entries.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selected == category, onClick = { selected = category })
                        Text(category.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Denunciar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
