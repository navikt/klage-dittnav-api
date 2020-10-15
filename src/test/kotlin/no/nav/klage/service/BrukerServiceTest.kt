package no.nav.klage.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.*
import no.nav.klage.domain.Navn
import no.nav.klage.util.TokenUtil
import no.nav.pam.geography.PostDataDAO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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


    private val hentPdlPersonResponse: HentPdlPersonResponse = createFullPdlPersonResponse()
    private val hentPdlPersonResponseWithMissingNavn: HentPdlPersonResponse = createPdlPersonResponseWithMissingNavn()
    private val hentPdlPersonResponseWithMissingFolkeregisteridentifikator: HentPdlPersonResponse =
        createPdlPersonResponseWithMissingFolkeregisteridentifikator()
    private val hentPdlPersonResponseWithWrongPostnummer: HentPdlPersonResponse =
        createPdlPersonResponseWithWrongPostnummer()

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

    @BeforeEach
    fun init() {
        clearAllMocks()
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
    fun `should handle exception from pam-geography`() {
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
}
