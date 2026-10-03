package com.tomaesseblock.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telecom.TelecomManager
import android.util.Log
import com.tomaesseblock.TomaEsseBlockApp
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.CallDecisionEngine
import com.tomaesseblock.domain.IncomingCall
import com.tomaesseblock.domain.PhoneNumbers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Chamado pelo sistema para cada chamada recebida, antes de o telefone tocar.
 * Requer que o app seja definido como "app de identificação de chamadas e spam"
 * (RoleManager.ROLE_CALL_SCREENING).
 */
class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val container = (application as TomaEsseBlockApp).container
        val appContext = applicationContext
        // Escopo da aplicação, não do serviço: o sistema desliga o serviço logo após a resposta,
        // e isso não pode cancelar a gravação no histórico.
        container.appScope.launch {
            val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
            val normalized = PhoneNumbers.normalize(rawNumber)
            // O sistema espera a resposta em poucos segundos; em caso de demora, deixa tocar.
            val decision = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                runCatching { evaluate(callDetails, rawNumber, normalized) }
                    .onFailure { Log.e(TAG, "Falha ao avaliar chamada", it) }
                    .getOrNull()
            } ?: CallDecision.Allow()

            respondToCall(callDetails, decision.toResponse())
            Log.i(TAG, "Chamada de ${normalized.ifEmpty { "oculto" }}: $decision")

            runCatching { container.repository.record(normalized, decision) }
                .onFailure { Log.e(TAG, "Falha ao gravar no histórico", it) }

            val settings = container.settings.current()
            when (decision) {
                is CallDecision.Block ->
                    if (settings.notifyBlocked) Notifications.showBlocked(appContext, normalized, decision)
                is CallDecision.Allow ->
                    if (settings.showCallerId && decision.isSuspicious && decision.identification != null) {
                        Notifications.showCallerId(appContext, normalized, decision.identification)
                    }
            }
        }
    }

    private suspend fun evaluate(details: Call.Details, rawNumber: String, normalized: String): CallDecision {
        val container = (application as TomaEsseBlockApp).container
        val hidden = details.handle == null ||
            details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED ||
            normalized.isEmpty()
        val call = IncomingCall(
            number = normalized,
            isHidden = hidden,
            contactName = if (hidden) null else container.contacts.findName(rawNumber),
        )
        return CallDecisionEngine.decide(
            call = call,
            settings = container.settings.current(),
            rules = container.repository.allRules(),
            spam = container.repository.spamSummary(normalized),
            recentBlockedAttempts = if (hidden) 0 else container.repository.recentBlockedAttempts(normalized),
        )
    }

    private fun CallDecision.toResponse(): CallResponse = when (this) {
        // Bloqueio silencioso: o telefone não toca, a chamada é recusada e o próprio Android
        // registra no histórico de chamadas como "bloqueada", sem notificação do sistema.
        is CallDecision.Block -> CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        is CallDecision.Allow -> CallResponse.Builder().build()
    }

    private companion object {
        const val TAG = "CallBlocker"
        const val RESPONSE_TIMEOUT_MS = 3_000L
    }
}
