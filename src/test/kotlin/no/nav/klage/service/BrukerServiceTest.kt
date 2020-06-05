package no.nav.klage.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.pdl.*
import no.nav.klage.domain.*
import no.nav.klage.domain.Navn
import no.nav.pam.geography.PostDataDAO

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BrukerServiceTest {
    private val pdlClient: PdlClient = mockk()
    private val postDataDAO: PostDataDAO = mockk()
    private val brukerService = BrukerService(pdlClient)
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

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @Test
    fun `should convert from pdl format to Bruker object`() {
        every { pdlClient.getPersonInfo() } returns hentPdlPersonResponse
        every { postDataDAO.findPostData(any()).get().city } returns poststed
        val expectedOutput = Bruker(
            Navn(fornavn, mellomnavn, etternavn),
            Adresse(adressenavn, postnummer, poststed, husnummer, husbokstav),
            Kontaktinformasjon("$landskode $nummer", null),
            Identifikator(idType, folkeregisteridentifikator)
        )
        val output: Bruker = brukerService.getBruker()
        assertEquals(expectedOutput, output)
    }
}
