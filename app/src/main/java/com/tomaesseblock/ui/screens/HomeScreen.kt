package com.tomaesseblock.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tomaesseblock.overlay.CallerIdOverlay
import com.tomaesseblock.ui.MainViewModel
import com.tomaesseblock.ui.theme.Danger
import com.tomaesseblock.ui.theme.Safe

private fun Context.holdsScreeningRole(): Boolean =
    getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

/** Abre a tela do sistema "Exibir sobre outros apps" já no nosso app. */
fun overlayPermissionIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

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
    var hasPhoneState by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_PHONE_STATE)) }
    var hasOverlay by remember { mutableStateOf(CallerIdOverlay.canDrawOverlays(context)) }
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
        hasPhoneState = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        hasOverlay = CallerIdOverlay.canDrawOverlays(context)
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
        hasPhoneState = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val protectedNow = hasRole && settings.blockingEnabled

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        ScreenTitle("Toma Esse Block")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = (if (protectedNow) Safe else Danger).copy(alpha = 0.12f),
            ),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (protectedNow) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (protectedNow) Safe else Danger,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (protectedNow) "Proteção ativa" else "Proteção desativada",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        when {
                            !hasRole -> "Defina o app como identificador de chamadas e spam."
                            !settings.blockingEnabled -> "O bloqueio está desligado nos ajustes."
                            else -> "Chamadas indesejadas serão bloqueadas automaticamente."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (!hasRole) {
            Button(
                onClick = {
                    val rm = context.getSystemService(RoleManager::class.java)
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Ativar bloqueio de chamadas") }
        }

        if (!hasContacts || !hasNotifications || !hasPhoneState) {
            OutlinedButton(
                onClick = {
                    val perms = buildList {
                        add(Manifest.permission.READ_CONTACTS)
                        add(Manifest.permission.READ_PHONE_STATE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(
                    buildString {
                        append("Permitir ")
                        append(
                            listOfNotNull(
                                "contatos".takeIf { !hasContacts },
                                "notificações".takeIf { !hasNotifications },
                                "estado das chamadas".takeIf { !hasPhoneState },
                            ).joinToString(", "),
                        )
                    },
                )
            }
        }

        if (!hasOverlay) {
            OutlinedButton(
                onClick = { context.startActivity(overlayPermissionIntent(context)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Permitir aviso por cima da chamada") }
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard("Hoje", today.toString(), Modifier.weight(1f))
            StatCard("Total bloqueadas", total.toString(), Modifier.weight(1f))
            StatCard("Denunciados", reported.toString(), Modifier.weight(1f))
        }

        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Quem está ligando?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Consulte um número para ver denúncias e bloquear ou denunciar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(8.dp))
                Button(onClick = onOpenLookup) { Text("Buscar número") }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
