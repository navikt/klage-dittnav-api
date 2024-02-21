package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.*
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class CommonService(
    private val klankeRepository: KlankeRepository,
    private val validationService: ValidationService,
    private val kafkaInternalEventService: KafkaInternalEventService,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val fileClient: FileClient,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,

    ) {

    companion object {

        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createKlanke(input: KlankeFullInput, bruker: Bruker): Klanke {
        val klage = input.toKlanke(bruker = bruker)
        return klankeRepository.save(klage).also {
            updateMetrics(input = klage)
        }
    }

    fun createKlanke(input: KlankeMinimalInput, bruker: Bruker): Klanke {
        val klage = input.toKlanke(bruker = bruker)
        return klankeRepository.save(klage).also {
            updateMetrics(input = klage)
        }
    }

    fun KlankeFullInput.toKlanke(bruker: Bruker): Klanke {
        return Klanke(
            checkboxesSelected = checkboxesSelected?.toMutableList() ?: mutableListOf(),
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
            type = type!!,
        )
    }

    fun KlankeMinimalInput.toKlanke(bruker: Bruker): Klanke {
        return Klanke(
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
            enhetsnummer = null,
            type = type!!,
        )
    }

    private fun updateMetrics(input: Klanke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            input.innsendingsytelse.toTema().toString()
        }
        when (input.type) {
            Type.KLAGE -> klageAnkeMetrics.incrementKlagerInitialized(temaReport)
            Type.ANKE -> klageAnkeMetrics.incrementAnkerInitialized(temaReport)
            Type.KLAGE_ETTERSENDELSE -> {} //TODO
            Type.ANKE_ETTERSENDELSE -> {} //TODO
        }
    }

    fun getDraftOrCreateKlanke(input: KlankeMinimalInput, bruker: Bruker): Klanke {
        val existingKlage = getLatestKlankeDraft(
            bruker = bruker,
            tema = input.innsendingsytelse.toTema(),
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
            type = input.type!!,
        )

        return existingKlage ?: createKlanke(
            input = input,
            bruker = bruker,
        )
    }

    fun getLatestKlankeDraft(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
        type: Type,
    ): Klanke? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        return klankeRepository.findByFoedselsnummerAndStatusAndType(
            fnr = fnr,
            status = KlageAnkeStatus.DRAFT,
            type = type
        )
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }
    }

    fun updateCheckboxesSelected(
        klankeId: UUID,
        checkboxesSelected: Set<CheckboxEnum>?,
        bruker: Bruker
    ): LocalDateTime {
        val existingKlage = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlage)
        validationService.validateKlankeAccess(existingKlage, bruker)

        existingKlage.checkboxesSelected.clear()
        if (!checkboxesSelected.isNullOrEmpty()) {
            existingKlage.checkboxesSelected.addAll(checkboxesSelected)
        }
        existingKlage.modifiedByUser = LocalDateTime.now()

        return existingKlage.modifiedByUser
    }

    fun finalizeKlanke(klankeId: UUID, bruker: Bruker): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)

        if (existingKlanke.isFinalized()) {
            return existingKlanke.modifiedByUser
        }

        validationService.validateKlankeAccess(klanke = existingKlanke, bruker = bruker)
        validationService.validateKlanke(klanke = existingKlanke)

        existingKlanke.status = KlageAnkeStatus.DONE
        existingKlanke.modifiedByUser = LocalDateTime.now()

        kafkaProducer.sendToKafka(createAggregatedKlanke(bruker = bruker, klanke = existingKlanke))
        registerFinalizedMetrics(klanke = existingKlanke)

        logger.debug(
            "Klanke {} med tema {} er sendt inn.",
            klankeId,
            existingKlanke.tema.name,
        )

        return existingKlanke.modifiedByUser
    }

    private fun registerFinalizedMetrics(klanke: Klanke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(klanke.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            klanke.tema.toString()
        }

        when (klanke.type) {
            Type.KLAGE -> {
                klageAnkeMetrics.incrementKlagerFinalizedTitle(klanke.innsendingsytelse)
                klageAnkeMetrics.incrementKlagerFinalized(temaReport)
                klageAnkeMetrics.incrementKlagerGrunn(temaReport, klanke.checkboxesSelected)
            }
            Type.ANKE -> {
                klageAnkeMetrics.incrementAnkerFinalized(temaReport)
            }
            Type.KLAGE_ETTERSENDELSE -> {} //TODO
            Type.ANKE_ETTERSENDELSE -> {} //TODO
        }


        if (klanke.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (klanke.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }
        vedleggMetrics.registerNumberOfVedleggPerUser(klanke.vedlegg.size.toDouble())
    }

    fun getKlankePdf(klankeId: UUID, bruker: Bruker): ByteArray {
        val existingKlage = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlage, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlage, bruker = bruker)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageAnkeFile(existingKlage.journalpostId!!)
    }

    fun createKlankePdfWithFoersteside(klankeId: UUID, bruker: Bruker): ByteArray? {
        val existingKlage = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlage, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlage, bruker = bruker)

        validationService.validateKlanke(klanke = existingKlage)

        klageDittnavPdfgenService.createKlankePdfWithFoersteside(
            createPdfWithFoerstesideInput(klanke = existingKlage, bruker)
        ).also {
            setPdfDownloadedWithoutAccessValidation(
                klankeId = klankeId,
                pdfDownloaded = LocalDateTime.now()
            )
            return it
        }
    }

    private fun createAggregatedKlanke(
        bruker: Bruker,
        klanke: Klanke
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klanke.vedtakDate)

        return AggregatedKlageAnke(
            id = klanke.id.toString(),
            fornavn = bruker.navn.fornavn,
            mellomnavn = bruker.navn.mellomnavn ?: "",
            etternavn = bruker.navn.etternavn,
            vedtak = vedtak ?: "",
            dato = klanke.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(klanke.fritekst!!),
            identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = klanke.tema.name,
            ytelse = klanke.innsendingsytelse.nb,
            vedlegg = klanke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userChoices = klanke.checkboxesSelected.map { x -> x.getFullText(klanke.language) },
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.internalSaksnummer,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.valueOf(klanke.type.name),
            enhetsnummer = klanke.enhetsnummer,
        )
    }

    fun createPdfWithFoerstesideInput(klanke: Klanke, bruker: Bruker): OpenKlankeInput {
        return OpenKlankeInput(
            foedselsnummer = klanke.foedselsnummer,
            navn = bruker.navn,
            fritekst = klanke.fritekst!!,
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.internalSaksnummer,
            vedtakDate = klanke.vedtakDate,
            innsendingsytelse = klanke.innsendingsytelse,
            checkboxesSelected = klanke.checkboxesSelected.toSet(),
            language = klanke.language,
            hasVedlegg = klanke.vedlegg.isNotEmpty() || klanke.hasVedlegg,
            enhetsnummer = klanke.enhetsnummer,
            type = klanke.type,
        )
    }

    fun getKlanke(klankeId: UUID, bruker: Bruker): Klanke {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke, bruker = bruker)
        return klanke
    }

    fun validateAccess(klankeId: UUID, bruker: Bruker) {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = klanke, bruker = bruker)
    }

    fun getJournalpostId(klankeId: UUID, bruker: Bruker): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke, false)
        validationService.validateKlankeAccess(klanke, bruker)
        return klanke.journalpostId
    }

    fun updateFritekst(klankeId: UUID, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.fritekst = fritekst
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateUserSaksnummer(klankeId: UUID, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.userSaksnummer = userSaksnummer
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateVedtakDate(klankeId: UUID, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.vedtakDate = vedtakDate
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateEnhetsnummer(
        ankeId: UUID,
        enhetsnummer: String?,
        bruker: Bruker
    ): LocalDateTime {
        val existingAnke = klankeRepository.findById(ankeId).get()
        validationService.checkKlankeStatus(existingAnke)
        validationService.validateKlankeAccess(existingAnke, bruker)

        existingAnke.enhetsnummer = enhetsnummer
        existingAnke.modifiedByUser = LocalDateTime.now()

        return existingAnke.modifiedByUser
    }

    fun updateJournalpostId(klankeId: UUID, journalpostId: String, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.journalpostId = journalpostId
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.journalpostId = journalpostId
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    private fun getAndValidateAccess(klankeId: UUID, bruker: Bruker): Klanke {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, bruker = bruker)
        return existingKlanke
    }

    fun updateHasVedlegg(klankeId: UUID, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.hasVedlegg = hasVedlegg
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateStatusWithoutValidation(klankeId: UUID, status: KlageAnkeStatus): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.status = status
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun deleteKlanke(klankeId: UUID, bruker: Bruker) {
        val existingKlanke = getAndValidateAccess(klankeId, bruker)

        existingKlanke.status = KlageAnkeStatus.DELETED
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

    fun getJournalpostIdWithoutValidation(klankeId: UUID): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        return klanke.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String) {
        updateJournalpostIdWithoutValidation(klankeId, journalpostId)
        kafkaInternalEventService.publishEvent(
            Event(
                klageAnkeId = klankeId.toString(),
                name = "journalpostId",
                id = klankeId.toString(),
                data = journalpostId,
            )
        )
    }

    fun setPdfDownloadedWithoutAccessValidation(klankeId: UUID, pdfDownloaded: LocalDateTime?) {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)

        existingKlanke.pdfDownloaded = pdfDownloaded
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

}
