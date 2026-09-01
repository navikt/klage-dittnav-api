package no.nav.klage.config.problem

import no.nav.klage.domain.exception.AttachmentCouldNotBeConvertedException
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentFilenameTooLongException
import no.nav.klage.domain.exception.AttachmentHasVirusException
import no.nav.klage.domain.exception.AttachmentIsEmptyException
import no.nav.klage.domain.exception.AttachmentTotalTooLargeException
import no.nav.klage.domain.exception.InvalidFullmaktException
import no.nav.klage.domain.exception.InvalidIdentException
import no.nav.klage.domain.exception.KlankeIsDeletedException
import no.nav.klage.domain.exception.KlankeIsFinalizedException
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.util.getLogger
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
    }

    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(
        ex: JwtTokenUnauthorizedException,
        request: NativeWebRequest,
    ): ProblemDetail {
        val detail =
            if (ex.message == null) {
                if (request.getHeader("Authorization") == null) {
                    "No authorization header in request"
                } else {
                    ex.cause?.message ?: "No error message available"
                }
            } else {
                ex.message ?: error("Message can't be null")
            }

        ourLogger.error("Exception thrown to client: ${HttpStatus.UNAUTHORIZED.reasonPhrase}, $detail", ex)

        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail)
    }

    @ExceptionHandler
    fun handleKlankeNotFound(ex: KlankeNotFoundException): ProblemDetail = create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    @ExceptionHandler
    fun handleKlankeIsDeleted(ex: KlankeIsDeletedException): ProblemDetail = create(httpStatus = HttpStatus.CONFLICT, ex = ex)

    @ExceptionHandler
    fun handleKlankeIsFinalized(ex: KlankeIsFinalizedException): ProblemDetail = create(httpStatus = HttpStatus.CONFLICT, ex = ex)

    @ExceptionHandler
    fun handleAttachmentCouldNotBeConvertedException(ex: AttachmentCouldNotBeConvertedException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentEncryptedException(ex: AttachmentEncryptedException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentHasVirusException(ex: AttachmentHasVirusException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentIsEmptyException(ex: AttachmentIsEmptyException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentTotalTooLargeException(ex: AttachmentTotalTooLargeException): ProblemDetail =
        create(httpStatus = HttpStatus.PAYLOAD_TOO_LARGE, ex = ex)

    @ExceptionHandler
    fun handleAttachmentFilenameTooLongException(ex: AttachmentFilenameTooLongException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleInvalidFullmaktException(ex: InvalidFullmaktException): ProblemDetail = create(httpStatus = HttpStatus.FORBIDDEN, ex = ex)

    @ExceptionHandler
    fun handleInvalidIdent(ex: InvalidIdentException): ProblemDetail = create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    private fun create(
        httpStatus: HttpStatus,
        ex: Exception,
    ): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"

        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex,
        )

        return ProblemDetail.forStatusAndDetail(httpStatus, errorMessage).apply {
            title = errorMessage
        }
    }

    private fun logError(
        httpStatus: HttpStatus,
        errorMessage: String,
        exception: Exception,
    ) {
        when {
            httpStatus.is5xxServerError -> {
                ourLogger.error("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }

            else -> {
                ourLogger.warn("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }
        }
    }
}
