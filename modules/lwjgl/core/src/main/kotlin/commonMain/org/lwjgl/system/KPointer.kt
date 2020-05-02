/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

import org.lwjgl.system.nio.*
import sun.misc.*
import kotlin.reflect.*

/**
 * Pointer interface.
 *
 *
 * LWJGL can run on both 32bit and 64bit architectures. Since LWJGL applications deal with native memory directly, this interface provides necessary
 * information about the underlying architecture of the running JVM process.
 *
 *
 * When interacting with native functions, pointer values are mapped to Java `long`. LWJGL automatically converts long values to the correct pointer
 * addresses when used in native code. Native functions sometimes require arrays of pointer values; the [PointerBuffer] class may be used for that
 * purpose. It has an API similar to a [LongBuffer] but handles pointer casts automatically.
 */
interface KPointer {
    /**
     * Returns the raw pointer address as a `long` value.
     *
     * @return the pointer address
     */
    fun address(): Long

    /** Default [KPointer] implementation.  */
    abstract class Default protected constructor(address: Long) : KPointer {
        companion object {
            val UNSAFE: Unsafe = MemoryUtil.UNSAFE
            var ADDRESS: Long = 0
            var BUFFER_CONTAINER: Long = 0
            var BUFFER_MARK: Long = 0
            var BUFFER_POSITION: Long = 0
            var BUFFER_LIMIT: Long = 0
            var BUFFER_CAPACITY: Long = 0
            protected fun <T : KCustomBuffer<*>> wrap(clazz: KClass<out T>, address: Long, capacity: Int): T {
                val buffer: T
                buffer = try {
                    UNSAFE.allocateInstance(clazz.java) as T
                } catch (e: InstantiationException) {
                    throw UnsupportedOperationException(e)
                }
                UNSAFE.putLong(buffer, ADDRESS, address)
                UNSAFE.putInt(buffer, BUFFER_MARK, -1)
                UNSAFE.putInt(buffer, BUFFER_LIMIT, capacity)
                UNSAFE.putInt(buffer, BUFFER_CAPACITY, capacity)
                return buffer
            }

            protected fun <T : KCustomBuffer<*>> wrap(
                clazz: KClass<out T>,
                address: Long,
                capacity: Int,
                container: KByteBuffer?
            ): T {
                val buffer: T
                buffer = try {
                    UNSAFE.allocateInstance(clazz.java) as T
                } catch (e: InstantiationException) {
                    throw UnsupportedOperationException(e)
                }
                UNSAFE.putLong(buffer, ADDRESS, address)
                UNSAFE.putInt(buffer, BUFFER_MARK, -1)
                UNSAFE.putInt(buffer, BUFFER_LIMIT, capacity)
                UNSAFE.putInt(buffer, BUFFER_CAPACITY, capacity)
                UNSAFE.putObject(buffer, BUFFER_CONTAINER, container)
                return buffer
            }

            init {
                try {
                    ADDRESS =
                        UNSAFE.objectFieldOffset(Default::class.java.getDeclaredField("address"))
                    BUFFER_CONTAINER = UNSAFE.objectFieldOffset(
                        CustomBuffer::class.java.getDeclaredField("container")
                    )
                    BUFFER_MARK =
                        UNSAFE.objectFieldOffset(CustomBuffer::class.java.getDeclaredField("mark"))
                    BUFFER_POSITION = UNSAFE.objectFieldOffset(
                        CustomBuffer::class.java.getDeclaredField("position")
                    )
                    BUFFER_LIMIT =
                        UNSAFE.objectFieldOffset(CustomBuffer::class.java.getDeclaredField("limit"))
                    BUFFER_CAPACITY = UNSAFE.objectFieldOffset(
                        CustomBuffer::class.java.getDeclaredField("capacity")
                    )
                } catch (t: Throwable) {
                    throw UnsupportedOperationException(t)
                }
            }
        }

        // Removed final due to JDK-8139758. TODO: Restore if the fix is backported to JDK 8.
        protected var address: Long
        override fun address(): Long {
            return address
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is KPointer) {
                return false
            }
            return address == other.address()
        }

        override fun hashCode(): Int {
            return (address xor (address ushr 32)).toInt()
        }

        override fun toString(): String {
            return String.format("%s pointer [0x%X]", javaClass.simpleName, address)
        }

        init {
            if (Checks.CHECKS && address == MemoryUtil.NULL) {
                throw NullPointerException()
            }
            this.address = address
        }
    }

    companion object {
        /** The pointer size in bytes. Will be 4 on a 32bit JVM and 8 on a 64bit one.  */
        val POINTER_SIZE = MemoryAccessJNI.getPointerSize()

        /** The pointer size power-of-two. Will be 2 on a 32bit JVM and 3 on a 64bit one.  */
        val POINTER_SHIFT = if (POINTER_SIZE == 8) 3 else 2

        /** The value of `sizeof(long)` for the current platform.  */
        val CLONG_SIZE =
            if (POINTER_SIZE == 8 && Platform.get() === Platform.WINDOWS) 4 else POINTER_SIZE

        /** The value of `sizeof(long)` as a power-of-two.  */
        val CLONG_SHIFT = if (CLONG_SIZE == 8) 3 else 2

        /** Will be true on a 32bit JVM.  */
        val BITS32 = POINTER_SIZE * 8 == 32

        /** Will be true on a 64bit JVM.  */
        val BITS64 = POINTER_SIZE * 8 == 64
    }
}