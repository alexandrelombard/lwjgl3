/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

import org.lwjgl.*
import org.lwjgl.system.nio.*

/**
 * A class to check buffer boundaries in general. If there is insufficient space in the buffer when the call is made then a buffer overflow would otherwise
 * occur and cause unexpected behaviour, a crash, or worse, a security risk.
 *
 *
 * Internal class, don't use.
 *
 * @see Configuration.DISABLE_CHECKS
 *
 * @see Configuration.DEBUG
 *
 * @see Configuration.DEBUG_FUNCTIONS
 */
object KChecks {
    /**
     * Runtime checks flag.
     *
     *
     * When enabled, LWJGL will perform basic checks during its operation, mainly to avoid crashes in native code. Examples of such checks are:
     * context-specific function address validation, buffer capacity checks, null-termination checks, etc. These checks are generally low-overhead and should
     * not have a measurable effect on performance, so its recommended to have them enabled both during development and in production releases.
     *
     *
     * If maximum performance is required, they can be disabled by setting [Configuration.DISABLE_CHECKS] to true.
     */
    val CHECKS = !Configuration.DISABLE_CHECKS[false]

    /**
     * Debug mode flag.
     *
     *
     * When enabled, LWJGL will perform additional checks during its operation. These checks are more expensive than the ones enabled with [.CHECKS]
     * and will have a noticeable effect on performance, so they are disabled by default. Examples of such checks are: buffer object binding state check (GL),
     * buffer capacity checks for texture images (GL &amp; CL), etc. LWJGL will also print additional information, mainly during start-up.
     *
     *
     * Can be enabled by setting [Configuration.DEBUG] to true.
     */
    val DEBUG = Configuration.DEBUG[false]

    /**
     * Debug functions flag.
     *
     *
     * When enabled, capabilities classes will print an error message when they fail to retrieve a function pointer.
     *
     *
     * Can be enabled by setting [Configuration.DEBUG_FUNCTIONS] to true.
     */
    val DEBUG_FUNCTIONS = Configuration.DEBUG_FUNCTIONS[false]
    fun lengthSafe(array: ShortArray?): Int {
        return array?.size ?: 0
    }

    fun lengthSafe(array: IntArray?): Int {
        return array?.size ?: 0
    }

    fun lengthSafe(array: LongArray?): Int {
        return array?.size ?: 0
    }

    fun lengthSafe(array: FloatArray?): Int {
        return array?.size ?: 0
    }

    fun lengthSafe(array: DoubleArray?): Int {
        return array?.size ?: 0
    }

    fun remainingSafe(buffer: Buffer?): Int {
        return buffer?.remaining() ?: 0
    }

    fun remainingSafe(buffer: CustomBuffer<*>?): Int {
        return buffer?.remaining() ?: 0
    }

    /**
     * Checks if any of the specified functions pointers is `NULL`.
     *
     * @param functions the function pointers to check
     *
     * @return true if all function pointers are valid, false otherwise.
     */
    fun checkFunctions(vararg functions: Long): Boolean {
        for (pointer in functions) {
            if (pointer == MemoryUtil.NULL) {
                return false
            }
        }
        return true
    }

    /**
     * Ensures that the specified pointer is not `NULL` (0L).
     *
     * @param pointer the pointer to check
     *
     * @throws NullPointerException if `pointer` is `NULL`
     */
    fun check(pointer: Long): Long {
        if (pointer == MemoryUtil.NULL) {
            throw NullPointerException()
        }
        return pointer
    }

    private fun assertNT(found: Boolean) {
        require(found) { "Missing termination" }
    }

    /** Ensures that the specified array is null-terminated.  */
    fun checkNT(buf: IntArray) {
        checkBuffer(buf.size, 1)
        assertNT(buf[buf.size - 1] == 0)
    }

    /** Ensures that the specified array is terminated with the specified terminator.  */
    fun checkNT(buf: IntArray, terminator: Int) {
        checkBuffer(buf.size, 1)
        assertNT(buf[buf.size - 1] == terminator)
    }

