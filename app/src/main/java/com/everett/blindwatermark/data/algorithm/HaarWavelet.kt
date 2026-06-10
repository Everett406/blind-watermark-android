package com.everett.blindwatermark.data.algorithm

/**
 * Haar Wavelet Transform (1-level DWT)
 * Decomposes image into LL, LH, HL, HH sub-bands
 */
object HaarWavelet {

    /**
     * 1-level 2D Haar wavelet transform
     * Input: image matrix (must have even dimensions)
     * Output: [LL, LH, HL, HH] sub-bands
     */
    fun transform(image: Array<DoubleArray>): WaveletResult {
        val height = image.size
        val width = image[0].size
        val h2 = height / 2
        val w2 = width / 2

        val ll = Array(h2) { DoubleArray(w2) }
        val lh = Array(h2) { DoubleArray(w2) }
        val hl = Array(h2) { DoubleArray(w2) }
        val hh = Array(h2) { DoubleArray(w2) }

        for (i in 0 until h2) {
            for (j in 0 until w2) {
                val a = image[2 * i][2 * j]
                val b = image[2 * i][2 * j + 1]
                val c = image[2 * i + 1][2 * j]
                val d = image[2 * i + 1][2 * j + 1]

                ll[i][j] = (a + b + c + d) / 4.0
                lh[i][j] = (a + b - c - d) / 4.0
                hl[i][j] = (a - b + c - d) / 4.0
                hh[i][j] = (a - b - c + d) / 4.0
            }
        }

        return WaveletResult(ll, lh, hl, hh)
    }

    /**
     * Inverse 1-level 2D Haar wavelet transform
     */
    fun inverse(result: WaveletResult): Array<DoubleArray> {
        val h2 = result.ll.size
        val w2 = result.ll[0].size
        val height = h2 * 2
        val width = w2 * 2

        val image = Array(height) { DoubleArray(width) }

        for (i in 0 until h2) {
            for (j in 0 until w2) {
                val ll = result.ll[i][j]
                val lh = result.lh[i][j]
                val hl = result.hl[i][j]
                val hh = result.hh[i][j]

                image[2 * i][2 * j] = ll + lh + hl + hh
                image[2 * i][2 * j + 1] = ll + lh - hl - hh
                image[2 * i + 1][2 * j] = ll - lh + hl - hh
                image[2 * i + 1][2 * j + 1] = ll - lh - hl + hh
            }
        }

        return image
    }

    data class WaveletResult(
        val ll: Array<DoubleArray>,
        val lh: Array<DoubleArray>,
        val hl: Array<DoubleArray>,
        val hh: Array<DoubleArray>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as WaveletResult
            return ll.contentDeepEquals(other.ll) &&
                lh.contentDeepEquals(other.lh) &&
                hl.contentDeepEquals(other.hl) &&
                hh.contentDeepEquals(other.hh)
        }

        override fun hashCode(): Int {
            var result = ll.contentDeepHashCode()
            result = 31 * result + lh.contentDeepHashCode()
            result = 31 * result + hl.contentDeepHashCode()
            result = 31 * result + hh.contentDeepHashCode()
            return result
        }
    }
}
