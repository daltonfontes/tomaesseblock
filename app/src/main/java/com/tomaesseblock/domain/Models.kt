package com.tomaesseblock.domain

enum class RuleType { EXACT, PREFIX }

/** Regra de bloqueio criada pelo usuário. [pattern] já está normalizado. */
data class BlockRule(
    val id: Long = 0,
    val pattern: String,
    val type: RuleType,
    val label: String = "",
)

enum class SpamCategory(val label: String) {
    TELEMARKETING("Telemarketing"),
    SCAM("Golpe / Fraude"),
    DEBT_COLLECTION("Cobrança"),
    ROBOCALL("Robô / Ligação muda"),
    SURVEY("Pesquisa"),
    OTHER("Outro"),
}

/** Resumo das denúncias de spam de um número. */
data class SpamSummary(
    val reports: Int,
    val topCategory: SpamCategory,
)

data class BlockSettings(
    val blockingEnabled: Boolean = true,
    val blockHidden: Boolean = false,
    val blockNotInContacts: Boolean = false,
    val blockTelemarketing0303: Boolean = true,
    val blockReportedSpam: Boolean = true,
    /** Quantidade mínima de denúncias para considerar o número spam. */
    val spamThreshold: Int = 1,
    val showCallerId: Boolean = true,
    /** Mostra o identificador por cima da tela de chamada (requer "Exibir sobre outros apps"). */
    val showOverlay: Boolean = true,
    val notifyBlocked: Boolean = true,
)

enum class BlockReason(val label: String) {
    BLOCK_LIST("Na sua lista de bloqueio"),
    PREFIX("Prefixo bloqueado"),
    HIDDEN("Número oculto"),
    NOT_IN_CONTACTS("Fora dos contatos"),
    TELEMARKETING_0303("Telemarketing (0303)"),
    SPAM_REPORTED("Denunciado como spam"),
}

/** Quão chamativo deve ser o identificador de chamadas. */
enum class AlertLevel {
    /** Nada a mostrar (contato ou número comum). */
    NONE,

    /** Informativo: 0800, central de atendimento, internacional, oculto. */
    INFO,

    /** Suspeito: telemarketing 0303. */
    WARNING,

    /** Perigo: número com denúncias de spam. */
    DANGER,
}

sealed interface CallDecision {
    /** Deixa tocar; [identification] é exibida como identificador de chamadas (estilo Whoscall). */
    data class Allow(val identification: String? = null, val level: AlertLevel = AlertLevel.NONE) : CallDecision {
        val isSuspicious: Boolean get() = level >= AlertLevel.WARNING
    }

    data class Block(val reason: BlockReason, val label: String) : CallDecision
}

/** Tudo o que o motor precisa saber sobre uma chamada recebida. */
data class IncomingCall(
    /** Número já normalizado; vazio se oculto. */
    val number: String,
    val isHidden: Boolean,
    /** Nome do contato, se o número estiver na agenda. */
    val contactName: String?,
)
