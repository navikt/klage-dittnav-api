package no.nav.klage.config.problem

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import no.nav.klage.domain.exception.AttachmentCouldNotBeConvertedException
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentFilenameTooLongException
import no.nav.klage.domain.exception.AttachmentGoneException
import no.nav.klage.domain.exception.AttachmentHasVirusException
import no.nav.klage.domain.exception.AttachmentIsEmptyException
import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.domain.exception.AttachmentNotFoundException
import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.domain.exception.AttachmentTotalTooLargeException
import no.nav.klage.domain.exception.InvalidFullmaktException
import no.nav.klage.domain.exception.InvalidIdentException
import no.nav.klage.domain.exception.KlankeIsDeletedException
import no.nav.klage.domain.exception.KlankeIsFinalizedException
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.exception.SectionedValidationErrorWithDetailsException
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)

        private val CLIENT_ERROR = AttributeKey.stringKey("request.client_error")
    }

    // Spring's built-in exceptions only get a generic title like "Bad Request", so the exception type is used instead.
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        setClientErrorOnSpan(statusCode = statusCode, title = ex.javaClass.simpleName)
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
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
    fun handleAttachmentTooLargeException(ex: AttachmentTooLargeException): ProblemDetail =
        create(httpStatus = HttpStatus.CONTENT_TOO_LARGE, ex = ex)

    @ExceptionHandler
    fun handleAttachmentGoneException(ex: AttachmentGoneException): ProblemDetail = create(httpStatus = HttpStatus.GONE, ex = ex)

    @ExceptionHandler
    fun handleAttachmentNotFoundException(ex: AttachmentNotFoundException): ProblemDetail =
        create(httpStatus = HttpStatus.NOT_FOUND, ex = ex)

    @ExceptionHandler
    fun handleAttachmentMissingFilenameException(ex: AttachmentMissingFilenameException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleAttachmentTotalTooLargeException(ex: AttachmentTotalTooLargeException): ProblemDetail =
        create(httpStatus = HttpStatus.CONTENT_TOO_LARGE, ex = ex)

    @ExceptionHandler
    fun handleAttachmentFilenameTooLongException(ex: AttachmentFilenameTooLongException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleInvalidFullmaktException(ex: InvalidFullmaktException): ProblemDetail = create(httpStatus = HttpStatus.FORBIDDEN, ex = ex)

    @ExceptionHandler
    fun handleInvalidIdent(ex: InvalidIdentException): ProblemDetail = create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex)

    @ExceptionHandler
    fun handleSectionedValidationErrorWithDetails(ex: SectionedValidationErrorWithDetailsException): ProblemDetail =
        create(httpStatus = HttpStatus.BAD_REQUEST, ex = ex, errorMessage = ex.title).apply {
            setProperty("sections", ex.sections)
        }

    private fun create(
        httpStatus: HttpStatus,
        ex: Exception,
        errorMessage: String = ex.message ?: "No error message available",
    ): ProblemDetail {
        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex,
        )
        setClientErrorOnSpan(statusCode = httpStatus, title = errorMessage)

        return ProblemDetail.forStatusAndDetail(httpStatus, errorMessage).apply {
            title = errorMessage
        }
    }

    // Handled exceptions are invisible to the OTel agent, so the reason for a 4xx is added to the server span explicitly.
    private fun setClientErrorOnSpan(
        statusCode: HttpStatusCode,
        title: String,
    ) {
        if (statusCode.is4xxClientError) {
            Span.current().setAttribute(CLIENT_ERROR, title)
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
