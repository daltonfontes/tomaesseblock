package com.tomaesseblock.domain

/**
 * Decide se uma chamada deve ser bloqueada ou apenas identificada.
 * É uma função pura para ser fácil de testar; quem coleta os dados é o serviço.
 *
 * Ordem de prioridade:
 *  1. Número oculto
 *  2. Lista de bloqueio do usuário (exato, depois prefixo) — vale até para contatos
 *  3. Contatos são sempre permitidos
 *  4. Telemarketing 0303 (padrão da Anatel)
 *  5. Denúncias de spam
 *  6. Bloquear quem não está nos contatos
 */
object CallDecisionEngine {

    fun decide(
        call: IncomingCall,
        settings: BlockSettings,
        rules: List<BlockRule>,
        spam: SpamSummary?,
    ): CallDecision {
        if (!settings.blockingEnabled) return CallDecision.Allow(identify(call, spam))

        if (call.isHidden || call.number.isEmpty()) {
            return if (settings.blockHidden) {
                CallDecision.Block(BlockReason.HIDDEN, BlockReason.HIDDEN.label)
            } else {
                CallDecision.Allow("Número oculto", isSuspicious = false)
            }
        }

        rules.firstOrNull { it.type == RuleType.EXACT && it.pattern == call.number }?.let {
            return CallDecision.Block(BlockReason.BLOCK_LIST, it.label.ifBlank { BlockReason.BLOCK_LIST.label })
        }
        rules.filter { it.type == RuleType.PREFIX && call.number.startsWith(it.pattern) }
            .maxByOrNull { it.pattern.length }
            ?.let {
                return CallDecision.Block(
                    BlockReason.PREFIX,
                    it.label.ifBlank { "Prefixo ${it.pattern}" },
                )
            }

        if (call.contactName != null) return CallDecision.Allow(call.contactName)

        if (settings.blockTelemarketing0303 && isTelemarketing0303(call.number)) {
            return CallDecision.Block(BlockReason.TELEMARKETING_0303, BlockReason.TELEMARKETING_0303.label)
        }

        if (settings.blockReportedSpam && spam != null && spam.reports >= settings.spamThreshold) {
            return CallDecision.Block(BlockReason.SPAM_REPORTED, spamLabel(spam))
        }

        if (settings.blockNotInContacts) {
            return CallDecision.Block(BlockReason.NOT_IN_CONTACTS, BlockReason.NOT_IN_CONTACTS.label)
        }

        return CallDecision.Allow(identify(call, spam), isSuspicious = isSuspicious(call, spam))
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
            n.startsWith("+") -> "Chamada internacional"
            else -> null
        }
    }

    private fun isSuspicious(call: IncomingCall, spam: SpamSummary?): Boolean =
        (spam != null && spam.reports > 0) || isTelemarketing0303(call.number)

    fun isTelemarketing0303(number: String): Boolean = number.startsWith("0303")

    private fun spamLabel(spam: SpamSummary): String {
        val times = if (spam.reports == 1) "1 denúncia" else "${spam.reports} denúncias"
        return "${spam.topCategory.label} ($times)"
    }
}
