package no.nav.klage.config.problem

import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(
        ex: JwtTokenUnauthorizedException,
        request: NativeWebRequest
    ): ProblemDetail {
        val detail = if (ex.message == null) {
            if (request.getHeader("Authorization") == null) {
                logger.debug("Returning warning: No authorization header in request")
                "No authorization header in request"
            } else {
                logger.debug("Returning warning: ${ex.cause?.message}")
                ex.cause?.message ?: "No error message available"
            }
        } else ex.message ?: error("Message can't be null")

        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail)
    }

    @ExceptionHandler
    fun handleKlageNotFound(
        ex: KlageNotFoundException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleKlageIsDeleted(
        ex: KlageIsDeletedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

    @ExceptionHandler
    fun handleKlageIsFinalized(
        ex: KlageIsFinalizedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

    @ExceptionHandler
    fun handleAnkeNotFound(
        ex: AnkeNotFoundException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleAnkeIsDeleted(
        ex: AnkeIsDeletedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

    @ExceptionHandler
    fun handleAnkeIsFinalized(
        ex: AnkeIsFinalizedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

    @ExceptionHandler
    fun handleAvailableAnkeNotFound(
        ex: AvailableAnkeNotFoundException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleAttemptedIllegalUpdate(
        ex: AttemptedIllegalUpdateException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleAttachmentCouldNotBeConvertedException(
        ex: AttachmentCouldNotBeConvertedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleAttachmentEncryptedException(
        ex: AttachmentEncryptedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleAttachmentHasVirusException(
        ex: AttachmentHasVirusException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleAttachmentIsEmptyException(
        ex: AttachmentIsEmptyException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleMaxUploadSizeException(
        ex: MaxUploadSizeExceededException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.PAYLOAD_TOO_LARGE, AttachmentTooLargeException())

    @ExceptionHandler
    fun handleAttachmentTotalTooLargeException(
        ex: AttachmentTotalTooLargeException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.PAYLOAD_TOO_LARGE, ex)

    @ExceptionHandler
    fun handleFullmaktNotFound(
        ex: FullmaktNotFoundException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleUpdateMismatch(
        ex: UpdateMismatchException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler
    fun handleInvalidIdent(
        ex: InvalidIdentException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    private fun create(status: HttpStatus, ex: Exception): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"
        return ProblemDetail.forStatusAndDetail(status, errorMessage).apply {
            title = errorMessage
        }
    }
}