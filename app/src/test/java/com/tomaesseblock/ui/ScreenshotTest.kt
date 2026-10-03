package com.tomaesseblock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tomaesseblock.data.CallEventEntity
import com.tomaesseblock.domain.BlockReason
import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.BlockSettings
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.domain.SpamCategory
import com.tomaesseblock.domain.SpamSummary
import com.tomaesseblock.ui.screens.BlockListContent
import com.tomaesseblock.ui.screens.HistoryContent
import com.tomaesseblock.ui.screens.HomeContent
import com.tomaesseblock.ui.screens.LookupContent
import com.tomaesseblock.ui.screens.SettingsContent
import com.tomaesseblock.ui.theme.TomaEsseBlockTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/**
 * Gera capturas de tela de cada aba, nos temas claro e escuro, com dados de exemplo.
 * `./gradlew recordPaparazziDebug` salva as imagens em app/src/test/snapshots/images/.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Before
    fun fixedLocale() {
        // Datas do histórico sempre no formato brasileiro, independentemente da máquina do CI.
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    @Test
    fun inicio() = shotBothThemes(Tab.HOME) {
        HomeContent(
            blockingEnabled = true,
            today = 3,
            total = 47,
            reported = 12,
            hasRole = true,
            hasContacts = true,
            hasNotifications = true,
            onActivateRole = {},
            onRequestPermissions = {},
            onOpenLookup = {},
        )
    }

    @Test
    fun bloqueios() = shotBothThemes(Tab.BLOCKLIST) {
        BlockListContent(rules = SAMPLE_RULES, onRemove = {}, onAdd = { _, _, _ -> })
    }

    @Test
    fun historico() = shotBothThemes(Tab.HISTORY) {
        HistoryContent(history = SAMPLE_HISTORY, onClear = {}, onOpenNumber = {})
    }

    @Test
    fun buscar() = shotBothThemes(Tab.LOOKUP) {
        LookupContent(
            result = SAMPLE_LOOKUP,
            onLookup = {},
            onBlock = {},
            onUnblock = {},
            onReport = {},
            onClearReports = {},
        )
    }

    @Test
    fun ajustes() = shotBothThemes(Tab.SETTINGS) {
        SettingsContent(s = BlockSettings(), onUpdate = {}, onOpenPrivacyPolicy = {})
    }

    private fun shotBothThemes(tab: Tab, content: @Composable () -> Unit) {
        for (dark in listOf(false, true)) {
            paparazzi.snapshot(if (dark) "escuro" else "claro") {
                TomaEsseBlockTheme(darkTheme = dark) {
                    AppScaffold(currentRoute = tab.route, onTabSelected = {}) { padding ->
                        Box(Modifier.padding(padding)) { content() }
                    }
                }
            }
        }
    }

    private companion object {
        // 02/10/2026 14:30 em Brasília.
        const val BASE_TIME = 1_790_962_200_000L
        const val HOUR = 3_600_000L

        val SAMPLE_RULES = listOf(
            BlockRule(id = 1, pattern = "0303", type = RuleType.PREFIX, label = "Telemarketing"),
            BlockRule(id = 2, pattern = "11987654321", type = RuleType.EXACT, label = "Golpe do falso banco"),
            BlockRule(id = 3, pattern = "1140", type = RuleType.PREFIX, label = "Call center"),
            BlockRule(id = 4, pattern = "21998887777", type = RuleType.EXACT, label = ""),
        )

        val SAMPLE_HISTORY = listOf(
            CallEventEntity(
                id = 1, number = "03031234567", timestamp = BASE_TIME, blocked = true,
                reason = BlockReason.TELEMARKETING_0303.name, label = "Telemarketing (0303)",
            ),
            CallEventEntity(
                id = 2, number = "11987654321", timestamp = BASE_TIME - HOUR, blocked = true,
                reason = BlockReason.BLOCK_LIST.name, label = "Golpe do falso banco",
            ),
            CallEventEntity(
                id = 3, number = "08001234567", timestamp = BASE_TIME - 3 * HOUR, blocked = false,
                reason = null, label = "Ligação gratuita (0800)",
            ),
            CallEventEntity(
                id = 4, number = "1140028922", timestamp = BASE_TIME - 5 * HOUR, blocked = true,
                reason = BlockReason.PREFIX.name, label = "Call center",
            ),
            CallEventEntity(
                id = 5, number = "21976543210", timestamp = BASE_TIME - 26 * HOUR, blocked = false,
                reason = null, label = "Possível spam: Cobrança (1 denúncia)",
            ),
            CallEventEntity(
                id = 6, number = "", timestamp = BASE_TIME - 30 * HOUR, blocked = true,
                reason = BlockReason.HIDDEN.name, label = "Número oculto",
            ),
        )

        val SAMPLE_LOOKUP = LookupResult(
            number = "11912345678",
            contactName = null,
            identification = "Possível spam: Golpe / Fraude (3 denúncias)",
            decision = CallDecision.Block(BlockReason.SPAM_REPORTED, "Golpe / Fraude (3 denúncias)"),
            spam = SpamSummary(reports = 3, topCategory = SpamCategory.SCAM),
            isBlockedExact = false,
        )
    }
}
