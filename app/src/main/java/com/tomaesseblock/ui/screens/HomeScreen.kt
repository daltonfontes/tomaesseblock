package com.tomaesseblock.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tomaesseblock.ui.MainViewModel
import com.tomaesseblock.ui.theme.Extrato

private fun Context.holdsScreeningRole(): Boolean =
    getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
fun HomeScreen(vm: MainViewModel, onOpenLookup: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val total by vm.blockedTotal.collectAsState()
    val today by vm.blockedToday.collectAsState()
    val reported by vm.reportedNumbers.collectAsState()

    var hasRole by remember { mutableStateOf(context.holdsScreeningRole()) }
    var hasContacts by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CONTACTS)) }
    var hasNotifications by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    // Reavalia ao voltar de telas do sistema.
    LifecycleResumeEffect(Unit) {
        hasRole = context.holdsScreeningRole()
        hasContacts = context.hasPermission(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
        onPauseOrDispose { }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasRole = context.holdsScreeningRole()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasContacts = context.hasPermission(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showDisclosure by remember { mutableStateOf(false) }
    if (showDisclosure) {
        PermissionDisclosureDialog(
            onAccept = {
                showDisclosure = false
                val perms = buildList {
                    add(Manifest.permission.READ_CONTACTS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(perms.toTypedArray())
            },
            onDismiss = { showDisclosure = false },
        )
    }

    HomeContent(
        blockingEnabled = settings.blockingEnabled,
        today = today,
        total = total,
        reported = reported,
        hasRole = hasRole,
        hasContacts = hasContacts,
        hasNotifications = hasNotifications,
        onActivateRole = {
            val rm = context.getSystemService(RoleManager::class.java)
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        },
        // Explica o uso dos dados antes do pedido do sistema (exigência do Google Play).
        onRequestPermissions = { showDisclosure = true },
        onOpenLookup = onOpenLookup,
    )
}

/** Visual da tela inicial, sem dependências do sistema (usado também nas capturas de tela). */
@Composable
fun HomeContent(
    blockingEnabled: Boolean,
    today: Int,
    total: Int,
    reported: Int,
    hasRole: Boolean,
    hasContacts: Boolean,
    hasNotifications: Boolean,
    onActivateRole: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenLookup: () -> Unit,
) {
    val protectedNow = hasRole && blockingEnabled
    val ink = MaterialTheme.colorScheme.onSurface

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        ScreenTitle("Hoje")

        // Situação do filtro: uma linha do extrato com a etiqueta à direita.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("FILTRO DE CHAMADAS", style = MaterialTheme.typography.labelSmall, color = Extrato.colors.pencil)
                Text(
                    when {
                        !hasRole -> "Ainda não ativado"
                        !blockingEnabled -> "Pausado — tudo toca"
                        else -> "Recusando em silêncio"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            StatusTag(
                text = when {
                    !hasRole -> "Inativo"
                    !blockingEnabled -> "Pausado"
                    else -> "Ligado"
                },
                blocked = !protectedNow,
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = ink)

        if (!hasRole) {
            Button(
                onClick = onActivateRole,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp).heightIn(min = 52.dp),
            ) { Text("ATIVAR BLOQUEIO DE CHAMADAS") }
        }

        if (!hasContacts || !hasNotifications) {
            OutlinedButton(
                onClick = onRequestPermissions,
                border = BorderStroke(1.5.dp, ink),
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp).heightIn(min = 48.dp),
            ) {
                Text(
                    buildString {
                        append("PERMITIR ")
                        append(
                            listOfNotNull(
                                "CONTATOS".takeIf { !hasContacts },
                                "NOTIFICAÇÕES".takeIf { !hasNotifications },
                            ).joinToString(" E "),
                        )
                    },
                )
            }
        }

        // Números do dia, em células separadas por réguas.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = if (hasRole && hasContacts && hasNotifications) 0.dp else 16.dp)
                .height(IntrinsicSize.Min),
        ) {
            Stat("bloq. hoje", today.toString(), highlight = true, modifier = Modifier.weight(1f))
            VerticalDivider(thickness = 1.dp, color = ink)
            Stat("no total", total.toString(), modifier = Modifier.weight(1f).padding(start = 12.dp))
            VerticalDivider(thickness = 1.dp, color = ink)
            Stat("denunciados", reported.toString(), modifier = Modifier.weight(1f).padding(start = 12.dp))
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = ink)

        SectionLabel("Quem ligou?")
        Text(
            "Consulte um número para ver denúncias, bloquear ou sempre permitir.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
        )
        OutlinedButton(
            onClick = onOpenLookup,
            border = BorderStroke(1.5.dp, ink),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp).heightIn(min = 48.dp),
        ) { Text("BUSCAR NÚMERO →") }
    }
}

/** Divulgação clara de como os contatos são usados, mostrada antes do pedido de permissão. */
@Composable
private fun PermissionDisclosureDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Por que pedimos essas permissões") },
        text = {
            Text(
                "Contatos: usamos a sua agenda apenas para conferir se quem está ligando é um contato, " +
                    "para nunca bloquear essas pessoas. Seus contatos não são copiados, guardados nem " +
                    "enviados para lugar nenhum — tudo acontece só neste aparelho.\n\n" +
                    "Notificações: usadas só se você ativar os avisos de bloqueio nos ajustes.",
            )
        },
        confirmButton = { TextButton(onClick = onAccept) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não") } },
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(modifier.padding(vertical = 14.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 40.sp),
            color = if (highlight) Extrato.colors.blocked else MaterialTheme.colorScheme.onSurface,
        )
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}
