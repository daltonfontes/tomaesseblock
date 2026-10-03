package com.tomaesseblock.data

import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.CallDecisionEngine
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.domain.SpamCategory
import com.tomaesseblock.domain.SpamSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CallRepository(private val db: AppDatabase) {

    val rules: Flow<List<BlockRule>> = db.blockRuleDao().observeAll().map { list -> list.map { it.toDomain() } }
    val history: Flow<List<CallEventEntity>> = db.callEventDao().observeRecent()
    val blockedCount: Flow<Int> = db.callEventDao().observeBlockedCount()
    val reportedNumbers: Flow<Int> = db.spamReportDao().observeReportedNumbers()

    fun blockedSince(since: Long): Flow<Int> = db.callEventDao().observeBlockedSince(since)

    suspend fun allRules(): List<BlockRule> = db.blockRuleDao().getAll().map { it.toDomain() }

    /**
     * Adiciona uma regra. Para prefixo, mantém só os dígitos digitados (sem normalizar DDD).
     * Um mesmo número/prefixo só existe uma vez: bloquear quem estava permitido (ou o contrário)
     * substitui a regra anterior.
     */
    suspend fun addRule(
        input: String,
        type: RuleType,
        label: String = "",
        action: RuleAction = RuleAction.BLOCK,
    ): Boolean {
        val pattern = when (type) {
            RuleType.EXACT -> PhoneNumbers.normalize(input)
            RuleType.PREFIX -> input.filter { it.isDigit() || it == '+' }
        }
        if (pattern.isEmpty()) return false
        db.blockRuleDao().insert(
            BlockRuleEntity(pattern = pattern, type = type.name, label = label.trim(), action = action.name),
        )
        return true
    }

    suspend fun removeRule(rule: BlockRule) {
        db.blockRuleDao().delete(
            BlockRuleEntity(
                id = rule.id,
                pattern = rule.pattern,
                type = rule.type.name,
                label = rule.label,
                action = rule.action.name,
            ),
        )
    }

    suspend fun unblockNumber(number: String) =
        db.blockRuleDao().deleteExact(PhoneNumbers.normalize(number), RuleAction.BLOCK.name)

    suspend fun removeAllowed(number: String) =
        db.blockRuleDao().deleteExact(PhoneNumbers.normalize(number), RuleAction.ALLOW.name)

    /** Bloqueios deste número na janela da exceção "ligou de novo". */
    suspend fun recentBlockedAttempts(normalized: String, now: Long = System.currentTimeMillis()): Int {
        if (normalized.isEmpty()) return 0
        return db.callEventDao().countBlockedSince(normalized, now - CallDecisionEngine.REPEATED_CALL_WINDOW_MS)
    }

    suspend fun exportBackup(): BackupData = BackupData(
        rules = allRules(),
        reports = db.spamReportDao().getAll(),
    )

    /** Junta o backup ao que já existe; denúncias idênticas não são duplicadas. */
    suspend fun importBackup(data: BackupData): ImportResult {
        db.blockRuleDao().insertAll(
            data.rules.map {
                BlockRuleEntity(pattern = it.pattern, type = it.type.name, label = it.label, action = it.action.name)
            },
        )
        val dao = db.spamReportDao()
        var reports = 0
        for (r in data.reports) {
            if (dao.countSame(r.number, r.category, r.createdAt) == 0) {
                dao.insert(r.copy(id = 0))
                reports++
            }
        }
        return ImportResult(rules = data.rules.size, reports = reports)
    }

    suspend fun report(number: String, category: SpamCategory, note: String = "") {
        val normalized = PhoneNumbers.normalize(number)
        if (normalized.isEmpty()) return
        db.spamReportDao().insert(SpamReportEntity(number = normalized, category = category.name, note = note))
    }

    suspend fun clearReports(number: String) = db.spamReportDao().clear(PhoneNumbers.normalize(number))

    suspend fun spamSummary(normalized: String): SpamSummary? {
        if (normalized.isEmpty()) return null
        val counts = db.spamReportDao().countsFor(normalized)
        if (counts.isEmpty()) return null
        val top = counts.first()
        val category = runCatching { SpamCategory.valueOf(top.category) }.getOrDefault(SpamCategory.OTHER)
        return SpamSummary(reports = counts.sumOf { it.total }, topCategory = category)
    }

    suspend fun record(normalized: String, decision: CallDecision) {
        val event = when (decision) {
            is CallDecision.Block -> CallEventEntity(
                number = normalized,
                blocked = true,
                reason = decision.reason.name,
                label = decision.label,
            )
            is CallDecision.Allow -> CallEventEntity(
                number = normalized,
                blocked = false,
                reason = null,
                label = decision.identification,
            )
        }
        db.callEventDao().insert(event)
    }

    suspend fun clearHistory() = db.callEventDao().clear()
}
