package com.tomaesseblock.data

import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.RuleAction
import com.tomaesseblock.domain.RuleType
import com.tomaesseblock.domain.SpamCategory
import org.json.JSONArray
import org.json.JSONObject

/** Conteúdo de um backup manual: regras (bloqueio e permitidos) e denúncias. */
data class BackupData(
    val rules: List<BlockRule>,
    val reports: List<SpamReportEntity>,
)

data class ImportResult(val rules: Int, val reports: Int)

/**
 * Lê e escreve o arquivo de backup (JSON). O backup do Android fica desligado para o app, então
 * este arquivo, salvo onde o usuário quiser, é o jeito de levar a lista para outro aparelho.
 */
object BackupCodec {
    const val FORMAT = "tomaesseblock-backup"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"

    private val VALID_PATTERN = Regex("""^\+?\d{1,20}$""")

    fun encode(data: BackupData, exportedAt: Long = System.currentTimeMillis()): String {
        val rules = JSONArray()
        data.rules.forEach {
            rules.put(
                JSONObject()
                    .put("pattern", it.pattern)
                    .put("type", it.type.name)
                    .put("action", it.action.name)
                    .put("label", it.label),
            )
        }
        val reports = JSONArray()
        data.reports.forEach {
            reports.put(
                JSONObject()
                    .put("number", it.number)
                    .put("category", it.category)
                    .put("note", it.note)
                    .put("createdAt", it.createdAt),
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("rules", rules)
            .put("reports", reports)
            .toString(2)
    }

    /**
     * @throws IllegalArgumentException se o arquivo não for um backup do app.
     * Itens inválidos (número estranho, tipo desconhecido) são ignorados, não derrubam a importação.
     */
    fun decode(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Arquivo não é um backup válido", e)
        }
        require(root.optString("format") == FORMAT) { "Arquivo não é um backup do Calloff" }
        require(root.optInt("version", 0) in 1..VERSION) { "Backup de uma versão mais nova do app" }

        val rules = mutableListOf<BlockRule>()
        root.optJSONArray("rules")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val pattern = o.optString("pattern")
                val type = enumOrNull<RuleType>(o.optString("type")) ?: continue
                val action = enumOrNull<RuleAction>(o.optString("action")) ?: RuleAction.BLOCK
                if (!VALID_PATTERN.matches(pattern)) continue
                rules += BlockRule(pattern = pattern, type = type, label = o.optString("label").take(100), action = action)
            }
        }

        val reports = mutableListOf<SpamReportEntity>()
        root.optJSONArray("reports")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val number = o.optString("number")
                val category = enumOrNull<SpamCategory>(o.optString("category")) ?: continue
                if (!VALID_PATTERN.matches(number)) continue
                reports += SpamReportEntity(
                    number = number,
                    category = category.name,
                    note = o.optString("note").take(500),
                    createdAt = o.optLong("createdAt", 0L),
                )
            }
        }
        return BackupData(rules, reports)
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
