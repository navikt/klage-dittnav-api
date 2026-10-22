package no.nav.klage.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.concurrent.TimeUnit

class KlankeRequestTimingFilterTest {
    private val filter = KlankeRequestTimingFilter()
    private val logger = LoggerFactory.getLogger(KlankeRequestTimingFilter::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private val basePath = "/api/klanker/123e4567-e89b-12d3-a456-426614174000"
    private val uploadPath = "$basePath/vedlegg"
    private val fritekstPath = "$basePath/fritekst"

    @BeforeEach
    fun attachAppender() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `logs upload phases with klanke id but without file contents`() {
        val request = MockHttpServletRequest("POST", uploadPath).apply { setContent(byteArrayOf(1, 2, 3)) }
        val response = MockHttpServletResponse()

        filter.doFilter(
            request,
            response,
            FilterChain { req, res ->
                req.setAttribute(TIMED_REQUEST_CONTROLLER_ENTRY_NANOS, System.nanoTime())
                (res as HttpServletResponse).status = 200
            },
        )

        val message = appender.list.single().formattedMessage
        assertTrue(message.contains("operation=attachment_upload"))
        assertTrue(message.contains("klankeId=123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(message.contains("contentLengthBytes=3"))
        assertTrue(message.contains("beforeControllerMs="))
        assertTrue(message.contains("afterControllerMs="))
        assertTrue(message.contains("totalMs="))
        assertTrue(message.contains("status=200"))
        assertFalse(message.contains("vedlegg.pdf"))
    }

    @Test
    fun `does not log fast fritekst requests even when they fail`() {
        val request = MockHttpServletRequest("PUT", fritekstPath).apply { setContent("""{"value":"private text"}""".toByteArray()) }
        val response = MockHttpServletResponse()

        filter.doFilter(
            request,
            response,
            FilterChain { req, res ->
                req.setAttribute(TIMED_REQUEST_CONTROLLER_ENTRY_NANOS, System.nanoTime())
                (res as HttpServletResponse).status = 503
            },
        )

        assertTrue(appender.list.isEmpty())
    }

    @Test
    fun `logs fritekst only above five seconds while keeping all uploads`() {
        val fiveSeconds = TimeUnit.SECONDS.toNanos(5)

        assertFalse(filter.shouldLogTiming("fritekst_update", fiveSeconds - 1))
        assertFalse(filter.shouldLogTiming("fritekst_update", fiveSeconds))
        assertTrue(filter.shouldLogTiming("fritekst_update", fiveSeconds + 1))
        assertTrue(filter.shouldLogTiming("attachment_upload", 0))
    }

    @Test
    fun `logs requests rejected before controller entry`() {
        val request = MockHttpServletRequest("POST", uploadPath)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, res -> (res as HttpServletResponse).status = 413 })

        val message = appender.list.single().formattedMessage
        assertTrue(message.contains("klankeId=123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(message.contains("beforeControllerMs=null"))
        assertTrue(message.contains("afterControllerMs=null"))
        assertTrue(message.contains("status=413"))
    }

    @Test
    fun `does not report success when request processing throws`() {
        val request = MockHttpServletRequest("POST", uploadPath)
        val response = MockHttpServletResponse()

        assertThrows<ServletException> {
            filter.doFilter(request, response, FilterChain { _, _ -> throw ServletException("processing failed") })
        }

        assertTrue(
            appender.list
                .single()
                .formattedMessage
                .contains("status=unhandled_exception"),
        )
    }

    @Test
    fun `does not log other endpoints or methods`() {
        val response = MockHttpServletResponse()
        var chainCalls = 0

        for (
        (method, path) in
        listOf(
            "GET" to uploadPath,
            "POST" to fritekstPath,
            "PUT" to uploadPath,
            "PUT" to "$fritekstPath/another",
            "POST" to "/api/klanker/123e4567-e89b-12d3-a456-426614174000%0Ainjected/vedlegg",
        )
        ) {
            filter.doFilter(MockHttpServletRequest(method, path), response, FilterChain { _, _ -> chainCalls++ })
        }

        assertEquals(5, chainCalls)
        assertTrue(appender.list.isEmpty())
    }
}
