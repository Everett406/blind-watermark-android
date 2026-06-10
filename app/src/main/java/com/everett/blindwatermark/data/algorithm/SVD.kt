package com.everett.blindwatermark.data.algorithm

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.sign

/**
 * Singular Value Decomposition (SVD)
 * Decomposes matrix A into U * S * V^T
 *
 * Uses a simplified power iteration approach for small matrices (8x8 blocks)
 */
object SVD {

    data class SVDResult(
        val u: Array<DoubleArray>,
        val s: DoubleArray,
        val vt: Array<DoubleArray>
    )

    /**
     * Compute SVD for a small matrix using Jacobi eigenvalue algorithm
     * Suitable for 8x8 DCT blocks
     */
    fun decompose(a: Array<DoubleArray>): SVDResult {
        val m = a.size
        val n = a[0].size
        val minDim = minOf(m, n)

        // A^T * A for eigenvalue computation
        val ata = multiply(transpose(a), a)

        // Compute eigenvalues and eigenvectors of A^T * A using Jacobi
        val (eigenvalues, eigenvectors) = jacobiEigenvalue(ata)

        // Sort eigenvalues in descending order
        val sortedIndices = eigenvalues.indices.sortedByDescending { eigenvalues[it] }

        val s = DoubleArray(minDim) { i ->
            if (i < eigenvalues.size) sqrt(abs(eigenvalues[sortedIndices[i]])) else 0.0
        }

        // V matrix (eigenvectors)
        val v = Array(n) { i ->
            DoubleArray(n) { j ->
                eigenvectors[i][sortedIndices[j]]
            }
        }

        // U = A * V * S^-1
        val u = Array(m) { DoubleArray(minDim) }
        val av = multiply(a, v)
        for (i in 0 until m) {
            for (j in 0 until minDim) {
                u[i][j] = if (s[j] > 1e-10) av[i][j] / s[j] else 0.0
            }
        }

        val vt = transpose(v)

        return SVDResult(u, s, vt)
    }

    /**
     * Reconstruct matrix from SVD components
     */
    fun reconstruct(u: Array<DoubleArray>, s: DoubleArray, vt: Array<DoubleArray>): Array<DoubleArray> {
        val m = u.size
        val n = vt.size
        val us = Array(m) { DoubleArray(n) }

        for (i in 0 until m) {
            for (j in 0 until minOf(s.size, n)) {
                us[i][j] = u[i][j] * s[j]
            }
        }

        return multiply(us, vt)
    }

    /**
     * Jacobi eigenvalue algorithm for symmetric matrices
     */
    private fun jacobiEigenvalue(
        matrix: Array<DoubleArray>,
        maxIterations: Int = 100,
        epsilon: Double = 1e-10
    ): Pair<DoubleArray, Array<DoubleArray>> {
        val n = matrix.size
        val a = Array(n) { matrix[it].copyOf() }
        val v = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

        for (_iter in 0 until maxIterations) {
            // Find largest off-diagonal element
            var maxVal = 0.0
            var p = 0
            var q = 0
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    if (abs(a[i][j]) > maxVal) {
                        maxVal = abs(a[i][j])
                        p = i
                        q = j
                    }
                }
            }

            if (maxVal < epsilon) break

            // Compute rotation
            val diff = a[p][p] - a[q][q]
            val phi = if (abs(diff) < epsilon) {
                Math.PI / 4
            } else {
                0.5 * kotlin.math.atan2(2 * a[p][q], diff)
            }
            val c = cos(phi)
            val s_val = kotlin.math.sin(phi)

            // Update A
            val app = a[p][p]
            val aqq = a[q][q]
            val apq = a[p][q]

            a[p][p] = c * c * app - 2 * c * s_val * apq + s_val * s_val * aqq
            a[q][q] = s_val * s_val * app + 2 * c * s_val * apq + c * c * aqq
            a[p][q] = 0.0
            a[q][p] = 0.0

            for (i in 0 until n) {
                if (i != p && i != q) {
                    val aip = a[i][p]
                    val aiq = a[i][q]
                    a[i][p] = c * aip - s_val * aiq
                    a[p][i] = a[i][p]
                    a[i][q] = c * aiq + s_val * aip
                    a[q][i] = a[i][q]
                }
            }

            // Update V
            for (i in 0 until n) {
                val vip = v[i][p]
                val viq = v[i][q]
                v[i][p] = c * vip - s_val * viq
                v[i][q] = c * viq + s_val * vip
            }
        }

        val eigenvalues = DoubleArray(n) { a[it][it] }
        return Pair(eigenvalues, v)
    }

    private fun transpose(a: Array<DoubleArray>): Array<DoubleArray> {
        val m = a.size
        val n = a[0].size
        return Array(n) { i -> DoubleArray(m) { j -> a[j][i] } }
    }

    private fun multiply(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val m = a.size
        val n = b[0].size
        val p = b.size
        val result = Array(m) { DoubleArray(n) }
        for (i in 0 until m) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until p) {
                    sum += a[i][k] * b[k][j]
                }
                result[i][j] = sum
            }
        }
        return result
    }
}
