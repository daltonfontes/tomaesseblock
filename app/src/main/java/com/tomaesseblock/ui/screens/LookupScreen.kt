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
import com.tomaesseblock.ui.theme.Danger
import com.tomaesseblock.ui.theme.Safe
import com.tomaesseblock.ui.theme.Warning

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
        ScreenTitle("Buscar número")
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        result?.let { r ->
            ResultCard(r)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (r.isBlockedExact) {
                    OutlinedButton(onClick = onUnblock, modifier = Modifier.weight(1f)) { Text("Desbloquear") }
                } else {
                    Button(
                        onClick = onBlock,
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
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
    val (color, verdict) = when {
        r.isAllowedExact -> Safe to "Na sua lista de permitidos — sempre toca"
        r.decision is CallDecision.Block -> Danger to "Será bloqueado: ${r.decision.label}"
        r.spam != null -> Warning to "Suspeito — será identificado, mas não bloqueado"
        else -> Safe to "Sem denúncias — chamada permitida"
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(PhoneNumbers.format(r.number), style = MaterialTheme.typography.headlineSmall)
            r.identification?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = color) }
            Spacer(Modifier.size(8.dp))
            Text(verdict, style = MaterialTheme.typography.bodyMedium)
            r.spam?.let {
                Text(
                    "${it.reports} denúncia(s) • mais comum: ${it.topCategory.label}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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
