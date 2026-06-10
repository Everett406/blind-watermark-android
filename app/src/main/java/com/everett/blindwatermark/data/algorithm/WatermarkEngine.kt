package com.everett.blindwatermark.data.algorithm

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

/**
 * Blind Watermark Engine
 * Implements DWT-DCT-SVD based blind watermarking algorithm
 */
object WatermarkEngine {

    private const val BLOCK_SIZE = 8
    private const val ALPHA = 0.1 // Embedding strength

    /**
     * Embed text watermark into a bitmap image
     * @param carrier The original image bitmap
     * @param watermark The text watermark to embed
     * @param password Optional password for scrambling
     * @return Watermarked bitmap
     */
    fun embed(carrier: Bitmap, watermark: String, password: String = ""): Bitmap {
        // Convert watermark text to binary sequence
        val watermarkBits = textToBits(watermark)

        // Convert bitmap to Y channel (luminance)
        val (yChannel, width, height) = bitmapToYChannel(carrier)

        // Ensure dimensions are even for wavelet transform
        val paddedWidth = if (width % 2 == 0) width else width + 1
        val paddedHeight = if (height % 2 == 0) height else height + 1
        val padded = Array(paddedHeight) { i ->
            DoubleArray(paddedWidth) { j ->
                if (i < height && j < width) yChannel[i][j] else 0.0
            }
        }

        // 1-level Haar DWT
        val waveletResult = HaarWavelet.transform(padded)
        val ll = waveletResult.ll

        // Embed watermark into LL sub-band using DCT-SVD
        val watermarkedLL = embedIntoLL(ll, watermarkBits, password)

        // Inverse wavelet transform
        val inverseResult = HaarWavelet.inverse(
            HaarWavelet.WaveletResult(
                watermarkedLL,
                waveletResult.lh,
                waveletResult.hl,
                waveletResult.hh
            )
        )

        // Convert back to bitmap
        return yChannelToBitmap(inverseResult, carrier, width, height)
    }

    /**
     * Extract text watermark from a watermarked bitmap image
     * @param watermarked The watermarked image bitmap
     * @param password Optional password used during embedding
     * @return Extracted watermark text, or null if no watermark found
     */
    fun extract(watermarked: Bitmap, password: String = ""): String? {
        val (yChannel, width, height) = bitmapToYChannel(watermarked)

        val paddedWidth = if (width % 2 == 0) width else width + 1
        val paddedHeight = if (height % 2 == 0) height else height + 1
        val padded = Array(paddedHeight) { i ->
            DoubleArray(paddedWidth) { j ->
                if (i < height && j < width) yChannel[i][j] else 0.0
            }
        }

        // 1-level Haar DWT
        val waveletResult = HaarWavelet.transform(padded)
        val ll = waveletResult.ll

        // Extract watermark bits from LL sub-band
        val extractedBits = extractFromLL(ll, password)

        return bitsToText(extractedBits)
    }

    /**
     * Embed watermark bits into LL sub-band using block-based DCT-SVD
     */
    private fun embedIntoLL(
        ll: Array<DoubleArray>,
        watermarkBits: List<Int>,
        password: String
    ): Array<DoubleArray> {
        val height = ll.size
        val width = ll[0].size
        val result = Array(height) { i -> ll[i].copyOf() }

        // Scramble watermark bits with password if provided
        val scrambledBits = if (password.isNotEmpty()) {
            scrambleBits(watermarkBits, password)
        } else {
            watermarkBits
        }

        val blocksH = height / BLOCK_SIZE
        val blocksW = width / BLOCK_SIZE
        val totalBlocks = blocksH * blocksW

        var bitIndex = 0
        for (blockIdx in 0 until minOf(totalBlocks, scrambledBits.size)) {
            val bh = blockIdx / blocksW
            val bw = blockIdx % blocksW

            if (bh >= blocksH || bw >= blocksW) break

            // Extract block
            val block = Array(BLOCK_SIZE) { i ->
                DoubleArray(BLOCK_SIZE) { j ->
                    result[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j]
                }
            }

            // DCT
            val dctBlock = DCT.dct2(block)

            // SVD on DCT coefficients
            val svd = SVD.decompose(dctBlock)

            // Modify largest singular value based on watermark bit
            if (bitIndex < scrambledBits.size) {
                val bit = scrambledBits[bitIndex]
                val modifiedS0 = if (bit == 1) {
                    svd.s[0] + ALPHA * svd.s[0]
                } else {
                    svd.s[0] - ALPHA * svd.s[0]
                }
                svd.s[0] = modifiedS0
                bitIndex++
            }

            // Reconstruct block
            val modifiedBlock = SVD.reconstruct(svd.u, svd.s, svd.vt)
            val idctBlock = DCT.idct2(modifiedBlock)

            // Put block back
            for (i in 0 until BLOCK_SIZE) {
                for (j in 0 until BLOCK_SIZE) {
                    result[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j] = idctBlock[i][j]
                }
            }
        }

        return result
    }

