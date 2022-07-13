package no.nav.klage.config.problem

import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
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
    fun handleAnkeNotFound(
        ex: AnkeNotFoundException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.NOT_FOUND, ex, request)

    @ExceptionHandler
    fun handleAnkeIsDeleted(
        ex: AnkeIsDeletedException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.CONFLICT, ex, request)

    @ExceptionHandler
    fun handleAnkeIsFinalized(
        ex: AnkeIsFinalizedException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.CONFLICT, ex, request)

    @ExceptionHandler
    fun handleAvailableAnkeNotFound(
        ex: AvailableAnkeNotFoundException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> =
        create(Status.NOT_FOUND, ex, request)

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
        return create(Status.BAD_REQUEST, AttachmentTooLargeException(), request)
    }

    @ExceptionHandler
    fun handleAttachmentTotalTooLargeException(
        ex: AttachmentTotalTooLargeException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> {
        return create(Status.BAD_REQUEST, ex, request)
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

    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(
        ex: JwtTokenUnauthorizedException,
        request: NativeWebRequest
    ): ResponseEntity<Problem> {
        val newException = if (ex.message == null) {
            if (request.getHeader("Authorization") == null) {
                JwtTokenUnauthorizedException(msg = "No authorization header in request", cause = ex.cause)
            } else {
                JwtTokenUnauthorizedException(msg = ex.cause?.message, cause = ex.cause)
            }
        } else ex

        return create(Status.UNAUTHORIZED, newException, request)
    }
}