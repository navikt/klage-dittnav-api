package no.nav.klage.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.*
import no.nav.klage.domain.Navn
import no.nav.klage.domain.exception.FullmaktNotFoundException
import no.nav.klage.util.TokenUtil
import no.nav.pam.geography.PostDataDAO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

internal class BrukerServiceTest {
    private val pdlClient: PdlClient = mockk()
    private val postDataDAO: PostDataDAO = mockk()
    private val tokenUtil: TokenUtil = mockk()
    private val brukerService = BrukerService(pdlClient, tokenUtil)
    private val fornavn = "Fornavn"
    private val mellomnavn = "Mellomnavn"
    private val etternavn = "Etternavn"
    private val husnummer = "Husnummer"
    private val husbokstav = "Husbokstav"
    private val adressenavn = "Adressenavn"
    private val postnummer = "0187"
    private val poststed = "OSLO"
    private val landskode = "Landskode"
    private val nummer = "Nummer"
    private val folkeregisteridentifikator = "12345678910"
    private val status = "I_BRUK"
    private val idType = "FNR"

    private val motpartsPersonIdent = "motpartsPersonIdent"
    private val motpartsPersonIdent2 = "motpartsPersonIdent2"
    private val motpartsRolle = FullmaktsRolle.FULLMEKTIG
    private val omraadeFOR = Tema.FOR
    private val omraadeSYK = Tema.SYK
    private val gyldigFraOgMed = LocalDate.now().minusYears(1)
    private val gyldigTilOgMed = LocalDate.now().plusYears(1)


    private val hentPdlPersonResponse: HentPdlPersonResponse = createFullPdlPersonResponse()
    private val hentPdlPersonResponseWithMissingNavn: HentPdlPersonResponse = createPdlPersonResponseWithMissingNavn()
    private val hentPdlPersonResponseWithMissingFolkeregisteridentifikator: HentPdlPersonResponse =
        createPdlPersonResponseWithMissingFolkeregisteridentifikator()
    private val hentPdlPersonResponseWithWrongPostnummer: HentPdlPersonResponse =
        createPdlPersonResponseWithWrongPostnummer()
    private val hentFullmektigResponse: HentFullmektigResponse =
        createFullmektigInfoWithSystemUser()
    private val hentExpiredFullmektigResponse: HentFullmektigResponse =
        createExpiredFullmektigInfoWithSystemUser()
    private val hentFutureFullmektigResponse: HentFullmektigResponse =
        createFutureFullmektigInfoWithSystemUser()
    private val hentFullmektigForAllOmraaderResponse: HentFullmektigResponse =
        createFullmektigForAllOmraaderInfoWithSystemUser()


