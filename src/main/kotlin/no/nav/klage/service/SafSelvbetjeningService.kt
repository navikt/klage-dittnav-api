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

    fun getUsersDocumentTemas(userIdent: String): List<String?> {
        val usersDocumentTemas =
            safselvbetjeningGraphQlClient
                .getDokumentoversikt(
                    ident = userIdent,
                ).data
                ?.dokumentoversiktSelvbetjening
                ?.tema
                ?.map {
                    it.kode
                }

        if (usersDocumentTemas.isNullOrEmpty()) {
            logger.error("Fikk ikke hentet dokument-temaer i arkivet for bruker.")
            return emptyList()
        } else {
            return usersDocumentTemas
        }
    }
}
