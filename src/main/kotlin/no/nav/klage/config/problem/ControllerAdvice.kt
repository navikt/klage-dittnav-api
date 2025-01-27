package no.nav.klage.config.problem

import no.nav.klage.domain.exception.*
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(
        ex: JwtTokenUnauthorizedException,
        request: NativeWebRequest
    ): ProblemDetail {
        val detail = if (ex.message == null) {
            if (request.getHeader("Authorization") == null) {
                secureLogger.debug("Returning warning: No authorization header in request")
                "No authorization header in request"
            } else {
                secureLogger.debug("Returning warning: ${ex.cause?.message}")
                ex.cause?.message ?: "No error message available"
            }
        } else ex.message ?: error("Message can't be null")

        secureLogger.error("Exception thrown to client: ${HttpStatus.UNAUTHORIZED.reasonPhrase}, $detail", ex)

        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail)
    }

    @ExceptionHandler
    fun handleKlankeNotFound(
        ex: KlankeNotFoundException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleKlankeIsDeleted(
        ex: KlankeIsDeletedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

    @ExceptionHandler
    fun handleKlankeIsFinalized(
        ex: KlankeIsFinalizedException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.CONFLICT, ex)

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
    fun handleAttachmentTotalTooLargeException(
        ex: AttachmentTotalTooLargeException,
        request: NativeWebRequest
    ): ProblemDetail =
        create(HttpStatus.PAYLOAD_TOO_LARGE, ex)

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

    private fun create(httpStatus: HttpStatus, ex: Exception): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"

        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex
        )

        return ProblemDetail.forStatusAndDetail(httpStatus, errorMessage).apply {
            title = errorMessage
        }
    }

    private fun logError(httpStatus: HttpStatus, errorMessage: String, exception: Exception) {
        when {
            httpStatus.is5xxServerError -> {
                secureLogger.error("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }

            else -> {
                secureLogger.warn("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }
        }
    }
}