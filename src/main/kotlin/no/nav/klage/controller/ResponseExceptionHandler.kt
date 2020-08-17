package no.nav.klage.controller

import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.util.getLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class ResponseExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = getLogger(javaClass.enclosingClass)
    }

    @ExceptionHandler(value = [MaxUploadSizeExceededException::class])
    protected fun handleMaxUploadSizeException(
        ex: RuntimeException, request: WebRequest
    ): ResponseEntity<Any?>? {
        log.warn("User tried to upload too large attachment", ex)
        return ResponseEntity.badRequest().body(AttachmentTooLargeException())
    }
}