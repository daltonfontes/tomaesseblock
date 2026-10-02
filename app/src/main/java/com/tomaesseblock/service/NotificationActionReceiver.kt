package com.tomaesseblock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomaesseblock.TomaEsseBlockApp
import com.tomaesseblock.domain.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Trata os botões "Bloquear" / "Desbloquear" das notificações. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(EXTRA_NUMBER) ?: return
        val repository = (context.applicationContext as TomaEsseBlockApp).container.repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_BLOCK -> repository.addRule(number, RuleType.EXACT, "Bloqueado pela notificação")
                    ACTION_UNBLOCK -> {
                        repository.unblockNumber(number)
                        repository.clearReports(number)
                    }
                }
                Notifications.cancel(context, number)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_BLOCK = "com.tomaesseblock.action.BLOCK"
        const val ACTION_UNBLOCK = "com.tomaesseblock.action.UNBLOCK"
        const val EXTRA_NUMBER = "number"
    }
}
