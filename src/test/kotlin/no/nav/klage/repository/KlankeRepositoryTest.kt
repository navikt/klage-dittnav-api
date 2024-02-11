package no.nav.klage.repository

import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.domain.titles.Innsendingsytelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime


@ActiveProfiles("dbtest")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KlankeRepositoryTest(
    @Autowired
    private val klageRepository: KlageRepository,
    @Autowired
    private val ankeRepository: AnkeRepository,
    @Autowired
    private val klankeRepository: KlankeRepository,
    @Autowired
    private val testEntityManager: TestEntityManager,
) {

    companion object {
        @Container
        @JvmField
        val postgreSQLContainer: TestPostgresqlContainer = TestPostgresqlContainer.instance
    }

    @Test
    fun `persist klage works`() {

        val klage = testEntityManager.persistAndFlush(
            Klage(
                checkboxesSelected = mutableListOf(),
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                tema = Tema.FRI,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                internalSaksnummer = "int123",
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.PEN,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
            )
        )

        testEntityManager.clear()

        assertThat(klageRepository.findAll().first()).isEqualTo(klage)
    }

    @Test
    fun `persist anke works`() {

        val anke = testEntityManager.persistAndFlush(
            Anke(
                enhetsnummer = "abc123",
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                tema = Tema.FRI,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                internalSaksnummer = "int123",
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.PEN,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
            )
        )

        testEntityManager.clear()

        assertThat(ankeRepository.findAll().first()).isEqualTo(anke)
    }

    @Test
    fun `persist vedlegg works`() {

        val klanke = testEntityManager.persistAndFlush(
            Anke(
                enhetsnummer = "abc123",
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                tema = Tema.FRI,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                internalSaksnummer = "int123",
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.PEN,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
            )
        )

        testEntityManager.clear()

        val vedlegg = Vedlegg(
            tittel = "v1",
            ref = "rtneioarsl",
            contentType = "pdf",
            sizeInBytes = 512,
        )
        klanke.vedlegg += vedlegg
        ankeRepository.save(klanke)

        testEntityManager.flush()
        testEntityManager.clear()

        assertThat(klankeRepository.findAll().first().vedlegg.first()).isEqualTo(vedlegg)
    }

}
