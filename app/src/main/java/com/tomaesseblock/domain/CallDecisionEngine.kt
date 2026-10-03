package com.tomaesseblock.domain

/**
 * Decide se uma chamada deve ser bloqueada ou apenas identificada.
 * É uma função pura para ser fácil de testar; quem coleta os dados é o serviço.
 *
 * Ordem de prioridade:
 *  1. Número oculto
 *  2. Lista de permitidos (exato, depois prefixo) — vence qualquer bloqueio
 *  3. Lista de bloqueio (exato, depois prefixo) — vale até para contatos
 *  4. Contatos são sempre permitidos
 *  5. Telemarketing 0303 (padrão da Anatel)
 *  6. Denúncias de spam (só das categorias escolhidas)
 *  7. Ligações internacionais
 *  8. Bloquear quem não está nos contatos
 *
 * Bloqueios amplos ([BlockReason.isBroad]) viram "deixa tocar" se o mesmo número já foi
 * bloqueado há poucos minutos e [BlockSettings.allowRepeatedCalls] estiver ligado.
 */
object CallDecisionEngine {

    /** Janela da exceção "ligou de novo". */
    const val REPEATED_CALL_WINDOW_MS = 5 * 60 * 1000L

    const val REPEATED_CALL_LABEL = "Ligou de novo em poucos minutos"
    const val ALLOW_LIST_LABEL = "Na sua lista de permitidos"

    /**
     * @param recentBlockedAttempts quantas vezes este número foi bloqueado dentro de
     *   [REPEATED_CALL_WINDOW_MS] antes desta chamada.
     */
    fun decide(
        call: IncomingCall,
        settings: BlockSettings,
        rules: List<BlockRule>,
        spam: SpamSummary?,
        recentBlockedAttempts: Int = 0,
    ): CallDecision {
        if (!settings.blockingEnabled) return CallDecision.Allow(identify(call, spam))

        if (call.isHidden || call.number.isEmpty()) {
            return if (settings.blockHidden) {
                CallDecision.Block(BlockReason.HIDDEN, BlockReason.HIDDEN.label)
            } else {
                CallDecision.Allow("Número oculto", isSuspicious = false)
            }
        }

        matchRule(call.number, rules, RuleAction.ALLOW)?.let {
            return CallDecision.Allow(it.label.ifBlank { ALLOW_LIST_LABEL })
        }

        val block = blockDecision(call, settings, rules, spam)
            ?: return if (call.contactName != null) {
                CallDecision.Allow(call.contactName)
            } else {
                CallDecision.Allow(identify(call, spam), isSuspicious = isSuspicious(call, spam))
            }

        if (settings.allowRepeatedCalls && block.reason.isBroad && recentBlockedAttempts > 0) {
            return CallDecision.Allow(REPEATED_CALL_LABEL)
        }
        return block
    }

    private fun blockDecision(
        call: IncomingCall,
        settings: BlockSettings,
        rules: List<BlockRule>,
        spam: SpamSummary?,
    ): CallDecision.Block? {
        matchRule(call.number, rules, RuleAction.BLOCK)?.let {
            return if (it.type == RuleType.EXACT) {
                CallDecision.Block(BlockReason.BLOCK_LIST, it.label.ifBlank { BlockReason.BLOCK_LIST.label })
            } else {
                CallDecision.Block(BlockReason.PREFIX, it.label.ifBlank { "Prefixo ${it.pattern}" })
            }
        }

        if (call.contactName != null) return null

        if (settings.blockTelemarketing0303 && isTelemarketing0303(call.number)) {
            return CallDecision.Block(BlockReason.TELEMARKETING_0303, BlockReason.TELEMARKETING_0303.label)
        }

        if (settings.blockReportedSpam && spam != null && spam.reports >= settings.spamThreshold &&
            spam.topCategory in settings.blockedSpamCategories
        ) {
            return CallDecision.Block(BlockReason.SPAM_REPORTED, spamLabel(spam))
        }

        if (settings.blockInternational && isInternational(call.number)) {
            return CallDecision.Block(BlockReason.INTERNATIONAL, BlockReason.INTERNATIONAL.label)
        }

        if (settings.blockNotInContacts) {
            return CallDecision.Block(BlockReason.NOT_IN_CONTACTS, BlockReason.NOT_IN_CONTACTS.label)
        }
        return null
    }

    /** Regra exata tem prioridade; entre prefixos, vence o mais longo. */
    fun matchRule(number: String, rules: List<BlockRule>, action: RuleAction): BlockRule? {
        val candidates = rules.filter { it.action == action }
        return candidates.firstOrNull { it.type == RuleType.EXACT && it.pattern == number }
            ?: candidates.filter { it.type == RuleType.PREFIX && number.startsWith(it.pattern) }
                .maxByOrNull { it.pattern.length }
    }

    /** Texto do identificador de chamadas para chamadas que não foram bloqueadas. */
    fun identify(call: IncomingCall, spam: SpamSummary?): String? {
        call.contactName?.let { return it }
        val n = call.number
        return when {
            n.isEmpty() -> "Número oculto"
            spam != null && spam.reports > 0 -> "Possível spam: ${spamLabel(spam)}"
            isTelemarketing0303(n) -> "Telemarketing (0303)"
            n.startsWith("0800") -> "Ligação gratuita (0800)"
            n.startsWith("0300") || n.startsWith("0500") || n.startsWith("0900") -> "Serviço especial (${n.take(4)})"
            (n.startsWith("4004") || n.startsWith("3003")) && n.length == 8 -> "Central de atendimento"
            isInternational(n) -> "Chamada internacional"
            else -> null
        }
    }

    private fun isSuspicious(call: IncomingCall, spam: SpamSummary?): Boolean =
        (spam != null && spam.reports > 0) || isTelemarketing0303(call.number)

    fun isTelemarketing0303(number: String): Boolean = number.startsWith("0303")

    /** Números normalizados de fora do Brasil mantêm o "+" (ver [PhoneNumbers.normalize]). */
    fun isInternational(number: String): Boolean = number.startsWith("+")

    private fun spamLabel(spam: SpamSummary): String {
        val times = if (spam.reports == 1) "1 denúncia" else "${spam.reports} denúncias"
        return "${spam.topCategory.label} ($times)"
    }
}
