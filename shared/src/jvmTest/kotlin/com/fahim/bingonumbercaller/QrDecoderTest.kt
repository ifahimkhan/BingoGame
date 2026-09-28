package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.QrCodeGenerator
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.test.Test
import kotlin.test.assertEquals

class QrDecoderTest {

    @Test
    fun testQrCodeGeneratorDecodingAcrossPayloads() {
        val testPayloads = listOf(
            "192.168.1.1:80",
            "192.168.1.100:8080",
            "bingolive://connect?host=10.0.0.1&port=8080",
            "bingolive://connect?host=192.168.1.100&port=8080&token=123456",
            "bingolive://connect?host=192.168.1.100&port=8080&token=123456&extra=somelongstringheretoversionup"
        )

        for (payload in testPayloads) {
            val matrix = QrCodeGenerator.generateQrMatrix(payload)
            val scale = 8
            val quietZone = 4
            val qzPx = quietZone * scale
            val width = (matrix[0].size + quietZone * 2) * scale
            val height = (matrix.size + quietZone * 2) * scale
            val pixels = IntArray(width * height) { 0xFFFFFFFF.toInt() }

            for (y in matrix.indices) {
                for (x in matrix[y].indices) {
                    if (matrix[y][x]) {
                        for (dy in 0 until scale) {
                            for (dx in 0 until scale) {
                                pixels[(qzPx + y * scale + dy) * width + (qzPx + x * scale + dx)] = 0xFF000000.toInt()
                            }
                        }
                    }
                }
            }
            val source = RGBLuminanceSource(width, height, pixels)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = QRCodeReader()
            val result = reader.decode(bitmap)
            assertEquals(payload, result.text)
        }
    }
}
