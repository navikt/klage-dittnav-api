package no.nav.klage.service

import no.nav.klage.clients.safselvbetjening.SafSelvbetjeningGraphQlClient
import no.nav.klage.kodeverk.Tema
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

    fun userHasDocumentForTema(tema: Tema, userIdent: String): Boolean {
        val usersDocuments = safselvbetjeningGraphQlClient.getDokumentoversikt(ident = userIdent).data?.dokumentoversiktSelvbetjening?.tema?.map { it.kode }
        return usersDocuments?.contains(tema.name) ?: false
    }
}