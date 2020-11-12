package no.nav.klage.util

import no.nav.klage.domain.klage.VedtakType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakParserTest {

    private val earlierVedtakTextWithDate = "Tidligere vedtak - 04.11.2020"
    private val earlierVedtakText = "Tidligere vedtak"
    private val invalidVedtakText = "Invalid vedtak text - 04.11.2020"
    private val latestVedtakText = "Siste vedtak"
    private val vedtakDate = LocalDate.of(2020, 11, 4)

    @Test
    fun `vedtakTypeFromVedtak should return VedtakType when vedtak is valid`() {
        assertEquals(VedtakType.EARLIER, vedtakTypeFromVedtak(earlierVedtakTextWithDate))
    }

    @Test
    fun `vedtakTypeFromVedtak should return null when vedtak is invalid`() {
        assertNull(vedtakTypeFromVedtak(invalidVedtakText))
    }

    @Test
    fun `vedtakDateFromVedtak should return date when vedtak is valid`() {
        assertEquals(vedtakDate, vedtakDateFromVedtak(earlierVedtakTextWithDate))
    }

    @Test
    fun `vedtakDateFromVedtak should not return date when vedtak is invalid`() {
        assertNull(vedtakDateFromVedtak(invalidVedtakText))
    }

    @Test
    fun `vedtakFromTypeAndDate should return earlier vedtak with date when type is earlier and date exists`() {
        assertEquals(earlierVedtakTextWithDate, vedtakFromTypeAndDate(VedtakType.EARLIER, vedtakDate))
    }

    @Test
    fun `vedtakFromTypeAndDate should return earlier vedtak with date when type is null and date exists`() {
        assertEquals(earlierVedtakTextWithDate, vedtakFromTypeAndDate(null, vedtakDate))
    }

    @Test
    fun `vedtakFromTypeAndDate should return earlier vedtak without date when type is earlier and date is null`() {
        assertEquals(earlierVedtakText, vedtakFromTypeAndDate(VedtakType.EARLIER, null))
    }

    @Test
    fun `vedtakFromTypeAndDate should return latest vedtak without date when type is later and date is null`() {
        assertEquals(latestVedtakText, vedtakFromTypeAndDate(VedtakType.LATEST, null))
    }

    @Test
    fun `vedtakFromTypeAndDate should return latest vedtak without date when type is later and date exists`() {
        assertEquals(latestVedtakText, vedtakFromTypeAndDate(VedtakType.LATEST, vedtakDate))
    }

    @Test
    fun `vedtakFromTypeAndDate should return null date when type is null and date is null`() {
        assertNull(vedtakFromTypeAndDate(null, null))
    }
}