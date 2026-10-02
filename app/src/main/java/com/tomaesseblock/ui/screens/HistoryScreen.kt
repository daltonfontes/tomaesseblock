package com.tomaesseblock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.ui.MainViewModel
import com.tomaesseblock.ui.theme.Danger
import com.tomaesseblock.ui.theme.Safe
import com.tomaesseblock.ui.theme.Warning
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel, onOpenNumber: (String) -> Unit) {
    val history by vm.history.collectAsState()
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle("Histórico", Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = vm::clearHistory, modifier = Modifier.padding(end = 8.dp)) { Text("Limpar") }
            }
        }
        if (history.isEmpty()) {
            Text(
                "As chamadas analisadas pelo app aparecerão aqui.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(history, key = { it.id }) { event ->
                val suspicious = !event.blocked && event.label?.startsWith("Possível spam") == true
                ListItem(
                    modifier = Modifier.clickable(enabled = event.number.isNotEmpty()) { onOpenNumber(event.number) },
                    leadingContent = {
                        when {
                            event.blocked -> Icon(Icons.Filled.Block, contentDescription = "Bloqueada", tint = Danger)
                            suspicious -> Icon(Icons.Filled.Warning, contentDescription = "Suspeita", tint = Warning)
                            else -> Icon(Icons.Filled.Call, contentDescription = "Permitida", tint = Safe)
                        }
                    },
                    headlineContent = { Text(PhoneNumbers.format(event.number)) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                if (event.blocked) "Bloqueada" else "Permitida",
                                event.label?.takeIf { it.isNotBlank() },
                            ).joinToString(" • "),
                        )
                    },
                    trailingContent = {
                        Text(formatter.format(Date(event.timestamp)), style = MaterialTheme.typography.labelSmall)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
