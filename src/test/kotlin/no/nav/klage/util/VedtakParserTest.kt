package no.nav.klage.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakParserTest {

    private val earlierVedtakTextWithDate = "Tidligere vedtak - 04.11.2020"
    private val vedtakDate = LocalDate.of(2020, 11, 4)

    @Test
    fun `vedtakFromTypeAndDate should return earlier vedtak with date when type is null and date exists`() {
        assertEquals(earlierVedtakTextWithDate, vedtakFromDate(vedtakDate))
    }

    @Test
    fun `vedtakFromTypeAndDate should return null date when type is null and date is null`() {
        assertNull(vedtakFromDate(null))
    }
}