package com.tomaesseblock.data

import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupData(
        rules = listOf(
            BlockRule(pattern = "0303", type = RuleType.PREFIX, label = "Telemarketing"),
            BlockRule(pattern = "11987654321", type = RuleType.EXACT, label = "Médico", action = RuleAction.ALLOW),
            BlockRule(pattern = "+12125551234", type = RuleType.EXACT),
        ),
        reports = listOf(
            SpamReportEntity(number = "11912345678", category = "SCAM", note = "falso banco", createdAt = 1_000L),
        ),
    )

    @Test
    fun `ida e volta preserva regras e denuncias`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample, exportedAt = 42L))
        assertEquals(sample.rules, decoded.rules)
        assertEquals(sample.reports, decoded.reports)
    }

    @Test
    fun `rejeita arquivo que nao e backup do app`() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"foo": 1}""") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("isso não é json") }
    }

    @Test
    fun `rejeita versao mais nova`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format":"tomaesseblock-backup","version":99}""")
        }
    }

    @Test
    fun `ignora itens invalidos sem perder os validos`() {
        val json = """
            {"format":"tomaesseblock-backup","version":1,
             "rules":[
               {"pattern":"0303","type":"PREFIX","action":"BLOCK","label":""},
               {"pattern":"abc","type":"EXACT"},
               {"pattern":"1199","type":"REGEX"},
               {"pattern":"11999998888","type":"EXACT"}
             ],
             "reports":[
               {"number":"11999998888","category":"SCAM","createdAt":5},
               {"number":"11999998888","category":"NAO_EXISTE","createdAt":6}
             ]}
        """.trimIndent()
        val decoded = BackupCodec.decode(json)
        assertEquals(listOf("0303", "11999998888"), decoded.rules.map { it.pattern })
        // Sem "action" no arquivo: trata como bloqueio (compatível com backups antigos).
        assertTrue(decoded.rules.all { it.action == RuleAction.BLOCK })
        assertEquals(1, decoded.reports.size)
    }
}
