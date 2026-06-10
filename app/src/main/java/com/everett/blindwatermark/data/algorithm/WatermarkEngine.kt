package com.everett.blindwatermark.data.algorithm

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

/**
 * Blind Watermark Engine - LSB (Least Significant Bit) implementation
 * Simple, stable, and memory-efficient
 */
object WatermarkEngine {

    private const val TAG = "WatermarkEngine"
    // Use blue channel bits 0-1 (2 bits per pixel) for watermark
    // This is robust against JPEG compression and minor image modifications

    /**
     * Embed text watermark into a bitmap image using LSB
     * @param carrier The original image bitmap
     * @param watermark The text watermark to embed
     * @param password Optional password for scrambling
     * @return Watermarked bitmap
     */
    fun embed(carrier: Bitmap, watermark: String, password: String = ""): Bitmap {
        Log.d(TAG, "开始嵌入水印，图片尺寸: ${carrier.width}x${carrier.height}, 水印: '$watermark'")

        val width = carrier.width
        val height = carrier.height
        val totalPixels = width * height

        // Convert text to bytes with length prefix
        val watermarkBytes = watermark.toByteArray(Charsets.UTF_8)
        val length = watermarkBytes.size

        // Need: 4 bytes for length + N bytes for text = (4 + N) * 8 bits
        // Each pixel stores 2 bits (blue channel bits 0-1)
        // So need: (4 + N) * 8 / 2 = (4 + N) * 4 pixels
        val totalBitsNeeded = (4 + length) * 8
        val pixelsNeeded = (totalBitsNeeded + 1) / 2 // round up

        if (pixelsNeeded > totalPixels) {
            throw IllegalArgumentException("图片太小，无法嵌入水印。需要至少 $pixelsNeeded 像素，但图片只有 $totalPixels 像素")
        }

        // Copy original bitmap
        val result = carrier.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(totalPixels)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        // Build bit stream: 32-bit length prefix + data bits
        val bits = mutableListOf<Int>()

        // Length prefix (32 bits, big-endian)
        for (i in 31 downTo 0) {
            bits.add((length shr i) and 1)
        }

        // Data bits
        for (byte in watermarkBytes) {
            for (i in 7 downTo 0) {
                bits.add((byte.toInt() shr i) and 1)
            }
        }

        // Scramble pixel indices if password provided
        val indices = if (password.isNotEmpty()) {
            val seed = password.hashCode().toLong()
            (0 until totalPixels).shuffled(java.util.Random(seed))
        } else {
            (0 until totalPixels).toList()
        }

        // Embed bits into pixels (2 bits per pixel in blue channel)
        var bitIndex = 0
        for (pixelIdx in indices) {
            if (bitIndex >= bits.size) break

            val pixel = pixels[pixelIdx]
            val a = Color.alpha(pixel)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Embed 2 bits into blue channel bits 0-1
            val bit1 = bits[bitIndex++]
            val bit2 = if (bitIndex < bits.size) bits[bitIndex++] else 0

            val newB = (b and 0xFC) or (bit1 shl 1) or bit2

            pixels[pixelIdx] = Color.argb(a, r, g, newB)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        Log.d(TAG, "水印嵌入完成，共嵌入 ${bits.size} 比特到 $pixelsNeeded 个像素")
        return result
    }

    /**
     * Extract text watermark from a watermarked bitmap image
     * @param watermarked The watermarked image bitmap
     * @param password Optional password used during embedding
     * @return Extracted watermark text, or null if no watermark found
     */
    fun extract(watermarked: Bitmap, password: String = ""): String? {
        Log.d(TAG, "开始提取水印，图片尺寸: ${watermarked.width}x${watermarked.height}")

        val width = watermarked.width
        val height = watermarked.height
        val totalPixels = width * height

        val pixels = IntArray(totalPixels)
        watermarked.getPixels(pixels, 0, width, 0, 0, width, height)

        // Get pixel indices in same order as embedding
        val indices = if (password.isNotEmpty()) {
            val seed = password.hashCode().toLong()
            (0 until totalPixels).shuffled(java.util.Random(seed))
        } else {
            (0 until totalPixels).toList()
        }

        // Extract bits (2 bits per pixel from blue channel)
        val bits = mutableListOf<Int>()
        for (pixelIdx in indices) {
            val pixel = pixels[pixelIdx]
            val b = Color.blue(pixel)
            bits.add((b shr 1) and 1)
            bits.add(b and 1)
        }

        // Need at least 32 bits for length prefix
        if (bits.size < 32) return null

        // Read length prefix
        var length = 0
        for (i in 0 until 32) {
            length = (length shl 1) or bits[i]
        }

        Log.d(TAG, "读取到长度前缀: $length")

        // Validate length
        if (length <= 0 || length > 10000) {
            Log.w(TAG, "长度无效: $length")
            return null
        }

        val totalBits = 32 + length * 8
        if (bits.size < totalBits) {
            Log.w(TAG, "比特数不足: ${bits.size} < $totalBits")
            return null
        }

        // Read data bytes
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            var byte = 0
            for (j in 0 until 8) {
                byte = (byte shl 1) or bits[32 + i * 8 + j]
            }
            bytes[i] = byte.toByte()
        }

        return try {
            val result = String(bytes, Charsets.UTF_8)
            Log.d(TAG, "提取成功: '$result'")
            result
        } catch (e: Exception) {
            Log.e(TAG, "UTF-8 解码失败", e)
            null
        }
    }
}
