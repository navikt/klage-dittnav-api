package no.nav.klage.config.problem

import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.spring.web.advice.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling

@ControllerAdvice
class ProblemHandlingControllerAdvice : OurOwnExceptionAdviceTrait, ProblemHandling

interface OurOwnExceptionAdviceTrait : AdviceTrait {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @ExceptionHandler
    fun handleKlageNotFound(
        ex: KlageNotFoundException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.NOT_FOUND, ex, request)

    @ExceptionHandler
    fun handleKlageIsDeleted(
        ex: KlageIsDeletedException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.CONFLICT, ex, request)

    @ExceptionHandler
    fun handleKlageIsFinalized(
        ex: KlageIsFinalizedException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.CONFLICT, ex, request)

    @ExceptionHandler
    fun handleAttemptedIllegalUpdate(
        ex: AttemptedIllegalUpdateException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.BAD_REQUEST, ex, request)

    @ExceptionHandler
    fun handleMaxUploadSizeException(
        ex: MaxUploadSizeExceededException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> {
        logger.warn("User tried to upload too large attachment", ex)
        return create(Status.BAD_REQUEST, AttachmentTooLargeException(), request)
    }

    @ExceptionHandler
    fun handleFullmaktNotFound(
        ex: FullmaktNotFoundException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.NOT_FOUND, ex, request)

    @ExceptionHandler
    fun handleUpdateMismatch(
        ex: UpdateMismatchException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.BAD_REQUEST, ex, request)
}

