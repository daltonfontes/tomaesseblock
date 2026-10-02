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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Chamado pelo sistema para cada chamada recebida, antes de o telefone tocar.
 * Requer que o app seja definido como "app de identificação de chamadas e spam"
 * (RoleManager.ROLE_CALL_SCREENING).
 */
class CallBlockerService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
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

            val app = application as TomaEsseBlockApp
            val settings = app.container.settings.current()
            app.container.repository.record(normalized, decision)
            when (decision) {
                is CallDecision.Block ->
                    if (settings.notifyBlocked) Notifications.showBlocked(this@CallBlockerService, normalized, decision)
                is CallDecision.Allow ->
                    if (settings.showCallerId && decision.isSuspicious && decision.identification != null) {
                        Notifications.showCallerId(this@CallBlockerService, normalized, decision.identification)
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
        )
    }

    private fun CallDecision.toResponse(): CallResponse = when (this) {
        is CallDecision.Block -> CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        is CallDecision.Allow -> CallResponse.Builder().build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "CallBlocker"
        const val RESPONSE_TIMEOUT_MS = 3_000L
    }
}
