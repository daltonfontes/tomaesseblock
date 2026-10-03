package com.tomaesseblock.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.ui.MainViewModel

@Composable
fun BlockListScreen(vm: MainViewModel) {
    val rules by vm.rules.collectAsState()
    BlockListContent(rules = rules, onRemove = vm::removeRule, onAdd = vm::addRule)
}

/** Visual das listas de bloqueio e de permitidos, sem ViewModel (usado também nas capturas de tela). */
@Composable
fun BlockListContent(
    rules: List<BlockRule>,
    onRemove: (BlockRule) -> Unit,
    onAdd: (number: String, type: RuleType, label: String, action: RuleAction) -> Unit,
    initialAction: RuleAction = RuleAction.BLOCK,
) {
    var showAdd by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(initialAction) }
    val visible = rules.filter { it.action == action }
    val blocking = action == RuleAction.BLOCK

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(if (blocking) "Lista de bloqueio" else "Sempre permitir")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                RuleAction.entries.forEachIndexed { index, a ->
                    SegmentedButton(
                        selected = action == a,
                        onClick = { action = a },
                        shape = SegmentedButtonDefaults.itemShape(index, RuleAction.entries.size),
                    ) {
                        val count = rules.count { it.action == a }
                        Text("${if (a == RuleAction.BLOCK) "Bloqueados" else "Permitidos"} ($count)")
                    }
                }
            }
            if (visible.isEmpty()) {
                Text(
                    if (blocking) {
                        "Nenhum número bloqueado ainda.\nToque em \"Adicionar\" para bloquear um número ou um " +
                            "prefixo (ex.: 0303 para todo telemarketing, ou um DDD inteiro)."
                    } else {
                        "Números aqui sempre tocam, mesmo com bloqueios ligados — útil para quem não está nos " +
                            "seus contatos, como médico, escola ou entregas."
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { rule ->
                    ListItem(
                        headlineContent = {
                            Text(
                                if (rule.type == RuleType.PREFIX) {
                                    "Começa com ${rule.pattern}"
                                } else {
                                    PhoneNumbers.format(rule.pattern)
                                },
                            )
                        },
                        supportingContent = {
                            Text(
                                listOf(
                                    if (rule.type == RuleType.PREFIX) "Prefixo" else "Número",
                                    rule.label,
                                ).filter { it.isNotBlank() }.joinToString(" • "),
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onRemove(rule) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remover")
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Adicionar") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (showAdd) {
        AddRuleDialog(
            action = action,
            onDismiss = { showAdd = false },
            onConfirm = { number, type, label ->
                onAdd(number, type, label, action)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AddRuleDialog(
    action: RuleAction,
    onDismiss: () -> Unit,
    onConfirm: (String, RuleType, String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.EXACT) }
    val verb = if (action == RuleAction.BLOCK) "Bloquear" else "Sempre permitir"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(verb) },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RuleType.entries.forEachIndexed { index, t ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(index, RuleType.entries.size),
                        ) { Text(if (t == RuleType.EXACT) "Número" else "Prefixo") }
                    }
                }
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(if (type == RuleType.EXACT) "Número" else "Prefixo (ex.: 0303)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (type == RuleType.PREFIX) {
                    Row(Modifier.padding(top = 8.dp)) {
                        Text(
                            "O prefixo é comparado com o número sem +55 e sem código de operadora. " +
                                "Ex.: \"11\" vale para todo o DDD 11.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(number, type, label) },
                enabled = number.any { it.isDigit() },
            ) { Text(verb) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