    /** Ensures that the specified array is null-terminated.  */
    fun checkNT(buf: LongArray) {
        checkBuffer(buf.size, 1)
        assertNT(buf[buf.size - 1] == MemoryUtil.NULL)
    }

    /** Ensures that the specified array is null-terminated.  */
    fun checkNT(buf: FloatArray) {
        checkBuffer(buf.size, 1)
        assertNT(buf[buf.size - 1] == 0.0f)
    }

    /** Ensures that the specified ByteBuffer is null-terminated (last byte equal to 0).  */
    fun checkNT1(buf: KByteBuffer) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1].toInt() == 0)
    }

    /** Ensures that the specified ByteBuffer is null-terminated (last 2 bytes equal to 0).  */
    fun checkNT2(buf: KByteBuffer) {
        checkBuffer(buf.remaining(), 2)
        assertNT(buf[buf.limit() - 2].toInt() == 0)
    }

    /** Ensures that the specified IntBuffer is null-terminated.  */
    fun checkNT(buf: IntBuffer) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == 0)
    }

    /** Ensures that the specified IntBuffer is terminated with the specified terminator.  */
    fun checkNT(buf: IntBuffer, terminator: Int) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == terminator)
    }

    /** Ensures that the specified LongBuffer is null-terminated.  */
    fun checkNT(buf: LongBuffer) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == MemoryUtil.NULL)
    }

    /** Ensures that the specified FloatBuffer is null-terminated.  */
    fun checkNT(buf: FloatBuffer) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == 0.0f)
    }

    /** Ensures that the specified PointerBuffer is null-terminated.  */
    fun checkNT(buf: PointerBuffer) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == MemoryUtil.NULL)
    }

    /** Ensures that the specified PointerBuffer is terminated with the specified terminator.  */
    fun checkNT(buf: PointerBuffer, terminator: Long) {
        checkBuffer(buf.remaining(), 1)
        assertNT(buf[buf.limit() - 1] == terminator)
    }

    fun checkNTSafe(buf: IntArray?) {
        if (buf != null) {
            checkBuffer(buf.size, 1)
            assertNT(buf[buf.size - 1] == 0)
        }
    }

    fun checkNTSafe(buf: IntArray?, terminator: Int) {
        if (buf != null) {
            checkBuffer(buf.size, 1)
            assertNT(buf[buf.size - 1] == terminator)
        }
    }

    fun checkNTSafe(buf: LongArray?) {
        if (buf != null) {
            checkBuffer(buf.size, 1)
            assertNT(buf[buf.size - 1] == MemoryUtil.NULL)
        }
    }

    fun checkNTSafe(buf: FloatArray?) {
        if (buf != null) {
            checkBuffer(buf.size, 1)
            assertNT(buf[buf.size - 1] == 0.0f)
        }
    }

    fun checkNT1Safe(buf: ByteBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1].toInt() == 0)
        }
    }

    fun checkNT2Safe(buf: ByteBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 2)
            assertNT(buf[buf.limit() - 2].toInt() == 0)
        }
    }

    fun checkNTSafe(buf: IntBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == 0)
        }
    }

    fun checkNTSafe(buf: IntBuffer?, terminator: Int) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == terminator)
        }
    }

    fun checkNTSafe(buf: LongBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == MemoryUtil.NULL)
        }
    }

    fun checkNTSafe(buf: FloatBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == 0.0f)
        }
    }

    fun checkNTSafe(buf: PointerBuffer?) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == MemoryUtil.NULL)
        }
    }

    fun checkNTSafe(buf: PointerBuffer?, terminator: Long) {
        if (buf != null) {
            checkBuffer(buf.remaining(), 1)
            assertNT(buf[buf.limit() - 1] == terminator)
        }
    }

    private fun checkBuffer(bufferSize: Int, minimumSize: Int) {
        if (bufferSize < minimumSize) {
            throwIAE(bufferSize, minimumSize)
        }
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: ByteArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: ShortArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: IntArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: LongArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: FloatArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a array has enough capacity.
     *
     * @param buf  the array to check
     * @param size the minimum array capacity
     *
     * @throws IllegalArgumentException if `buf.length < size`
     */
    fun check(buf: DoubleArray, size: Int) {
        checkBuffer(buf.size, size)
    }

    /**
     * Helper method to ensure a CharSequence has enough characters.
     *
     * @param text the text to check
     * @param size the minimum number of characters
     *
     * @throws IllegalArgumentException if `text.length() < size`
     */
    fun check(text: CharSequence, size: Int) {
        checkBuffer(text.length, size)
    }

    /**
     * Helper method to ensure a buffer has enough capacity.
     *
     * @param buf  the buffer to check
     * @param size the minimum buffer capacity
     *
     * @throws IllegalArgumentException if `buf.remaining() < size`
     */
    fun check(buf: Buffer, size: Int) {
        checkBuffer(buf.remaining(), size)
    }

    /** @see .check
     */
    fun check(buf: Buffer, size: Long) {
        checkBuffer(buf.remaining(), size.toInt())
    }

    /**
     * Helper method to ensure a [CustomBuffer] has enough capacity.
     *
     * @param buf  the buffer to check
     * @param size the minimum buffer capacity
     *
     * @throws IllegalArgumentException if `buf.remaining() < size`
     */
    fun check(buf: CustomBuffer<*>, size: Int) {
        checkBuffer(buf.remaining(), size)
    }

    /** @see .check
     */
    fun check(buf: CustomBuffer<*>, size: Long) {
        checkBuffer(buf.remaining(), size.toInt())
    }

    fun checkSafe(buf: ShortArray?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.size, size)
        }
    }

    fun checkSafe(buf: IntArray?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.size, size)
        }
    }

    fun checkSafe(buf: LongArray?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.size, size)
        }
    }

    fun checkSafe(buf: FloatArray?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.size, size)
        }
    }

    fun checkSafe(buf: DoubleArray?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.size, size)
        }
    }

    fun checkSafe(buf: Buffer?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.remaining(), size)
        }
    }

    fun checkSafe(buf: Buffer?, size: Long) {
        if (buf != null) {
            checkBuffer(buf.remaining(), size.toInt())
        }
    }

    fun checkSafe(buf: CustomBuffer<*>?, size: Int) {
        if (buf != null) {
            checkBuffer(buf.remaining(), size)
        }
    }

    fun checkSafe(buf: CustomBuffer<*>?, size: Long) {
        if (buf != null) {
            checkBuffer(buf.remaining(), size.toInt())
        }
    }

    fun check(array: Array<Any?>, size: Int) {
        checkBuffer(array.size, size)
    }

    private fun checkBufferGT(bufferSize: Int, maximumSize: Int) {
        if (maximumSize < bufferSize) {
            throwIAEGT(bufferSize, maximumSize)
        }
    }

    fun checkGT(buf: Buffer, size: Int) {
        checkBufferGT(buf.remaining(), size)
    }

    fun checkGT(buf: CustomBuffer<*>, size: Int) {
        checkBufferGT(buf.remaining(), size)
    }

    fun check(index: Int, length: Int): Long {
        if (CHECKS) {
            CheckIntrinsics.checkIndex(index, length)
        }
        // Convert to long to support addressing up to 2^31-1 elements, regardless of sizeof(element).
        // The unsigned conversion helps the JIT produce code that is as fast as if int was returned.
        return Integer.toUnsignedLong(index)
    }

    // Separate calls to help inline check.
    private fun throwIAE(bufferSize: Int, minimumSize: Int) {
        throw IllegalArgumentException("Number of remaining elements is $bufferSize, must be at least $minimumSize")
    }

    private fun throwIAEGT(bufferSize: Int, maximumSize: Int) {
        throw IllegalArgumentException("Number of remaining buffer elements is $bufferSize, must be at most $maximumSize")
    }

    init {
        if (DEBUG_FUNCTIONS && !DEBUG) {
            APIUtil.DEBUG_STREAM.println("[LWJGL] The DEBUG_FUNCTIONS option requires DEBUG to produce output.")
        }
    }
}