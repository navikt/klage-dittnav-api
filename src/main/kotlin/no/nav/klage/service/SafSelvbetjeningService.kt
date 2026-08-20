package no.nav.klage.service

import no.nav.klage.clients.safselvbetjening.SafselvbetjeningGraphQlClient
import no.nav.klage.kodeverk.Tema

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class SafSelvbetjeningService(
    private val safselvbetjeningGraphQlClient: SafselvbetjeningGraphQlClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun userHasDocumentForTema(tema: Tema): Boolean {
        val usersDocuments = safselvbetjeningGraphQlClient.getDokumentoversikt(tokenUtil.getSubject()).data?.dokumentoversiktSelvbetjening?.tema?.map { it.kode }
        logger.debug("usersDocuments: $usersDocuments")
        logger.debug("tema name: " + tema.name)
        return usersDocuments?.contains(tema.name) ?: false
    }
}