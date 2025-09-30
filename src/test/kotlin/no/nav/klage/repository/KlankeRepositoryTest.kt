package no.nav.klage.repository

import no.nav.klage.db.TestPostgresqlContainer
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Type
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Sak
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
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
            Klanke(
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                sak = Sak(
                    sakstype = "FAGSAK",
                    fagsaksystem = "FS123",
                    fagsakid = "int123",
                ),
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.ALDERSPENSJON,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
                type = Type.KLAGE,
                caseIsAtKA = null,
            )
        )

        testEntityManager.clear()

        assertThat(klankeRepository.findAll().first()).isEqualTo(klage)
    }

    @Test
    fun `persist anke works`() {

        val anke = testEntityManager.persistAndFlush(
            Klanke(
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                sak = Sak(
                    sakstype = "FAGSAK",
                    fagsaksystem = "FS123",
                    fagsakid = "int123",
                ),
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.ALDERSPENSJON,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
                type = Type.ANKE,
                caseIsAtKA = true,
            )
        )

        testEntityManager.clear()

        assertThat(klankeRepository.findAll().first()).isEqualTo(anke)
    }

    @Test
    fun `persist vedlegg works`() {

        val klanke = testEntityManager.persistAndFlush(
            Klanke(
                foedselsnummer = "12345678910",
                fritekst = "hei",
                status = KlageAnkeStatus.DRAFT,
                userSaksnummer = "123",
                journalpostId = "abc",
                vedtakDate = LocalDate.now(),
                sak = Sak(
                    sakstype = "FAGSAK",
                    fagsaksystem = "FS123",
                    fagsakid = "int123",
                ),
                language = LanguageEnum.NB,
                innsendingsytelse = Innsendingsytelse.ALDERSPENSJON,
                hasVedlegg = true,
                pdfDownloaded = null,
                vedlegg = mutableSetOf(),
                created = LocalDateTime.now(),
                modifiedByUser = LocalDateTime.now(),
                type = Type.ANKE,
                caseIsAtKA = true,
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
        klankeRepository.save(klanke)

        testEntityManager.flush()
        testEntityManager.clear()

        assertThat(klankeRepository.findAll().first().vedlegg.first()).isEqualTo(vedlegg)
    }

}
