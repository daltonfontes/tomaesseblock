package com.tomaesseblock.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `remove codigo do pais e formatacao`() {
        assertEquals("11999998888", PhoneNumbers.normalize("+55 (11) 99999-8888"))
        assertEquals("11999998888", PhoneNumbers.normalize("5511999998888"))
        assertEquals("11999998888", PhoneNumbers.normalize("0055 11 99999 8888"))
    }

    @Test
    fun `remove zero de longa distancia e codigo de operadora`() {
        assertEquals("11999998888", PhoneNumbers.normalize("011 99999-8888"))
        assertEquals("1133334444", PhoneNumbers.normalize("01133334444"))
        assertEquals("11999998888", PhoneNumbers.normalize("0 21 11 99999-8888"))
        assertEquals("1133334444", PhoneNumbers.normalize("0151133334444"))
    }

    @Test
    fun `mantem servicos especiais`() {
        assertEquals("03031234567", PhoneNumbers.normalize("0303 123 4567"))
        assertEquals("08001234567", PhoneNumbers.normalize("0800-123-4567"))
    }

    @Test
    fun `numero internacional mantem o mais`() {
        assertEquals("+12125551234", PhoneNumbers.normalize("+1 212 555 1234"))
    }

    @Test
    fun `vazio ou oculto`() {
        assertEquals("", PhoneNumbers.normalize(null))
        assertEquals("", PhoneNumbers.normalize(""))
        assertEquals("", PhoneNumbers.normalize("Desconhecido"))
    }

    @Test
    fun `formatacao para exibicao`() {
        assertEquals("(11) 99999-8888", PhoneNumbers.format("11999998888"))
        assertEquals("(11) 3333-4444", PhoneNumbers.format("1133334444"))
        assertEquals("0303 123 4567", PhoneNumbers.format("03031234567"))
        assertEquals("Número oculto", PhoneNumbers.format(""))
    }
}
