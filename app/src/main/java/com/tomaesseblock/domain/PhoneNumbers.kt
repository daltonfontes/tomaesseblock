package com.tomaesseblock.domain

/**
 * Normalização de números de telefone com foco no Brasil.
 *
 * O objetivo é que "+55 (11) 99999-8888", "011 99999-8888", "0 21 11 99999-8888"
 * e "11999998888" virem a mesma chave ("11999998888") para comparação.
 */
object PhoneNumbers {

    /** Prefixos de serviços especiais (não geográficos) com 4 dígitos, ex.: 0800 1234567. */
    private val SPECIAL_SERVICE_PREFIXES = listOf("0300", "0303", "0500", "0800", "0900")

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        var digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        // Número internacional explícito.
        if (trimmed.startsWith("+") || digits.startsWith("00")) {
            digits = digits.removePrefix("00")
            return if (digits.startsWith("55")) normalizeNational(digits.drop(2)) else "+$digits"
        }

        // "55" + DDD + número sem o "+".
        if (digits.startsWith("55") && digits.length in 12..13) {
            return normalizeNational(digits.drop(2))
        }
        return normalizeNational(digits)
    }

    private fun normalizeNational(digits: String): String {
        if (SPECIAL_SERVICE_PREFIXES.any { digits.startsWith(it) }) return digits
        if (digits.startsWith("0")) {
            return when (digits.length) {
                // 0 + operadora (2) + DDD (2) + número (8 ou 9)
                13, 14 -> digits.drop(3)
                // 0 + DDD (2) + número (8 ou 9)
                11, 12 -> digits.drop(1)
                else -> digits
            }
        }
        return digits
    }

    /** Formata para exibição: (11) 99999-8888, 0800 123 4567 etc. */
    fun format(normalized: String): String {
        if (normalized.isEmpty()) return "Número oculto"
        if (normalized.startsWith("+")) return normalized
        val d = normalized
        return when {
            SPECIAL_SERVICE_PREFIXES.any { d.startsWith(it) } && d.length == 11 ->
                "${d.take(4)} ${d.substring(4, 7)} ${d.substring(7)}"
            d.length == 11 -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.substring(7)}"
            d.length == 10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.substring(6)}"
            d.length == 9 -> "${d.take(5)}-${d.substring(5)}"
            d.length == 8 && (d.startsWith("4004") || d.startsWith("3003")) -> "${d.take(4)} ${d.substring(4)}"
            d.length == 8 -> "${d.take(4)}-${d.substring(4)}"
            else -> d
        }
    }
}
