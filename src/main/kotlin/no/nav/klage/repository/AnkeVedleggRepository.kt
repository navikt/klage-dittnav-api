package no.nav.klage.repository

import no.nav.klage.util.getLogger
import org.springframework.stereotype.Repository

@Repository
class AnkeVedleggRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }
}
