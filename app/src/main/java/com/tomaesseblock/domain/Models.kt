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
    // Por padrão o bloqueio é silencioso (como no Whoscall): nada aparece na tela, a chamada
    // só fica registrada como "bloqueada" no histórico do telefone e no histórico do app.
    val showCallerId: Boolean = false,
    val notifyBlocked: Boolean = false,
)

enum class BlockReason(val label: String) {
    BLOCK_LIST("Na sua lista de bloqueio"),
    PREFIX("Prefixo bloqueado"),
    HIDDEN("Número oculto"),
    NOT_IN_CONTACTS("Fora dos contatos"),
    TELEMARKETING_0303("Telemarketing (0303)"),
    SPAM_REPORTED("Denunciado como spam"),
}

sealed interface CallDecision {
    /** Deixa tocar; [identification] é exibida como identificador de chamadas (estilo Whoscall). */
    data class Allow(val identification: String? = null, val isSuspicious: Boolean = false) : CallDecision

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