    private fun createFullPdlPersonResponse(): HentPdlPersonResponse {
        return HentPdlPersonResponse(
            HentPerson(
                Person(
                    emptyList(),
                    listOf(
                        no.nav.klage.clients.pdl.Navn(
                            fornavn,
                            mellomnavn,
                            etternavn
                        )
                    ),
                    listOf(
                        Bostedsadresse(
                            null,
                            null,
                            VegAdresse(
                                null,
                                husnummer,
                                husbokstav,
                                null,
                                adressenavn,
                                null,
                                null,
                                postnummer,
                                null
                            ),
                            null,
                            null
                        )
                    ),
                    listOf(
                        Telefonnummer(
                            landskode,
                            nummer,
                            null
                        )
                    ),
                    listOf(
                        Folkeregisteridentifikator(
                            folkeregisteridentifikator,
                            idType,
                            status
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createPdlPersonResponseWithMissingNavn(): HentPdlPersonResponse {
        return HentPdlPersonResponse(
            HentPerson(
                Person(
                    emptyList(),
                    emptyList(),
                    listOf(
                        Bostedsadresse(
                            null,
                            null,
                            VegAdresse(
                                null,
                                husnummer,
                                husbokstav,
                                null,
                                adressenavn,
                                null,
                                null,
                                postnummer,
                                null
                            ),
                            null,
                            null
                        )
                    ),
                    listOf(
                        Telefonnummer(
                            landskode,
                            nummer,
                            null
                        )
                    ),
                    listOf(
                        Folkeregisteridentifikator(
                            folkeregisteridentifikator,
                            idType,
                            status
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createPdlPersonResponseWithMissingFolkeregisteridentifikator(): HentPdlPersonResponse {
        return HentPdlPersonResponse(
            HentPerson(
                Person(
                    emptyList(),
                    listOf(
                        no.nav.klage.clients.pdl.Navn(
                            fornavn,
                            mellomnavn,
                            etternavn
                        )
                    ),
                    listOf(
                        Bostedsadresse(
                            null,
                            null,
                            VegAdresse(
                                null,
                                husnummer,
                                husbokstav,
                                null,
                                adressenavn,
                                null,
                                null,
                                postnummer,
                                null
                            ),
                            null,
                            null
                        )
                    ),
                    listOf(
                        Telefonnummer(
                            landskode,
                            nummer,
                            null
                        )
                    ),
                    emptyList()
                )
            ),
            null
        )
    }

    private fun createPdlPersonResponseWithWrongPostnummer(): HentPdlPersonResponse {
        return HentPdlPersonResponse(
            HentPerson(
                Person(
                    emptyList(),
                    listOf(
                        no.nav.klage.clients.pdl.Navn(
                            fornavn,
                            mellomnavn,
                            etternavn
                        )
                    ),
                    listOf(
                        Bostedsadresse(
                            null,
                            null,
                            VegAdresse(
                                null,
                                husnummer,
                                husbokstav,
                                null,
                                adressenavn,
                                null,
                                null,
                                "9999",
                                null
                            ),
                            null,
                            null
                        )
                    ),
                    listOf(
                        Telefonnummer(
                            landskode,
                            nummer,
                            null
                        )
                    ),
                    listOf(
                        Folkeregisteridentifikator(
                            folkeregisteridentifikator,
                            idType,
                            status
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createFullmektigInfoWithSystemUser(): HentFullmektigResponse {
        return HentFullmektigResponse(
            HentFullmektig(
                FullmektigWrapper(
                    listOf(
                        Fullmakt(
                            motpartsPersonIdent,
                            motpartsRolle,
                            listOf(
                                omraadeFOR.name
                            ),
                            gyldigFraOgMed,
                            gyldigTilOgMed
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createFullmektigForAllOmraaderInfoWithSystemUser(): HentFullmektigResponse {
        return HentFullmektigResponse(
            HentFullmektig(
                FullmektigWrapper(
                    listOf(
                        Fullmakt(
                            motpartsPersonIdent,
                            motpartsRolle,
                            listOf(
                                "*"
                            ),
                            gyldigFraOgMed,
                            gyldigTilOgMed
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createExpiredFullmektigInfoWithSystemUser(): HentFullmektigResponse {
        return HentFullmektigResponse(
            HentFullmektig(
                FullmektigWrapper(
                    listOf(
                        Fullmakt(
                            motpartsPersonIdent,
                            motpartsRolle,
                            listOf(
                                omraadeFOR.name
                            ),
                            gyldigFraOgMed,
                            gyldigFraOgMed
                        )
                    )
                )
            ),
            null
        )
    }

    private fun createFutureFullmektigInfoWithSystemUser(): HentFullmektigResponse {
        return HentFullmektigResponse(
            HentFullmektig(
                FullmektigWrapper(
                    listOf(
                        Fullmakt(
                            motpartsPersonIdent,
                            motpartsRolle,
                            listOf(
                                omraadeFOR.name
                            ),
                            gyldigTilOgMed,
                            gyldigTilOgMed
                        )
                    )
                )
            ),
            null
        )
    }


    @BeforeEach
    fun init() {
        clearAllMocks()
        ReflectionTestUtils.setField(brukerService, "allFullmaktOmraader", "*")
    }

    @Test
    fun `should convert from pdl format to Bruker object`() {
        every { pdlClient.getPersonInfo() } returns hentPdlPersonResponse
        every { postDataDAO.findPostData(any()).get().city } returns poststed
        every { tokenUtil.getExpiry() } returns 1
        val expectedOutput = Bruker(
            Navn(fornavn, mellomnavn, etternavn),
            Adresse(adressenavn, postnummer, poststed, husnummer, husbokstav),
            Kontaktinformasjon("$landskode $nummer", null),
            Identifikator(idType, folkeregisteridentifikator),
            1
        )
        val output: Bruker = brukerService.getBruker()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `should throw exception when name is missing from PDL`() {
        every { pdlClient.getPersonInfo() } returns hentPdlPersonResponseWithMissingNavn
        every { tokenUtil.getExpiry() } returns 1

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            brukerService.getBruker()
        }

        assertEquals("Navn missing", exception.message)
    }

    @Test
    fun `should receive poststed null when missing in pam-geograaphy`() {
        every { pdlClient.getPersonInfo() } returns hentPdlPersonResponseWithWrongPostnummer
        every { tokenUtil.getExpiry() } returns 1

        val output: Bruker = brukerService.getBruker()

        assertNull(output.adresse?.poststed)
    }

    @Test
    fun `should throw exception when folkeregisteridentifikator is missing from PDL`() {
        every { pdlClient.getPersonInfo() } returns hentPdlPersonResponseWithMissingFolkeregisteridentifikator
        every { tokenUtil.getExpiry() } returns 1

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            brukerService.getBruker()
        }

        assertEquals("Folkeregisteridentifikator missing", exception.message)
    }

    @Test
    fun `should verify fullmakt when present in PDL`() {
        every { pdlClient.getFullmektigInfoWithSystemUser(folkeregisteridentifikator) } returns hentFullmektigResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent
        brukerService.verifyFullmakt(omraadeFOR, folkeregisteridentifikator)
    }

    @Test
    fun `should verify fullmakt for all omraader when present in PDL`() {
        every { pdlClient.getFullmektigInfoWithSystemUser(folkeregisteridentifikator) } returns hentFullmektigForAllOmraaderResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent
        brukerService.verifyFullmakt(omraadeFOR, folkeregisteridentifikator)
    }

    @Test
    fun `should throw exception when requested omraade is not present in fullmakt in PDL`() {
        every { pdlClient.getFullmektigInfoWithSystemUser(any()) } returns hentFullmektigResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent

        Assertions.assertThrows(FullmaktNotFoundException::class.java) {
            brukerService.verifyFullmakt(omraadeSYK, folkeregisteridentifikator)
        }
    }

    @Test
    fun `should throw exception when current user is not present in fullmakt in PDL`() {
        every { pdlClient.getFullmektigInfoWithSystemUser(any()) } returns hentFullmektigResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent2

        Assertions.assertThrows(FullmaktNotFoundException::class.java) {
            brukerService.verifyFullmakt(omraadeSYK, folkeregisteridentifikator)
        }
    }

    @Test
    fun `should throw exception when fullmakt in PDL is expired`() {
        every { pdlClient.getFullmektigInfoWithSystemUser(any()) } returns hentExpiredFullmektigResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent

        Assertions.assertThrows(FullmaktNotFoundException::class.java) {
            brukerService.verifyFullmakt(omraadeSYK, folkeregisteridentifikator)
        }
    }

    @Test
    fun `should throw exception when fullmakt in PDL isn't initiated `() {
        every { pdlClient.getFullmektigInfoWithSystemUser(any()) } returns hentFutureFullmektigResponse
        every { tokenUtil.getSubject() } returns motpartsPersonIdent

        Assertions.assertThrows(FullmaktNotFoundException::class.java) {
            brukerService.verifyFullmakt(omraadeSYK, folkeregisteridentifikator)
        }
    }
}
