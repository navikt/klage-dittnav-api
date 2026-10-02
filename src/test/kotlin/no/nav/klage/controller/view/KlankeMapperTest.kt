package no.nav.klage.controller.view

import no.nav.klage.createTestKlanke
import no.nav.klage.domain.KlageAnkeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class KlankeMapperTest {
    @Test
    fun `SENDING is shown to the user as DONE with finalizedDate`() {
        val klanke = createTestKlanke(status = KlageAnkeStatus.SENDING, markedCompleted = LocalDateTime.now())

        val view = klanke.toKlankeView(userHasDocumentForThisTema = null)

        assertEquals(KlageAnkeStatus.DONE, view.status)
        assertEquals(klanke.modifiedByUser.toLocalDate(), view.finalizedDate)
    }

    @Test
    fun `DRAFT has no finalizedDate`() {
        val view = createTestKlanke(status = KlageAnkeStatus.DRAFT).toKlankeView(userHasDocumentForThisTema = null)

        assertEquals(KlageAnkeStatus.DRAFT, view.status)
        assertNull(view.finalizedDate)
    }

    @Test
    fun `finalized view uses markedCompleted when present`() {
        val markedCompleted = LocalDateTime.now().minusDays(1)
        val klanke = createTestKlanke(status = KlageAnkeStatus.DONE, markedCompleted = markedCompleted)

        assertEquals(markedCompleted.toLocalDate(), klanke.toKlankeFinalizedView().finalizedDate)
    }

    @Test
    fun `finalized view falls back to modifiedByUser for legacy DONE without markedCompleted`() {
        val modifiedByUser = LocalDateTime.now().minusYears(1)
        val klanke = createTestKlanke(status = KlageAnkeStatus.DONE, modifiedByUser = modifiedByUser)

        val view = klanke.toKlankeFinalizedView()

        assertEquals(modifiedByUser.toLocalDate(), view.finalizedDate)
        assertEquals(modifiedByUser, view.modifiedByUser)
    }

    @Test
    fun `finalized view fails for SENDING without markedCompleted`() {
        val klanke = createTestKlanke(status = KlageAnkeStatus.SENDING)

        assertThrows<IllegalStateException> { klanke.toKlankeFinalizedView() }
    }
}
