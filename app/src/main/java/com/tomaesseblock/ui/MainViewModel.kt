package com.tomaesseblock.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tomaesseblock.AppContainer
import com.tomaesseblock.TomaEsseBlockApp
import com.tomaesseblock.data.BackupCodec
import com.tomaesseblock.data.CallEventEntity
import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.BlockSettings
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.CallDecisionEngine
import com.tomaesseblock.domain.IncomingCall
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.domain.SpamCategory
import com.tomaesseblock.domain.SpamSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Resultado da tela "Buscar número". */
data class LookupResult(
    val number: String,
    val contactName: String?,
    val identification: String?,
    val decision: CallDecision,
    val spam: SpamSummary?,
    val isBlockedExact: Boolean,
    val isAllowedExact: Boolean = false,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    val settings: StateFlow<BlockSettings> =
        container.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockSettings())

    val rules: StateFlow<List<BlockRule>> =
        repo.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<CallEventEntity>> =
        repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blockedTotal: StateFlow<Int> =
        repo.blockedCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockedToday: StateFlow<Int> =
        repo.blockedSince(startOfToday()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val reportedNumbers: StateFlow<Int> =
        repo.reportedNumbers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _lookup = MutableStateFlow<LookupResult?>(null)
    val lookup: StateFlow<LookupResult?> = _lookup.asStateFlow()

    /** Mensagens curtas para o usuário (resultado de exportar/importar etc.). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun updateSettings(transform: (BlockSettings) -> BlockSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    fun addRule(input: String, type: RuleType, label: String, action: RuleAction = RuleAction.BLOCK) {
        viewModelScope.launch {
            repo.addRule(input, type, label, action)
            refreshLookup()
        }
    }

    // Ações sobre um número qualquer (usadas pelo Histórico).

    fun blockNumber(number: String) = addRule(number, RuleType.EXACT, "")

    fun allowNumber(number: String) = addRule(number, RuleType.EXACT, "", RuleAction.ALLOW)

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            repo.unblockNumber(number)
            refreshLookup()
        }
    }

    fun removeAllowed(number: String) {
        viewModelScope.launch {
            repo.removeAllowed(number)
            refreshLookup()
        }
    }

    fun reportNumber(number: String, category: SpamCategory) {
        viewModelScope.launch {
            repo.report(number, category)
            refreshLookup()
            _messages.tryEmit("Denúncia registrada")
        }
    }

    fun exportBackup(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val message = runCatching {
                val data = repo.exportBackup()
                withContext(Dispatchers.IO) {
                    val stream = resolver.openOutputStream(uri, "wt") ?: error("Não foi possível abrir o arquivo")
                    stream.bufferedWriter().use { it.write(BackupCodec.encode(data)) }
                }
                "Exportado: ${data.rules.size} regra(s) e ${data.reports.size} denúncia(s)"
            }.getOrElse { "Falha ao exportar: ${it.message}" }
            _messages.tryEmit(message)
        }
    }

    fun importBackup(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val message = runCatching {
                val json = withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: error("Não foi possível abrir o arquivo")
                    stream.bufferedReader().use { it.readText() }
                }
                val result = repo.importBackup(BackupCodec.decode(json))
                refreshLookup()
                "Importado: ${result.rules} regra(s) e ${result.reports} denúncia(s)"
            }.getOrElse { "Falha ao importar: ${it.message}" }
            _messages.tryEmit(message)
        }
    }

    fun removeRule(rule: BlockRule) {
        viewModelScope.launch { repo.removeRule(rule) }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearHistory() }
    }

    fun lookup(rawNumber: String) {
        viewModelScope.launch { _lookup.value = computeLookup(rawNumber) }
    }

    fun blockFromLookup() {
        val current = _lookup.value ?: return
        addRule(current.number, RuleType.EXACT, current.spam?.topCategory?.label.orEmpty())
    }

    fun unblockFromLookup() {
        val current = _lookup.value ?: return
        unblockNumber(current.number)
    }

    fun allowFromLookup() {
        val current = _lookup.value ?: return
        allowNumber(current.number)
    }

    fun removeAllowedFromLookup() {
        val current = _lookup.value ?: return
        removeAllowed(current.number)
    }

    fun report(category: SpamCategory) {
        val current = _lookup.value ?: return
        viewModelScope.launch {
            repo.report(current.number, category)
            refreshLookup()
        }
    }

    fun clearReports() {
        val current = _lookup.value ?: return
        viewModelScope.launch {
            repo.clearReports(current.number)
            refreshLookup()
        }
    }

    private suspend fun refreshLookup() {
        _lookup.value?.let { _lookup.value = computeLookup(it.number) }
    }

    private suspend fun computeLookup(rawNumber: String): LookupResult? {
        val normalized = PhoneNumbers.normalize(rawNumber)
        if (normalized.isEmpty()) return null
        val contact = withContext(Dispatchers.IO) { container.contacts.findName(rawNumber) }
        val spam = repo.spamSummary(normalized)
        val rules = repo.allRules()
        val call = IncomingCall(number = normalized, isHidden = false, contactName = contact)
        return LookupResult(
            number = normalized,
            contactName = contact,
            identification = CallDecisionEngine.identify(call, spam),
            decision = CallDecisionEngine.decide(call, container.settings.current(), rules, spam),
            spam = spam,
            isBlockedExact = rules.any {
                it.type == RuleType.EXACT && it.pattern == normalized && it.action == RuleAction.BLOCK
            },
            isAllowedExact = rules.any {
                it.type == RuleType.EXACT && it.pattern == normalized && it.action == RuleAction.ALLOW
            },
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TomaEsseBlockApp
                MainViewModel(app.container)
            }
        }
    }
}
