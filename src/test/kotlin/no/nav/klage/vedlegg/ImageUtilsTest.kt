package no.nav.klage.vedlegg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.experimental.and

internal class ImageUtilsTest {

    @Test
    fun imgSmallerThanA4RemainsUnchanged() {
        val orig = this::class.java.getResource("/pdf/jks.jpg").readBytes()
        val scaled: ByteArray = ImageUtils.downToA4(orig, "jpg")
        assertThat(scaled.size).isEqualTo(orig.size)
    }

    @Test
    fun imgBiggerThanA4IsScaledDown() {
        val orig = javaClass.getResource("/pdf/rdd.png").readBytes()
        val scaled: ByteArray = ImageUtils.downToA4(orig, "jpg")
        val origImg = fromBytes(orig)
        val scaledImg = fromBytes(scaled)
        assertThat(scaledImg.width).isLessThan(origImg.width)
        assertThat(scaledImg.height).isLessThan(origImg.height)
    }

    @Test
    fun scaledImgHasRetainedFormat() {
        val orig = javaClass.getResource("/pdf/rdd.png").readBytes()
        val scaled: ByteArray = ImageUtils.downToA4(orig, "jpg")
        assertThat(hasJpgSignature(scaled)).isTrue()
    }

    @Test
    fun rotateLandscapeToPortrait() {
        val orig = javaClass.getResource("/pdf/landscape.jpg").readBytes()
        val scaled: ByteArray = ImageUtils.downToA4(orig, "jpg")
        val origImg = fromBytes(orig)
        val scaledImg = fromBytes(scaled)
        assertThat(origImg.width).isGreaterThan(origImg.height)
        assertThat(scaledImg.height).isGreaterThan(scaledImg.width)
    }

    fun hasJpgSignature(bytes: ByteArray): Boolean {
        return (bytes[0] and 0XFF.toByte()) == 0xFF.toByte() &&
                (bytes[1] and 0XFF.toByte()) == 0xD8.toByte() &&
                (bytes[0] and 0XFF.toByte()) == 0xFF.toByte()
    }

    private fun fromBytes(bytes: ByteArray): BufferedImage {
        try {
            ByteArrayInputStream(bytes).use { inputStream -> return ImageIO.read(inputStream) }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }
}