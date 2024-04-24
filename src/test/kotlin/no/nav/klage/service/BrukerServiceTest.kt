package no.nav.klage.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
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
    private val request: HttpServletRequest = mockk()
    private val brukerService = BrukerService(pdlClient, tokenUtil, request)
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
        every { pdlClient.getPersonInfo(any()) } returns hentPdlPersonResponse
        every { postDataDAO.findPostData(any()).get().city } returns poststed
        every { tokenUtil.getSelvbetjeningExpiry() } returns 1
        every { request.getHeader(any()) } returns idPortenToken
        val expectedOutput = Bruker(
            Navn(fornavn, mellomnavn, etternavn),
            Adresse(adressenavn, postnummer, poststed, husnummer, husbokstav),
            Kontaktinformasjon("$landskode $nummer", null),
            Identifikator(idType, folkeregisteridentifikator),
            1658242074000
        )
        val output: Bruker = brukerService.getBruker()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `should throw exception when name is missing from PDL`() {
        every { pdlClient.getPersonInfo(any()) } returns hentPdlPersonResponseWithMissingNavn
        every { tokenUtil.getSelvbetjeningExpiry() } returns 1

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            brukerService.getBruker()
        }

        assertEquals("Navn missing", exception.message)
    }

    @Test
    fun `should receive poststed null when missing in pam-geograaphy`() {
        every { pdlClient.getPersonInfo(any()) } returns hentPdlPersonResponseWithWrongPostnummer
        every { tokenUtil.getSelvbetjeningExpiry() } returns 1
        every { request.getHeader(any()) } returns idPortenToken

        val output: Bruker = brukerService.getBruker()

        assertNull(output.adresse?.poststed)
    }

    val idPortenToken = "Bearer eyJraWQiOiJ2UHBaZW9HOGRkTHpmdHMxLWxnc3VnOHNyYVd3bW04dHhJaGJ3Y1h3R01JIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJFeHpOd0U0czVoS1ZfWEFoLV9VX0tlMGFWdDNhZDhVWlhIT1lFZzgweUJrPSIsImlzcyI6Imh0dHBzOlwvXC9vaWRjLXZlcjIuZGlmaS5ub1wvaWRwb3J0ZW4tb2lkYy1wcm92aWRlclwvIiwiY2xpZW50X2FtciI6InByaXZhdGVfa2V5X2p3dCIsInBpZCI6IjA1OTA2NDk4MDQwIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImNsaWVudF9pZCI6IjI4MzIwYThlLTYzMjUtNGJmNC1iMDVjLTc3MjhmOGY3YjFiYyIsImF1ZCI6Imh0dHBzOlwvXC9uYXYubm8iLCJhY3IiOiJMZXZlbDQiLCJzY29wZSI6Im9wZW5pZCIsImV4cCI6MTY1ODI0MjA3NCwiaWF0IjoxNjU4MjM4NDc0LCJjbGllbnRfb3Jnbm8iOiI4ODk2NDA3ODIiLCJqdGkiOiI2a3FIUXhuZy1wOGRIZ1oxV0FPTnZCT1FxWmVEM3NZZ3Bubm05WURmSjZNIiwiY29uc3VtZXIiOnsiYXV0aG9yaXR5IjoiaXNvNjUyMy1hY3RvcmlkLXVwaXMiLCJJRCI6IjAxOTI6ODg5NjQwNzgyIn19.0ZFT612gZJ2PM7OBi81AvgRf4IUCNseB-FAZhyNUl6PVNZe5DLRjippkEqNuXqmgGEzk9DnzsOiQJBkKYBnZX-Q60m0TmQ12ET8gXvbqFCDSkWAIGpM4gvjJ_iM1b-D4gfBocKnIBl6Lu0w4o5p6iu46l5XZO_Izv4ieEaRDTewaQwxZDEJEQQAmGQBkf8_epo335UYtejhvnbvC_FMWI6Zs372CsR8oc1V4ndI06lbTjq9GmGFbTghG82rhW77seFTqzaNwZOrji1sD_snYkd11GTEZUEQb804mK1_bfR1S-73xBnSUNGk9dUioRd884naVkSqKA21QWGHGt2foXA"

    @Test
    fun `should throw exception when folkeregisteridentifikator is missing from PDL`() {
        every { pdlClient.getPersonInfo(any()) } returns hentPdlPersonResponseWithMissingFolkeregisteridentifikator
        every { tokenUtil.getSelvbetjeningExpiry() } returns 1

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            brukerService.getBruker()
        }

        assertEquals("Folkeregisteridentifikator missing", exception.message)
    }
}
