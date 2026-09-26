package com.fahim.bingonumbercaller.domain

/**
 * Pure Kotlin QR Code Matrix Generator (QR Code Model 2, Byte mode, ECL M).
 * Generates a List<List<Boolean>> grid where true = dark module, false = light module.
 * Zero external or platform dependencies.
 */
object QrCodeGenerator {

    fun generateQrMatrix(payload: String): List<List<Boolean>> {
        val bytes = payload.encodeToByteArray()
        val version = selectVersion(bytes.size)
        val dataCodewords = encodeData(bytes, version)
        val allCodewords = generateCodewords(dataCodewords, version)
        return buildMatrix(allCodewords, version)
    }

    internal data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val dataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsPerBlockGroup1: Int,
        val numBlocksGroup2: Int,
        val dataCodewordsPerBlockGroup2: Int,
        val alignmentPatterns: List<Int>
    )

    // ECL 'M' specifications for versions 1 to 6
    internal val VERSIONS = listOf(
        VersionSpec(1, 26, 16, 10, 1, 16, 0, 0, emptyList()),
        VersionSpec(2, 44, 28, 16, 1, 28, 0, 0, listOf(6, 18)),
        VersionSpec(3, 70, 44, 26, 1, 44, 0, 0, listOf(6, 22)),
        VersionSpec(4, 100, 64, 18, 2, 32, 0, 0, listOf(6, 26)),
        VersionSpec(5, 134, 86, 24, 2, 43, 0, 0, listOf(6, 30)),
        VersionSpec(6, 172, 108, 16, 4, 27, 0, 0, listOf(6, 34))
    )

    internal fun selectVersion(byteCount: Int): VersionSpec {
        for (v in VERSIONS) {
            // Byte mode overhead: 4 bits mode + 8 bits length = 12 bits -> 2 bytes overhead
            if (byteCount + 2 <= v.dataCodewords) {
                return v
            }
        }
        return VERSIONS.last()
    }

    internal fun encodeData(data: ByteArray, version: VersionSpec): ByteArray {
        val bitBuffer = mutableListOf<Boolean>()

        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bitBuffer.add(((value ushr i) and 1) == 1)
            }
        }

        // Byte mode indicator: 0100
        appendBits(4, 4)

        // Character count indicator (8 bits for versions 1-9 in Byte mode)
        appendBits(data.size, 8)

        // Data bytes
        for (b in data) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        // Terminator: up to 4 zero bits
        val maxDataBits = version.dataCodewords * 8
        val terminatorLength = minOf(4, maxDataBits - bitBuffer.size)
        appendBits(0, terminatorLength)

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(false)
        }

        // Convert to bytes
        val result = ByteArray(version.dataCodewords)
        var byteIndex = 0
        for (i in bitBuffer.indices step 8) {
            var b = 0
            for (j in 0 until 8) {
                if (i + j < bitBuffer.size && bitBuffer[i + j]) {
                    b = b or (1 shl (7 - j))
                }
            }
            result[byteIndex++] = b.toByte()
        }

        // Fill remaining bytes with alternating pad bytes 0xEC and 0x11
        var pad = 0xEC
        while (byteIndex < version.dataCodewords) {
            result[byteIndex++] = pad.toByte()
            pad = if (pad == 0xEC) 0x11 else 0xEC
        }

        return result
    }

    // Galois Field GF(256) arithmetic for Reed-Solomon error correction
    private val exp = IntArray(512)
    private val log = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            exp[i] = exp[i - 255]
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return exp[log[x] + log[y]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var g = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, exp[i])
            val result = IntArray(g.size + 1)
            for (j in g.indices) {
                result[j] = result[j] xor g[j]
                result[j + 1] = result[j + 1] xor gfMul(g[j], factor[1])
            }
            g = result
        }
        return g
    }

    internal fun calculateEC(data: ByteArray, ecCount: Int): ByteArray {
        val gen = rsGeneratorPoly(ecCount)
        val remainder = IntArray(ecCount)

        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor remainder[0]
            for (i in 0 until ecCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMul(gen[i + 1], factor)
            }
            remainder[ecCount - 1] = gfMul(gen[ecCount], factor)
        }

        return ByteArray(ecCount) { remainder[it].toByte() }
    }

    internal fun generateCodewords(data: ByteArray, version: VersionSpec): ByteArray {
        val numBlocks = version.numBlocksGroup1 + version.numBlocksGroup2
        val dataBlocks = Array(numBlocks) { ByteArray(0) }
        val ecBlocks = Array(numBlocks) { ByteArray(0) }

        var offset = 0
        var blockIdx = 0
        for (i in 0 until version.numBlocksGroup1) {
            val len = version.dataCodewordsPerBlockGroup1
            val slice = data.copyOfRange(offset, offset + len)
            dataBlocks[blockIdx] = slice
            ecBlocks[blockIdx] = calculateEC(slice, version.ecCodewordsPerBlock)
            offset += len
            blockIdx++
        }
        for (i in 0 until version.numBlocksGroup2) {
            val len = version.dataCodewordsPerBlockGroup2
            val slice = data.copyOfRange(offset, offset + len)
            dataBlocks[blockIdx] = slice
            ecBlocks[blockIdx] = calculateEC(slice, version.ecCodewordsPerBlock)
            offset += len
            blockIdx++
        }

        // Interleave data codewords
        val result = ByteArray(version.totalCodewords)
        var outIdx = 0
        val maxDataLen = maxOf(version.dataCodewordsPerBlockGroup1, version.dataCodewordsPerBlockGroup2)
        for (c in 0 until maxDataLen) {
            for (b in 0 until numBlocks) {
                if (c < dataBlocks[b].size) {
                    result[outIdx++] = dataBlocks[b][c]
                }
            }
        }
        // Interleave EC codewords
        for (c in 0 until version.ecCodewordsPerBlock) {
            for (b in 0 until numBlocks) {
                result[outIdx++] = ecBlocks[b][c]
            }
        }

        return result
    }

    private fun buildMatrix(codewords: ByteArray, version: VersionSpec): List<List<Boolean>> {
        val size = 4 * version.version + 17
        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setModule(row: Int, col: Int, value: Boolean, function: Boolean = false) {
            if (row in 0 until size && col in 0 until size) {
                matrix[row][col] = value
                if (function) isFunction[row][col] = true
            }
        }

        // Finder patterns (7x7) + Separators
        fun placeFinder(r: Int, c: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val row = r + dr
                    val col = c + dc
                    if (row in 0 until size && col in 0 until size) {
                        val isBlack = (dr in 0..6 && dc in 0..6) && (
                            dr == 0 || dr == 6 || dc == 0 || dc == 6 ||
                            (dr in 2..4 && dc in 2..4)
                        )
                        setModule(row, col, isBlack, true)
                    }
                }
            }
        }
        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // Alignment patterns for version >= 2
        val alignCoords = version.alignmentPatterns
        for (r in alignCoords) {
            for (c in alignCoords) {
                if ((r <= 8 && c <= 8) || (r <= 8 && c >= size - 8) || (r >= size - 8 && c <= 8)) {
                    continue
                }
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isBlack = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
                        setModule(r + dr, c + dc, isBlack, true)
                    }
                }
            }
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            val v = (i % 2 == 0)
            if (!isFunction[6][i]) setModule(6, i, v, true)
            if (!isFunction[i][6]) setModule(i, 6, v, true)
        }

        // Dark module: always black, row = size - 8, col = 8
        setModule(size - 8, 8, true, true)

        // Format info area reservation
        for (i in 0..8) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        for (i in 0 until 8) {
            isFunction[8][size - 1 - i] = true
        }
        for (i in 0 until 7) {
            isFunction[size - 1 - i][8] = true
        }

        // Place Data Codewords (Zig-Zag)
        var bitIdx = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        var goingUp = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            val rows = if (goingUp) (size - 1 downTo 0) else (0 until size)

            for (row in rows) {
                for (d in 0..1) {
                    val col = right - d
                    if (!isFunction[row][col]) {
                        var bit = false
                        if (bitIdx < totalBits) {
                            val byteVal = codewords[bitIdx / 8].toInt() and 0xFF
                            val bitPos = 7 - (bitIdx % 8)
                            bit = ((byteVal ushr bitPos) and 1) == 1
                            bitIdx++
                        }
                        // Apply Mask 0: (row + col) % 2 == 0
                        val maskBit = (row + col) % 2 == 0
                        matrix[row][col] = bit xor maskBit
                    }
                }
            }
            right -= 2
            goingUp = !goingUp
        }

        // Format Info: ECL M (00) + Mask 0 (000) = 00000 -> with BCH + Mask 0x5412 = 0x5412
        val formatBits = listOf(true, false, true, false, true, false, false, false, false, false, true, false, false, true, false)

        // Top-left format bits
        for (i in 0..5) setModule(8, i, formatBits[i], true)
        setModule(8, 7, formatBits[6], true)
        setModule(8, 8, formatBits[7], true)
        setModule(7, 8, formatBits[8], true)
        for (i in 9..14) setModule(14 - i, 8, formatBits[i], true)

        // Bottom-left: copy of bits 0..6
        for (i in 0..6) setModule(size - 1 - i, 8, formatBits[i], true)

        // Top-right: copy of bits 7..14 (bit 14 at size-1 down to bit 7 at size-8)
        for (i in 0..7) setModule(8, size - 1 - i, formatBits[14 - i], true)

        return matrix.map { it.toList() }
    }
}

fun generateQrMatrix(payload: String): List<List<Boolean>> =
    QrCodeGenerator.generateQrMatrix(payload)
