package com.wanlv.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLoginValidationTest {
    @Test
    fun mainlandPhoneValidation_matchesBackendRule() {
        assertTrue(isValidMainlandPhone("13800138000"))
        assertFalse(isValidMainlandPhone("12800138000"))
        assertFalse(isValidMainlandPhone("1380013800"))
        assertFalse(isValidMainlandPhone("138001380000"))
    }

    @Test
    fun verificationCodeValidation_requiresSixDigits() {
        assertTrue(isValidVerificationCode("123456"))
        assertFalse(isValidVerificationCode("12345"))
        assertFalse(isValidVerificationCode("12345a"))
    }

    @Test
    fun invalidExpireSeconds_fallsBackToFiveMinutes() {
        assertEquals(300, normalizeCodeExpireSeconds(0))
        assertEquals(300, normalizeCodeExpireSeconds(-1))
        assertEquals(120, normalizeCodeExpireSeconds(120))
    }
}
