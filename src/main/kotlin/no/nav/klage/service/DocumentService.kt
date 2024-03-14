package no.nav.klage.service

import no.nav.klage.clients.safselvbetjening.SafselvbetjeningGraphQlClient
import no.nav.klage.clients.safselvbetjening.SafselvbetjeningRestClient
import no.nav.klage.domain.exception.FileNotFoundInSafException
import no.nav.klage.util.getLogger
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.io.RandomAccessStreamCache
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


@Service
class DocumentService(
    private val safselvbetjeningRestClient: SafselvbetjeningRestClient,
    private val safselvbetjeningGraphQlClient: SafselvbetjeningGraphQlClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getPathToDocumentPdfAndTitle(journalpostId: String): Pair<Path, String> {
        val journalpostInfo = safselvbetjeningGraphQlClient.getJournalpostById(journalpostId = journalpostId)

        if (journalpostInfo.data?.journalpostById?.dokumenter.isNullOrEmpty()) {
            throw FileNotFoundInSafException("Fikk ikke hentet fil fra arkivet.")
        }

        val journalpostIdAndDokumentInfoIdList = journalpostInfo.data!!.journalpostById!!.dokumenter.map {
            journalpostId to it.dokumentInfoId
        }

        return mergeJournalfoerteDocuments(
            documentsToMerge = journalpostIdAndDokumentInfoIdList,
            title = journalpostInfo.data.journalpostById!!.tittel
        )
    }

    private fun mergeJournalfoerteDocuments(
        documentsToMerge: List<Pair<String, String>>,
        title: String = "merged document"
    ): Pair<Path, String> {
        if (documentsToMerge.isEmpty()) {
            throw RuntimeException("No documents to merge")
        }

        val merger = PDFMergerUtility()

        val pdDocumentInformation = PDDocumentInformation()
        pdDocumentInformation.title = title
        merger.destinationDocumentInformation = pdDocumentInformation

        val pathToMergedDocument = Files.createTempFile(null, null)
        pathToMergedDocument.toFile().deleteOnExit()

        merger.destinationFileName = pathToMergedDocument.toString()

        val documentsWithPaths = documentsToMerge.map {
            val tmpFile = Files.createTempFile("", "")
            it to tmpFile
        }

        Flux.fromIterable(documentsWithPaths).flatMapSequential { (document, path) ->
            safselvbetjeningRestClient.downloadDocumentAsMono(
                journalpostId = document.first,
                dokumentInfoId = document.second,
                pathToFile = path,
            )
        }.collectList().block()

        documentsWithPaths.forEach { (_, path) ->
            merger.addSource(path.toFile())
        }

        //just under 256 MB before using file system
        merger.mergeDocuments(getMixedMemorySettingsForPDFBox(250_000_000))

        //clean tmp files that were downloaded from SAF
        try {
            documentsWithPaths.forEach { (_, pathToTmpFile) ->
                pathToTmpFile.toFile().delete()
            }
        } catch (e: Exception) {
            logger.warn("couldn't delete tmp files", e)
        }

        return pathToMergedDocument to title
    }

    @Throws(IOException::class)
    private fun getMixedMemorySettingsForPDFBox(bytes: Long): RandomAccessStreamCache.StreamCacheCreateFunction {
        return MemoryUsageSetting.setupMixed(bytes).streamCache
    }
}