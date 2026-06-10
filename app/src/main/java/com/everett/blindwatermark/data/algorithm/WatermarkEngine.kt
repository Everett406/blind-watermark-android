package com.everett.blindwatermark.data.algorithm

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.roundToInt

/**
 * Blind Watermark Engine
 * DWT-DCT based blind watermarking (without SVD for simplicity and robustness)
 */
object WatermarkEngine {

    private const val BLOCK_SIZE = 8
    private const val ALPHA = 5.0 // Embedding strength for DCT coefficients

    /**
     * Embed text watermark into a bitmap image
     */
    fun embed(carrier: Bitmap, watermark: String, password: String = ""): Bitmap {
        Log.d("WatermarkEngine", "开始嵌入水印，图片尺寸: ${carrier.width}x${carrier.height}, 水印: '$watermark'")

        val watermarkBits = textToBits(watermark)
        Log.d("WatermarkEngine", "水印比特数: ${watermarkBits.size}")

        val width = carrier.width
        val height = carrier.height
        val carrierPixels = IntArray(width * height)
        carrier.getPixels(carrierPixels, 0, width, 0, 0, width, height)

        // Convert bitmap to Y channel
        val yChannel = Array(height) { i ->
            DoubleArray(width) { j ->
                val pixel = carrierPixels[i * width + j]
                0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            }
        }

        // Pad dimensions so that after DWT, LL sub-band is divisible by BLOCK_SIZE
        // DWT halves dimensions, so we need: padded / 2 % BLOCK_SIZE == 0
        // => padded % (2 * BLOCK_SIZE) == 0
        val targetMultiple = 2 * BLOCK_SIZE // 16
        val paddedWidth = ((width + targetMultiple - 1) / targetMultiple) * targetMultiple
        val paddedHeight = ((height + targetMultiple - 1) / targetMultiple) * targetMultiple
        val padded = Array(paddedHeight) { i ->
            DoubleArray(paddedWidth) { j ->
                if (i < height && j < width) yChannel[i][j] else 0.0
            }
        }
        Log.d("WatermarkEngine", "原尺寸: ${width}x$height, 补齐后: ${paddedWidth}x$paddedHeight")

        // 1-level Haar DWT
        val waveletResult = HaarWavelet.transform(padded)
        val ll = waveletResult.ll
        Log.d("WatermarkEngine", "LL子带尺寸: ${ll.size}x${ll[0].size}")

        // Embed watermark into LL sub-band
        val watermarkedLL = embedIntoLL(ll, watermarkBits, password)
        Log.d("WatermarkEngine", "水印嵌入完成")

        // Inverse DWT
        val inverseResult = HaarWavelet.inverse(
            HaarWavelet.WaveletResult(watermarkedLL, waveletResult.lh, waveletResult.hl, waveletResult.hh)
        )

        // Convert back to bitmap
        return yChannelToBitmap(inverseResult, carrierPixels, width, height)
    }

    /**
     * Extract text watermark from a watermarked bitmap image
     */
    fun extract(watermarked: Bitmap, password: String = ""): String? {
        Log.d("WatermarkEngine", "开始提取水印，图片尺寸: ${watermarked.width}x${watermarked.height}")

        val (yChannel, width, height) = bitmapToYChannel(watermarked)

        // Pad dimensions so that after DWT, LL sub-band is divisible by BLOCK_SIZE
        val targetMultiple = 2 * BLOCK_SIZE // 16
        val paddedWidth = ((width + targetMultiple - 1) / targetMultiple) * targetMultiple
        val paddedHeight = ((height + targetMultiple - 1) / targetMultiple) * targetMultiple
        val padded = Array(paddedHeight) { i ->
            DoubleArray(paddedWidth) { j ->
                if (i < height && j < width) yChannel[i][j] else 0.0
            }
        }
        Log.d("WatermarkEngine", "原尺寸: ${width}x$height, 补齐后: ${paddedWidth}x$paddedHeight")

        val waveletResult = HaarWavelet.transform(padded)
        val ll = waveletResult.ll

        val extractedBits = extractFromLL(ll, password)
        Log.d("WatermarkEngine", "提取到 ${extractedBits.size} 个比特")

        return bitsToText(extractedBits)
    }

