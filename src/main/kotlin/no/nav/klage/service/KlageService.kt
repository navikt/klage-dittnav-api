package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.klage.KlageFullInput
import no.nav.klage.domain.klage.KlageInput
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.KlageRepository
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
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val fileClient: FileClient,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
    private val klankeRepository: KlankeRepository,
    private val validationService: ValidationService,
    private val commonService: CommonService,
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createKlage(input: KlageFullInput, bruker: Bruker): Klage {
        val klage = input.toKlage(bruker = bruker)
        return klageRepository.save(klage).also {
            updateMetrics(input = klage)
        }
    }

    fun createKlage(input: KlageInput, bruker: Bruker): Klage {
        val klage = input.toKlage(bruker = bruker)
        return klageRepository.save(klage).also {
            updateMetrics(input = klage)
        }
    }

    fun KlageFullInput.toKlage(bruker: Bruker): Klage {
        return Klage(
            checkboxesSelected = checkboxesSelected.toMutableList(),
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
        )
    }

    fun KlageInput.toKlage(bruker: Bruker): Klage {
        return Klage(
            checkboxesSelected = mutableListOf(),
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
        )
    }

    private fun updateMetrics(input: Klage) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            input.innsendingsytelse.toTema().toString()
        }
        klageAnkeMetrics.incrementKlagerInitialized(temaReport)
    }

    fun getDraftOrCreateKlage(input: KlageInput, bruker: Bruker): Klage {
        val existingKlage = getLatestKlageDraft(
            bruker = bruker,
            tema = input.innsendingsytelse.toTema(),
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
        )

        return existingKlage ?: createKlage(
            input = input,
            bruker = bruker,
        )
    }

    fun getLatestKlageDraft(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
    ): Klage? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val klage = klageRepository.findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }

        return if (klage != null) {
            validationService.validateKlageAccess(klage = klage, bruker = bruker)
            klage
        } else null
    }

    fun updateCheckboxesSelected(
        klageId: UUID,
        checkboxesSelected: Set<CheckboxEnum>?,
        bruker: Bruker
    ): LocalDateTime {
        val existingKlage = klageRepository.findById(klageId).get()
        validationService.checkKlankeStatus(existingKlage)
        validationService.validateKlankeAccess(existingKlage, bruker)

        existingKlage.checkboxesSelected.clear()
        if (!checkboxesSelected.isNullOrEmpty()) {
            existingKlage.checkboxesSelected.addAll(checkboxesSelected)
        }
        existingKlage.modifiedByUser = LocalDateTime.now()

        return existingKlage.modifiedByUser
    }

    fun finalizeKlage(klageId: UUID, bruker: Bruker): LocalDateTime {
        val existingKlage = klankeRepository.findById(klageId).get() as Klage
        validationService.checkKlankeStatus(klanke = existingKlage, includeFinalized = false)

        if (existingKlage.isFinalized()) {
            return existingKlage.modifiedByUser
        }

        validationService.validateKlankeAccess(klanke = existingKlage, bruker = bruker)
        validationService.validateKlage(klage = existingKlage)

        existingKlage.status = KlageAnkeStatus.DONE
        existingKlage.modifiedByUser = LocalDateTime.now()

        kafkaProducer.sendToKafka(createAggregatedKlage(bruker = bruker, klage = existingKlage))
        registerFinalizedMetrics(klage = existingKlage)

        logger.debug(
            "Klage {} med tema {} er sendt inn.",
            klageId,
            existingKlage.tema.name,
        )

        return existingKlage.modifiedByUser
    }

    private fun registerFinalizedMetrics(klage: Klage) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(klage.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            klage.tema.toString()
        }
        klageAnkeMetrics.incrementKlagerFinalizedTitle(klage.innsendingsytelse)
        klageAnkeMetrics.incrementKlagerFinalized(temaReport)
        klageAnkeMetrics.incrementKlagerGrunn(temaReport, klage.checkboxesSelected)

        if (klage.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (klage.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }
        vedleggMetrics.registerNumberOfVedleggPerUser(klage.vedlegg.size.toDouble())
    }

    fun getKlagePdf(klageId: UUID, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.findById(klageId).get()
        validationService.checkKlankeStatus(klanke = existingKlage, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlage, bruker = bruker)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageAnkeFile(existingKlage.journalpostId!!)
    }

    fun createKlagePdfWithFoersteside(klageId: UUID, bruker: Bruker): ByteArray? {
        val existingKlage = klageRepository.findById(klageId).get()
        validationService.checkKlankeStatus(klanke = existingKlage, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlage, bruker = bruker)
        validationService.validateKlage(klage = existingKlage)

        klageDittnavPdfgenService.createKlagePdfWithFoersteside(
            createPdfWithFoerstesideInput(klage = existingKlage, bruker)
        ).also {
            commonService.setPdfDownloadedWithoutAccessValidation(klankeId = klageId, pdfDownloaded = LocalDateTime.now())
            return it
        }
    }

    private fun createAggregatedKlage(
        bruker: Bruker,
        klage: Klage
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klage.vedtakDate)

        return AggregatedKlageAnke(
            id = klage.id.toString(),
            fornavn = bruker.navn.fornavn,
            mellomnavn = bruker.navn.mellomnavn ?: "",
            etternavn = bruker.navn.etternavn,
            vedtak = vedtak ?: "",
            dato = klage.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(klage.fritekst!!),
            identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = klage.tema.name,
            ytelse = klage.innsendingsytelse.nb,
            vedlegg = klage.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userChoices = klage.checkboxesSelected.map { x -> x.getFullText(klage.language) },
            userSaksnummer = klage.userSaksnummer,
            internalSaksnummer = klage.internalSaksnummer,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.KLAGE,
            enhetsnummer = null,
        )
    }

    fun createPdfWithFoerstesideInput(klage: Klage, bruker: Bruker): OpenKlageInput {
        return OpenKlageInput(
            foedselsnummer = klage.foedselsnummer,
            navn = bruker.navn,
            fritekst = klage.fritekst!!,
            userSaksnummer = klage.userSaksnummer,
            internalSaksnummer = klage.internalSaksnummer,
            vedtakDate = klage.vedtakDate,
            innsendingsytelse = klage.innsendingsytelse,
            checkboxesSelected = klage.checkboxesSelected.toSet(),
            language = klage.language,
            hasVedlegg = klage.vedlegg.isNotEmpty() || klage.hasVedlegg,
        )
    }

    fun getKlageDraftsByFnr(bruker: Bruker): List<Klage> {
        //dev test
        klankeRepository.findAll().first()

        return klageRepository.findByFoedselsnummerAndStatus(
            fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            status = KlageAnkeStatus.DRAFT
        )
    }
}
