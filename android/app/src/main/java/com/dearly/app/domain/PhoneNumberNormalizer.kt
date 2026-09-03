package com.dearly.app.domain

/** Converts common Vietnamese mobile-number input into the E.164 form Firebase requires. */
object PhoneNumberNormalizer {
    private val vietnamMobileNumber = Regex("^0(3|5|7|8|9)\\d{8}$")
    private val vietnamCountryCodeNumber = Regex("^84(3|5|7|8|9)\\d{8}$")
    private val vietnamE164Number = Regex("^\\+84(3|5|7|8|9)\\d{8}$")

    fun toE164(input: String): String? {
        val compact = input.trim().replace(Regex("[\\s().-]"), "")
        return when {
            vietnamE164Number.matches(compact) -> compact
            vietnamCountryCodeNumber.matches(compact) -> "+$compact"
            vietnamMobileNumber.matches(compact) -> "+84${compact.drop(1)}"
            else -> null
        }
    }
}