    /**
     * Embed watermark bits into LL sub-band using DCT coefficient modification
     * We embed into the (1,1) DCT coefficient (low frequency) of each block
     */
    private fun embedIntoLL(
        ll: Array<DoubleArray>,
        watermarkBits: List<Int>,
        password: String
    ): Array<DoubleArray> {
        val height = ll.size
        val width = ll[0].size
        val result = Array(height) { i -> ll[i].copyOf() }

        val scrambledBits = if (password.isNotEmpty()) scrambleBits(watermarkBits, password) else watermarkBits

        val blocksH = height / BLOCK_SIZE
        val blocksW = width / BLOCK_SIZE
        val totalBlocks = blocksH * blocksW

        Log.d("WatermarkEngine", "嵌入: totalBlocks=$totalBlocks, bits=${scrambledBits.size}")

        var bitIndex = 0
        for (blockIdx in 0 until totalBlocks) {
            if (bitIndex >= scrambledBits.size) break

            val bh = blockIdx / blocksW
            val bw = blockIdx % blocksW

            try {
                // Extract block
                val block = Array(BLOCK_SIZE) { i ->
                    DoubleArray(BLOCK_SIZE) { j ->
                        result[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j]
                    }
                }

                // DCT
                val dctBlock = DCT.dct2(block)

                // Embed bit into DCT(1,1) coefficient (second low-freq component)
                // This is more robust than DC component (0,0)
                val bit = scrambledBits[bitIndex]
                val originalCoeff = dctBlock[1][1]

                // Quantization-based embedding:
                // For bit=1: round to nearest odd multiple of ALPHA
                // For bit=0: round to nearest even multiple of ALPHA
                val quantized = kotlin.math.round(originalCoeff / ALPHA).toInt()
                val newQuantized = if (bit == 1) {
                    if (quantized % 2 == 0) quantized + 1 else quantized
                } else {
                    if (quantized % 2 == 1) quantized + 1 else quantized
                }
                dctBlock[1][1] = newQuantized * ALPHA.toDouble()

                bitIndex++

                // Inverse DCT
                val idctBlock = DCT.idct2(dctBlock)

                // Put block back
                for (i in 0 until BLOCK_SIZE) {
                    for (j in 0 until BLOCK_SIZE) {
                        result[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j] = idctBlock[i][j]
                    }
                }
            } catch (e: Exception) {
                Log.e("WatermarkEngine", "处理块($bh,$bw)时出错: ${e.message}")
                bitIndex++
            }
        }

        Log.d("WatermarkEngine", "嵌入完成，处理了 $bitIndex / ${scrambledBits.size} 个比特")
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

        for (blockIdx in 0 until totalBlocks) {
            val bh = blockIdx / blocksW
            val bw = blockIdx % blocksW

            try {
                val block = Array(BLOCK_SIZE) { i ->
                    DoubleArray(BLOCK_SIZE) { j ->
                        ll[bh * BLOCK_SIZE + i][bw * BLOCK_SIZE + j]
                    }
                }

                val dctBlock = DCT.dct2(block)
                val coeff = dctBlock[1][1]

                // Extract bit from parity of quantized coefficient
                val quantized = kotlin.math.round(coeff / ALPHA).toInt()
                val bit = kotlin.math.abs(quantized) % 2

                extractedBits.add(bit)
            } catch (e: Exception) {
                Log.e("WatermarkEngine", "提取块($bh,$bw)时出错: ${e.message}")
            }
        }

        Log.d("WatermarkEngine", "提取完成，提取到 ${extractedBits.size} 个比特")

        return if (password.isNotEmpty()) descrambleBits(extractedBits, password) else extractedBits
    }

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

    private fun bitsToText(bits: List<Int>): String? {
        if (bits.size < 32) {
            Log.w("WatermarkEngine", "比特数不足32: ${bits.size}")
            return null
        }

        // Read length prefix
        var length = 0
        for (i in 0 until 32) {
            length = (length shl 1) or bits[i]
        }

        Log.d("WatermarkEngine", "读取到长度前缀: $length")

        if (length <= 0 || length > 10000) {
            Log.w("WatermarkEngine", "长度无效: $length")
            return null
        }
        if (bits.size < 32 + length * 8) {
            Log.w("WatermarkEngine", "比特数不足: ${bits.size} < ${32 + length * 8}")
            return null
        }

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
            Log.d("WatermarkEngine", "解码成功: '$result'")
            result
        } catch (e: Exception) {
            Log.e("WatermarkEngine", "UTF-8解码失败", e)
            null
        }
    }

    private fun scrambleBits(bits: List<Int>, password: String): List<Int> {
        val seed = password.hashCode().toLong()
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

    private fun bitmapToYChannel(bitmap: Bitmap): Triple<Array<DoubleArray>, Int, Int> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val yChannel = Array(height) { i ->
            DoubleArray(width) { j ->
                val pixel = pixels[i * width + j]
                0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            }
        }

        return Triple(yChannel, width, height)
    }

    private fun yChannelToBitmap(
        yChannel: Array<DoubleArray>,
        carrierPixels: IntArray,
        width: Int,
        height: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultPixels = IntArray(width * height)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val originalPixel = carrierPixels[i * width + j]
                val origR = Color.red(originalPixel)
                val origG = Color.green(originalPixel)
                val origB = Color.blue(originalPixel)
                val origA = Color.alpha(originalPixel)

                val origY = 0.299 * origR + 0.587 * origG + 0.114 * origB
                val newY = yChannel[i][j].coerceIn(0.0, 255.0)

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