    /**
     * Extract watermark bits from LL sub-band
     */
    private fun extractFromLL(
        ll: Array<DoubleArray>,
        password: String
    ): List<Int> {
        val height = ll.size
        val width = ll[0].size

        val blocksH = height / BLOCK_SIZE
        val blocksW = width / BLOCK_SIZE
        val totalBlocks = blocksH * blocksW

        val extractedBits = mutableListOf<Int>()

        // We need a reference (original) to compare, but for blind watermarking
        // we use a different approach: we check the pattern of singular values
        // For simplicity, we use a threshold-based approach

        for (blockIdx in 0 until totalBlocks) {
            val bh = blockIdx / blocksW
            val bw = blockIdx % blocksW

            // Extract block
            val block = Array(BLOCK_SIZE) { i ->
                DoubleArray(BLOCK_SIZE) { j ->
                    ll[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j]
                }
            }

            // DCT
            val dctBlock = DCT.dct2(block)

            // SVD
            val svd = SVD.decompose(dctBlock)

            // Determine bit based on the pattern of singular values
            // If the largest singular value is relatively large, it's likely bit=1
            val s0 = svd.s[0]
            val s1 = if (svd.s.size > 1) svd.s[1] else 0.0

            // Use ratio as indicator
            val bit = if (s0 > s1 * (1 + ALPHA * 0.5)) 1 else 0
            extractedBits.add(bit)
        }

        // Descramble if password provided
        return if (password.isNotEmpty()) {
            descrambleBits(extractedBits, password)
        } else {
            extractedBits
        }
    }

    /**
     * Convert text to list of bits
     */
    private fun textToBits(text: String): List<Int> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val bits = mutableListOf<Int>()

        // Add length prefix (32 bits)
        val length = bytes.size
        for (i in 31 downTo 0) {
            bits.add((length shr i) and 1)
        }

        // Add data bits
        for (byte in bytes) {
            for (i in 7 downTo 0) {
                bits.add((byte.toInt() shr i) and 1)
            }
        }

        return bits
    }

    /**
     * Convert bits back to text
     */
    private fun bitsToText(bits: List<Int>): String? {
        if (bits.size < 32) return null

        // Read length prefix
        var length = 0
        for (i in 0 until 32) {
            length = (length shl 1) or bits[i]
        }

        if (length <= 0 || length > 10000) return null
        if (bits.size < 32 + length * 8) return null

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
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Simple bit scrambling using password hash as seed
     */
    private fun scrambleBits(bits: List<Int>, password: String): List<Int> {
        val seed = password.hashCode().toLong()
        val rng = LCG(seed)
        val indices = bits.indices.shuffled(java.util.Random(seed))
        return indices.map { bits[it] }
    }

    private fun descrambleBits(bits: List<Int>, password: String): List<Int> {
        val seed = password.hashCode().toLong()
        val indices = bits.indices.shuffled(java.util.Random(seed))
        val result = MutableList(bits.size) { 0 }
        for (i in bits.indices) {
            result[indices[i]] = bits[i]
        }
        return result
    }

    /**
     * Linear Congruential Generator for scrambling
     */
    private class LCG(seed: Long) {
        private var state = seed
        fun next(): Long {
            state = (state * 1103515245 + 12345) and 0x7fffffff
            return state
        }
    }

    /**
     * Extract Y (luminance) channel from ARGB bitmap
     */
    private fun bitmapToYChannel(bitmap: Bitmap): Triple<Array<DoubleArray>, Int, Int> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val yChannel = Array(height) { i ->
            DoubleArray(width) { j ->
                val pixel = pixels[i * width + j]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Y = 0.299R + 0.587G + 0.114B
                0.299 * r + 0.587 * g + 0.114 * b
            }
        }

        return Triple(yChannel, width, height)
    }

    /**
     * Convert Y channel back to bitmap, preserving original UV channels
     */
    private fun yChannelToBitmap(
        yChannel: Array<DoubleArray>,
        original: Bitmap,
        origWidth: Int,
        origHeight: Int
    ): Bitmap {
        val width = original.width
        val height = original.height
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultPixels = IntArray(width * height)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val originalPixel = pixels[i * width + j]
                val origR = Color.red(originalPixel)
                val origG = Color.green(originalPixel)
                val origB = Color.blue(originalPixel)
                val origA = Color.alpha(originalPixel)

                // Original Y
                val origY = 0.299 * origR + 0.587 * origG + 0.114 * origB

                // New Y from watermarked channel
                val newY = if (i < origHeight && j < origWidth) {
                    yChannel[i][j].coerceIn(0.0, 255.0)
                } else {
                    origY
                }

                // Calculate difference and distribute to RGB
                val diff = newY - origY
                val newR = (origR + diff).roundToInt().coerceIn(0, 255)
                val newG = (origG + diff).roundToInt().coerceIn(0, 255)
                val newB = (origB + diff).roundToInt().coerceIn(0, 255)

                resultPixels[i * width + j] = Color.argb(origA, newR, newG, newB)
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
}
