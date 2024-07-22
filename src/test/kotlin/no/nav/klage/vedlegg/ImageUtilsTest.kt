package no.nav.klage.vedlegg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.and

internal class ImageUtilsTest {
    companion object {
        private const val TEST_RESOURCES_FOLDER = "src/test/resources/"
    }

    @Test
    fun imgSmallerThanA4RemainsUnchanged() {
        val orig = File(TEST_RESOURCES_FOLDER + "/pdf/jks.jpg")
        val scaled = ImageUtils.downToA4(orig, "jpg")
        val origImg = fromBytes(orig.readBytes())
        val scaledImg = fromBytes(scaled.readBytes())
        assertThat(origImg.width).isEqualTo(scaledImg.width)
        assertThat(origImg.height).isEqualTo(scaledImg.height)
    }

    @Test
    fun imgBiggerThanA4IsScaledDown() {
        val orig = File(TEST_RESOURCES_FOLDER + "/pdf/rdd.png")
        val scaled = ImageUtils.downToA4(orig, "jpg")
        val origImg = fromBytes(orig.readBytes())
        val scaledImg = fromBytes(scaled.readBytes())
        assertThat(scaledImg.width).isLessThan(origImg.width)
        assertThat(scaledImg.height).isLessThan(origImg.height)
    }

    @Test
    fun scaledImgHasRetainedFormat() {
        val orig = File(TEST_RESOURCES_FOLDER + "/pdf/rdd.png")
        val scaled = ImageUtils.downToA4(orig, "jpg")
        assertThat(hasJpgSignature(scaled.readBytes())).isTrue()
    }

    @Test
    fun rotateLandscapeToPortrait() {
        val orig = File(TEST_RESOURCES_FOLDER + "/pdf/landscape.jpg")
        val scaled = ImageUtils.downToA4(orig, "jpg")
        val origImg = fromBytes(orig.readBytes())
        val scaledImg = fromBytes(scaled.readBytes())
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