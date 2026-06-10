package com.everett.blindwatermark.data.algorithm

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 2D Discrete Cosine Transform (DCT) and IDCT
 * Used for transforming image blocks to frequency domain
 */
object DCT {

    /**
     * Forward 2D DCT on an NxN block
     */
    fun dct2(block: Array<DoubleArray>): Array<DoubleArray> {
        val n = block.size
        val result = Array(n) { DoubleArray(n) }

        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        sum += block[i][j] *
                            cos((2 * i + 1) * u * Math.PI / (2 * n)) *
                            cos((2 * j + 1) * v * Math.PI / (2 * n))
                    }
                }
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                result[u][v] = (2.0 / n) * cu * cv * sum
            }
        }
        return result
    }

    /**
     * Inverse 2D DCT on an NxN block
     */
    fun idct2(block: Array<DoubleArray>): Array<DoubleArray> {
        val n = block.size
        val result = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (u in 0 until n) {
                    for (v in 0 until n) {
                        val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                        val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                        sum += cu * cv * block[u][v] *
                            cos((2 * i + 1) * u * Math.PI / (2 * n)) *
                            cos((2 * j + 1) * v * Math.PI / (2 * n))
                    }
                }
                result[i][j] = (2.0 / n) * sum
            }
        }
        return result
    }
}
