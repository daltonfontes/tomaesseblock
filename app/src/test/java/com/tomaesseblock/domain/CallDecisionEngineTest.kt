package com.tomaesseblock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallDecisionEngineTest {

    private val defaults = BlockSettings()
    private fun call(number: String, contact: String? = null) =
        IncomingCall(number = number, isHidden = number.isEmpty(), contactName = contact)

    @Test
    fun `numero na lista de bloqueio e bloqueado`() {
        val rules = listOf(BlockRule(pattern = "11999998888", type = RuleType.EXACT, label = "Chato"))
        val d = CallDecisionEngine.decide(call("11999998888"), defaults, rules, null)
        assertEquals(CallDecision.Block(BlockReason.BLOCK_LIST, "Chato"), d)
    }

    @Test
    fun `lista de bloqueio vale mesmo para contato`() {
        val rules = listOf(BlockRule(pattern = "11999998888", type = RuleType.EXACT))
        val d = CallDecisionEngine.decide(call("11999998888", contact = "Ex"), defaults, rules, null)
        assertTrue(d is CallDecision.Block)
    }

    @Test
    fun `prefixo mais longo vence`() {
        val rules = listOf(
            BlockRule(pattern = "11", type = RuleType.PREFIX, label = "DDD 11"),
            BlockRule(pattern = "1140", type = RuleType.PREFIX, label = "Call center"),
        )
        val d = CallDecisionEngine.decide(call("1140001234"), defaults, rules, null)
        assertEquals(CallDecision.Block(BlockReason.PREFIX, "Call center"), d)
    }

    @Test
    fun `telemarketing 0303 bloqueado por padrao`() {
        val d = CallDecisionEngine.decide(call("03031234567"), defaults, emptyList(), null)
        assertEquals(BlockReason.TELEMARKETING_0303, (d as CallDecision.Block).reason)
    }

    @Test
    fun `0303 apenas identificado quando bloqueio desligado`() {
        val s = defaults.copy(blockTelemarketing0303 = false)
        val d = CallDecisionEngine.decide(call("03031234567"), s, emptyList(), null)
        assertEquals(CallDecision.Allow("Telemarketing (0303)", isSuspicious = true), d)
    }

    @Test
    fun `contato e sempre permitido`() {
        val s = defaults.copy(blockNotInContacts = true)
        val spam = SpamSummary(10, SpamCategory.SCAM)
        val d = CallDecisionEngine.decide(call("11999998888", contact = "Mãe"), s, emptyList(), spam)
        assertEquals(CallDecision.Allow("Mãe"), d)
    }

    @Test
    fun `spam acima do limite e bloqueado`() {
        val s = defaults.copy(spamThreshold = 3)
        val d = CallDecisionEngine.decide(call("11999998888"), s, emptyList(), SpamSummary(3, SpamCategory.SCAM))
        assertEquals(BlockReason.SPAM_REPORTED, (d as CallDecision.Block).reason)
        assertEquals("Golpe / Fraude (3 denúncias)", d.label)
    }

    @Test
    fun `spam abaixo do limite e apenas identificado`() {
        val s = defaults.copy(spamThreshold = 3)
        val d = CallDecisionEngine.decide(call("11999998888"), s, emptyList(), SpamSummary(1, SpamCategory.TELEMARKETING))
        assertEquals(CallDecision.Allow("Possível spam: Telemarketing (1 denúncia)", isSuspicious = true), d)
    }

    @Test
    fun `numero oculto`() {
        assertTrue(CallDecisionEngine.decide(call(""), defaults, emptyList(), null) is CallDecision.Allow)
        val d = CallDecisionEngine.decide(call(""), defaults.copy(blockHidden = true), emptyList(), null)
        assertEquals(BlockReason.HIDDEN, (d as CallDecision.Block).reason)
    }

    @Test
    fun `modo rigoroso bloqueia desconhecidos`() {
        val d = CallDecisionEngine.decide(call("11999998888"), defaults.copy(blockNotInContacts = true), emptyList(), null)
        assertEquals(BlockReason.NOT_IN_CONTACTS, (d as CallDecision.Block).reason)
    }

    @Test
    fun `bloqueio desligado permite tudo`() {
        val rules = listOf(BlockRule(pattern = "11999998888", type = RuleType.EXACT))
        val d = CallDecisionEngine.decide(call("11999998888"), defaults.copy(blockingEnabled = false), rules, null)
        assertTrue(d is CallDecision.Allow)
    }

    @Test
    fun `numero comum sem denuncias e permitido sem identificacao`() {
        val d = CallDecisionEngine.decide(call("11999998888"), defaults, emptyList(), null)
        assertEquals(CallDecision.Allow(null, isSuspicious = false), d)
    }

    // --- Lista de permitidos ---

    @Test
    fun `permitido vence bloqueio exato e modo rigoroso`() {
        val rules = listOf(
            BlockRule(pattern = "11999998888", type = RuleType.EXACT, action = RuleAction.BLOCK),
            BlockRule(pattern = "11999998888", type = RuleType.EXACT, label = "Médico", action = RuleAction.ALLOW),
        )
        val s = defaults.copy(blockNotInContacts = true)
        assertEquals(CallDecision.Allow("Médico"), CallDecisionEngine.decide(call("11999998888"), s, rules, null))
    }

    @Test
    fun `prefixo permitido vence prefixo bloqueado e spam`() {
        val rules = listOf(
            BlockRule(pattern = "11", type = RuleType.PREFIX, action = RuleAction.BLOCK),
            BlockRule(pattern = "1133", type = RuleType.PREFIX, action = RuleAction.ALLOW),
        )
        val d = CallDecisionEngine.decide(call("1133334444"), defaults, rules, SpamSummary(5, SpamCategory.SCAM))
        assertEquals(CallDecision.Allow(CallDecisionEngine.ALLOW_LIST_LABEL), d)
    }

    @Test
    fun `regra de permitido nao bloqueia outros numeros`() {
        val rules = listOf(BlockRule(pattern = "1133", type = RuleType.PREFIX, action = RuleAction.ALLOW))
        val d = CallDecisionEngine.decide(call("03031234567"), defaults, rules, null)
        assertEquals(BlockReason.TELEMARKETING_0303, (d as CallDecision.Block).reason)
    }

    @Test
    fun `permitido nao libera numero oculto`() {
        val rules = listOf(BlockRule(pattern = "11", type = RuleType.PREFIX, action = RuleAction.ALLOW))
        val d = CallDecisionEngine.decide(call(""), defaults.copy(blockHidden = true), rules, null)
        assertEquals(BlockReason.HIDDEN, (d as CallDecision.Block).reason)
    }

    // --- Ligou de novo ---

    @Test
    fun `segunda ligacao de desconhecido toca no modo rigoroso`() {
        val s = defaults.copy(blockNotInContacts = true)
        assertTrue(CallDecisionEngine.decide(call("11999998888"), s, emptyList(), null) is CallDecision.Block)
        val again = CallDecisionEngine.decide(call("11999998888"), s, emptyList(), null, recentBlockedAttempts = 1)
        assertEquals(CallDecision.Allow(CallDecisionEngine.REPEATED_CALL_LABEL), again)
    }

    @Test
    fun `ligou de novo vale para internacional e prefixo`() {
        val intl = CallDecisionEngine.decide(
            call("+12125551234"), defaults.copy(blockInternational = true), emptyList(), null, recentBlockedAttempts = 2,
        )
        assertTrue(intl is CallDecision.Allow)
        val rules = listOf(BlockRule(pattern = "11", type = RuleType.PREFIX))
        val prefix = CallDecisionEngine.decide(call("11999998888"), defaults, rules, null, recentBlockedAttempts = 1)
        assertTrue(prefix is CallDecision.Allow)
    }

    @Test
    fun `ligou de novo nao libera bloqueio escolhido, spam nem 0303`() {
        val exact = listOf(BlockRule(pattern = "11999998888", type = RuleType.EXACT))
        assertTrue(CallDecisionEngine.decide(call("11999998888"), defaults, exact, null, 3) is CallDecision.Block)
        val spam = SpamSummary(2, SpamCategory.SCAM)
        assertTrue(CallDecisionEngine.decide(call("11988887777"), defaults, emptyList(), spam, 3) is CallDecision.Block)
        assertTrue(CallDecisionEngine.decide(call("03031234567"), defaults, emptyList(), null, 3) is CallDecision.Block)
    }

    @Test
    fun `ligou de novo desligado mantem bloqueio`() {
        val s = defaults.copy(blockNotInContacts = true, allowRepeatedCalls = false)
        val d = CallDecisionEngine.decide(call("11999998888"), s, emptyList(), null, recentBlockedAttempts = 5)
        assertEquals(BlockReason.NOT_IN_CONTACTS, (d as CallDecision.Block).reason)
    }

    // --- Categorias de spam ---

    @Test
    fun `categoria desmarcada so identifica`() {
        val s = defaults.copy(blockedSpamCategories = setOf(SpamCategory.SCAM))
        val survey = SpamSummary(4, SpamCategory.SURVEY)
        val d = CallDecisionEngine.decide(call("11999998888"), s, emptyList(), survey)
        assertEquals(CallDecision.Allow("Possível spam: Pesquisa (4 denúncias)", isSuspicious = true), d)
        val scam = SpamSummary(1, SpamCategory.SCAM)
        assertTrue(CallDecisionEngine.decide(call("11999998888"), s, emptyList(), scam) is CallDecision.Block)
    }

    // --- Internacionais ---

    @Test
    fun `internacional bloqueado so quando ligado`() {
        val n = call("+447911123456")
        assertTrue(CallDecisionEngine.decide(n, defaults, emptyList(), null) is CallDecision.Allow)
        val d = CallDecisionEngine.decide(n, defaults.copy(blockInternational = true), emptyList(), null)
        assertEquals(BlockReason.INTERNATIONAL, (d as CallDecision.Block).reason)
    }

    @Test
    fun `numero brasileiro com +55 nao e internacional`() {
        val normalized = PhoneNumbers.normalize("+55 11 99999-8888")
        val d = CallDecisionEngine.decide(call(normalized), defaults.copy(blockInternational = true), emptyList(), null)
        assertTrue(d is CallDecision.Allow)
    }

    @Test
    fun `contato internacional continua tocando`() {
        val d = CallDecisionEngine.decide(
            call("+12125551234", contact = "Tia nos EUA"), defaults.copy(blockInternational = true), emptyList(), null,
        )
        assertEquals(CallDecision.Allow("Tia nos EUA"), d)
    }
}
