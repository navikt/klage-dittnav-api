package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenAnkeInput
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.anke.AnkeFullInput
import no.nav.klage.domain.anke.AnkeInput
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AnkeService(
    klankeRepository: KlankeRepository,
    private val ankeRepository: AnkeRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val fileClient: FileClient,
    validationService: ValidationService,
    kafkaInternalEventService: KafkaInternalEventService,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
) : CommonService(
    klankeRepository = klankeRepository,
    validationService = validationService,
    kafkaInternalEventService = kafkaInternalEventService,
) {
    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createAnke(input: AnkeFullInput, bruker: Bruker): Anke {
        val anke = input.toAnke(bruker = bruker)
        return ankeRepository.save(anke).also {
            updateMetrics(input = anke)
        }
    }

    fun createAnke(input: AnkeInput, bruker: Bruker): Anke {
        val anke = input.toAnke(bruker = bruker)
        return ankeRepository.save(anke).also {
            updateMetrics(input = anke)
        }
    }

    fun AnkeFullInput.toAnke(bruker: Bruker): Anke {
        return Anke(
            foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            fritekst = fritekst,
            status = KlageAnkeStatus.DRAFT,
            tema = innsendingsytelse.toTema(),
            userSaksnummer = userSaksnummer,
            journalpostId = null,
            vedtakDate = vedtakDate,
            internalSaksnummer = internalSaksnummer,
            language = language,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = hasVedlegg,
            created = LocalDateTime.now(),
            modifiedByUser = LocalDateTime.now(),
            pdfDownloaded = null,
            enhetsnummer = enhetsnummer,
        )
    }

    fun AnkeInput.toAnke(bruker: Bruker): Anke {
        return Anke(
            foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            fritekst = null,
            status = KlageAnkeStatus.DRAFT,
            tema = innsendingsytelse.toTema(),
            userSaksnummer = null,
            journalpostId = null,
            vedtakDate = null,
            internalSaksnummer = internalSaksnummer,
            language = LanguageEnum.NB,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = false,
            created = LocalDateTime.now(),
            modifiedByUser = LocalDateTime.now(),
            pdfDownloaded = null,
            enhetsnummer = null,
        )
    }

    private fun updateMetrics(input: Anke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            input.innsendingsytelse.toTema().toString()
        }
        klageAnkeMetrics.incrementKlagerInitialized(temaReport)
    }

    fun getDraftOrCreateAnke(input: AnkeInput, bruker: Bruker): Anke {
        val existingAnke = getLatestAnkeDraft(
            bruker = bruker,
            tema = input.innsendingsytelse.toTema(),
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
        )

        return existingAnke ?: createAnke(
            input = input,
            bruker = bruker,
        )
    }

    private fun getLatestAnkeDraft(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
    ): Anke? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val anke = ankeRepository.findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }

        return if (anke != null) {
            validationService.validateAnkeAccess(anke = anke, bruker = bruker)
            anke
        } else null
    }

    fun finalizeAnke(ankeId: UUID, bruker: Bruker): LocalDateTime {
        val existingAnke = klankeRepository.findById(ankeId).get() as Anke
        validationService.checkKlankeStatus(klanke = existingAnke, includeFinalized = false)

        if (existingAnke.isFinalized()) {
            return existingAnke.modifiedByUser
        }

        validationService.validateKlankeAccess(klanke = existingAnke, bruker = bruker)
        validationService.validateAnke(anke = existingAnke)

        existingAnke.status = KlageAnkeStatus.DONE
        existingAnke.modifiedByUser = LocalDateTime.now()

        kafkaProducer.sendToKafka(createAggregatedAnke(bruker = bruker, anke = existingAnke))
        registerFinalizedMetrics(anke = existingAnke)

        logger.debug(
            "Anke {} med tema {} er sendt inn.",
            ankeId,
            existingAnke.tema.name,
        )

        return existingAnke.modifiedByUser
    }

    private fun registerFinalizedMetrics(anke: Anke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(anke.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            anke.tema.toString()
        }
        klageAnkeMetrics.incrementAnkerFinalized(temaReport)

        if (anke.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (anke.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }
        vedleggMetrics.registerNumberOfVedleggPerUser(anke.vedlegg.size.toDouble())
    }

    fun getAnkePdf(ankeId: UUID, bruker: Bruker): ByteArray {
        val existingAnke = ankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(klanke = existingAnke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingAnke, bruker = bruker)
        requireNotNull(existingAnke.journalpostId)
        return fileClient.getKlageAnkeFile(existingAnke.journalpostId!!)
    }

    fun createAnkePdfWithFoersteside(ankeId: UUID, bruker: Bruker): ByteArray? {
        val existingAnke = ankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(klanke = existingAnke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingAnke, bruker = bruker)
        validationService.validateAnke(anke = existingAnke)

        klageDittnavPdfgenService.createAnkePdfWithFoersteside(
            createPdfWithFoerstesideInput(anke = existingAnke, bruker)
        ).also {
            setPdfDownloadedWithoutAccessValidation(klankeId = ankeId, pdfDownloaded = LocalDateTime.now())
            return it
        }
    }

    private fun createAggregatedAnke(
        bruker: Bruker,
        anke: Anke
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(anke.vedtakDate)

        return AggregatedKlageAnke(
            id = anke.id.toString(),
            fornavn = bruker.navn.fornavn,
            mellomnavn = bruker.navn.mellomnavn ?: "",
            etternavn = bruker.navn.etternavn,
            vedtak = vedtak ?: "",
            dato = anke.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(anke.fritekst!!),
            identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = anke.tema.name,
            ytelse = anke.innsendingsytelse.nb,
            vedlegg = anke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userChoices = null,
            userSaksnummer = anke.userSaksnummer,
            internalSaksnummer = anke.internalSaksnummer,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.ANKE,
            enhetsnummer = anke.enhetsnummer,
        )
    }

    fun createPdfWithFoerstesideInput(anke: Anke, bruker: Bruker): OpenAnkeInput {
        return OpenAnkeInput(
            foedselsnummer = anke.foedselsnummer,
            navn = bruker.navn,
            fritekst = anke.fritekst!!,
            userSaksnummer = anke.userSaksnummer,
            internalSaksnummer = anke.internalSaksnummer,
            vedtakDate = anke.vedtakDate,
            innsendingsytelse = anke.innsendingsytelse,
            language = anke.language,
            hasVedlegg = anke.vedlegg.isNotEmpty() || anke.hasVedlegg,
            enhetsnummer = anke.enhetsnummer!!,
        )
    }

    fun updateEnhetsnummer(
        ankeId: UUID,
        enhetsnummer: String?,
        bruker: Bruker
    ): LocalDateTime {
        val existingAnke = ankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(existingAnke)
        validationService.validateKlankeAccess(existingAnke, bruker)

        existingAnke.enhetsnummer = enhetsnummer
        existingAnke.modifiedByUser = LocalDateTime.now()

        return existingAnke.modifiedByUser
    }

    fun getAnkeDraftsByFnr(bruker: Bruker): List<Anke> {
        return ankeRepository.findByFoedselsnummerAndStatus(
            fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            status = KlageAnkeStatus.DRAFT
        )
    }
}
