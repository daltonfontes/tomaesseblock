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
        assertEquals(CallDecision.Allow("Telemarketing (0303)", AlertLevel.WARNING), d)
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
        assertEquals(CallDecision.Allow("Possível spam: Telemarketing (1 denúncia)", AlertLevel.DANGER), d)
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
        assertEquals(CallDecision.Allow(null, AlertLevel.NONE), d)
    }

    @Test
    fun `nivel de alerta`() {
        assertEquals(AlertLevel.NONE, CallDecisionEngine.alertLevel(call("11999998888", contact = "Mãe"), null))
        assertEquals(AlertLevel.NONE, CallDecisionEngine.alertLevel(call("11999998888"), null))
        assertEquals(AlertLevel.INFO, CallDecisionEngine.alertLevel(call("08001234567"), null))
        assertEquals(AlertLevel.INFO, CallDecisionEngine.alertLevel(call(""), null))
        assertEquals(AlertLevel.WARNING, CallDecisionEngine.alertLevel(call("03031234567"), null))
        assertEquals(
            AlertLevel.DANGER,
            CallDecisionEngine.alertLevel(call("11999998888"), SpamSummary(1, SpamCategory.SCAM)),
        )
    }

    @Test
    fun `numero 0800 e identificado como informativo`() {
        val d = CallDecisionEngine.decide(call("08001234567"), defaults, emptyList(), null)
        assertEquals(CallDecision.Allow("Ligação gratuita (0800)", AlertLevel.INFO), d)
    }
}
