package no.nav.klage.service

import no.nav.klage.clients.safselvbetjening.SafSelvbetjeningGraphQlClient
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class SafSelvbetjeningService(
    private val safselvbetjeningGraphQlClient: SafSelvbetjeningGraphQlClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    /**
     * Returnerer null hvis oppslaget mot safselvbetjening feiler, og en (potensielt tom) liste hvis oppslaget gikk bra.
     */
    fun getUsersDocumentTemas(userIdent: String): List<String?>? {
        val response =
            try {
                safselvbetjeningGraphQlClient.getDokumentoversikt(
                    ident = userIdent,
                )
            } catch (e: Exception) {
                logger.error("Fikk ikke hentet dokument-temaer i arkivet for bruker.", e)
                return null
            }

        val errors = response.errors
        if (!errors.isNullOrEmpty()) {
            logger.error(
                "Fikk feil fra safselvbetjening ved henting av dokument-temaer i arkivet for bruker: ${
                    errors.map {
                        it.extensions.classification
                    }
                }",
            )
            return null
        }

        val data = response.data
        if (data == null) {
            logger.error("Fikk ikke hentet dokument-temaer i arkivet for bruker. Data manglet i responsen fra safselvbetjening.")
            return null
        }

        return data.dokumentoversiktSelvbetjening.tema.map { it.kode }
    }
}
