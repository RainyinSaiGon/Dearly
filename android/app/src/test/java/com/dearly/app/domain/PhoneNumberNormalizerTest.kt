package com.dearly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun localVietnameseMobileNumberIsConvertedToE164() {
        assertEquals("+84818916621", PhoneNumberNormalizer.toE164("0818916621"))
    }

    @Test
    fun formattedVietnameseE164NumberIsNormalized() {
        assertEquals("+84818916621", PhoneNumberNormalizer.toE164("+84 818 916 621"))
    }

    @Test
    fun invalidVietnameseNumberIsRejected() {
        assertNull(PhoneNumberNormalizer.toE164("081891662"))
        assertNull(PhoneNumberNormalizer.toE164("0123456789"))
    }
}
