/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

import java.nio.*
import java.util.*
import java.util.function.*
import java.util.stream.*

/** Base class of struct custom buffers.  */
abstract class KStructBuffer<T : Struct, SELF : KStructBuffer<T, SELF>> : KCustomBuffer<SELF>, Iterable<T> {
    protected constructor(container: ByteBuffer, remaining: Int) : super(
        MemoryUtil.memAddress(container),
        container,
        -1,
        0,
        remaining,
        remaining
    ) {
    }

    protected constructor(
        address: Long,
        container: ByteBuffer,
        mark: Int,
        position: Int,
        limit: Int,
        capacity: Int
    ) : super(address, container, mark, position, limit, capacity) {
    }

    override fun sizeof(): Int {
        return elementFactory.sizeof()
    }

    /**
     * Relative *get* method. Reads the struct at this buffer's current position, and then increments the position.
     *
     *
     * The struct instance returned is a view of the buffer at the current position. Changes to this buffer's content will be visible in the struct instance
     * and vice versa.
     *
     * @return the struct at the buffer's current position
     *
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    fun get(): T {
        return elementFactory.wrap(address, nextGetIndex(), container)
    }

    /**
     * Relative *get* method. Reads the struct data at this buffer's current position into the specified struct, and then increments the position.
     *
     * @return the struct at the buffer's current position
     *
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    operator fun get(value: T): SELF {
        val sizeof = elementFactory.sizeof()
        MemoryUtil.memCopy(address + Integer.toUnsignedLong(nextGetIndex()) * sizeof, value.address(), sizeof.toLong())
        return self()
    }

    /**
     * Relative *put* method&nbsp;&nbsp;*(optional operation)*.
     *
     *
     * Writes the specified struct into this buffer at the current position, and then increments the position.
     *
     * @param value the struct to be written
     *
     * @return This buffer
     *
     * @throws BufferOverflowException If this buffer's current position is not smaller than its limit
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    fun put(value: T): SELF {
        val sizeof = elementFactory.sizeof()
        MemoryUtil.memCopy(value.address(), address + Integer.toUnsignedLong(nextPutIndex()) * sizeof, sizeof.toLong())
        return self()
    }

    /**
     * Absolute *get* method. Reads the struct at the specified index.
     *
     *
     * The struct instance returned is a view of the buffer at the specified position. Changes to this buffer's content will be visible in the struct
     * instance and vice versa.
     *
     * @param index the index from which the struct will be read
     *
     * @return the struct at the specified index
     *
     * @throws IndexOutOfBoundsException If `index` is negative or not smaller than the buffer's limit
     */
    operator fun get(index: Int): T {
        return elementFactory.wrap(address, check(index, limit), container)
    }

    /**
     * Absolute *get* method. Reads the struct data at the specified index into the specified struct.
     *
     * @param index the index from which the struct will be read
     *
     * @return the struct at the specified index
     *
     * @throws IndexOutOfBoundsException If `index` is negative or not smaller than the buffer's limit
     */
    operator fun get(index: Int, value: T): SELF {
        val sizeof = elementFactory.sizeof()
        MemoryUtil.memCopy(address + Checks.check(index, limit) * sizeof, value.address(), sizeof.toLong())
        return self()
    }

    /**
     * Absolute *put* method&nbsp;&nbsp;*(optional operation)*.
     *
     *
     * Writes the specified struct into this buffer at the specified index.
     *
     * @param index the index at which the struct will be written
     * @param value the struct value to be written
     *
     * @return This buffer
     *
     * @throws IndexOutOfBoundsException        If `index` is negative or not smaller than the buffer's limit
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    fun put(index: Int, value: T): SELF {
        val sizeof = elementFactory.sizeof()
        MemoryUtil.memCopy(value.address(), address + Checks.check(index, limit) * sizeof, sizeof.toLong())
        return self()
    }

    /**
     * Relative *apply* method.
     *
     *
     * Passes the struct at the buffer's current position to the specified [Consumer], and then increments the position.
     *
     * @return This buffer
     *
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    fun apply(consumer: Consumer<T>): SELF {
        consumer.accept(get())
        return self()
    }

    /**
     * Absolute *apply* method.
     *
     *
     * Passes the struct at the specified position to the specified [Consumer].
     *
     * @param index the index where the `consumer` will be applied.
     *
     * @return This buffer
     *
     * @throws IndexOutOfBoundsException If `index` is negative or not smaller than the buffer's limit
     */
    fun apply(index: Int, consumer: Consumer<T>): SELF {
        consumer.accept(get(index))
        return self()
    }

    // --------------------------------------
    override fun iterator(): MutableIterator<T> {
        return KStructIterator<T, SELF>(address, container, elementFactory, position, limit)
    }

    // This class is static to avoid capturing the StructBuffer instance. Hotspot trivially marks the instance
    // as escaping when this happens, even if the iterator instance is not escaping and scalar replaced. This
    // is not a problem on Graal. Also, see JDK-8166840.
    private class KStructIterator<T : Struct, SELF : KStructBuffer<T, SELF>> internal constructor(
        private val address: Long,
        private val container: ByteBuffer?,
        private val factory: T,
        private var index: Int,
        private val fence: Int
    ) : MutableIterator<T> {
        override fun hasNext(): Boolean {
            return index < fence
        }

        override fun next(): T {
            if (Checks.CHECKS && fence <= index) {
                throw NoSuchElementException()
            }
            return factory.wrap(address, index++, container)
        }

        override fun forEachRemaining(action: Consumer<in T>) {
            Objects.requireNonNull(action)
            var i = index
            try {
                while (i < fence) {
                    action.accept(factory.wrap(address, i, container))
                    i++
                }
            } finally {
                index = i
            }
        }

        override fun remove() {
            throw UnsupportedOperationException("remove")
        }

    }

    override fun forEach(action: Consumer<in T>) {
        Objects.requireNonNull(action)
        val factory = elementFactory
        var i = position
        val fence = limit
        while (i < fence) {
            action.accept(factory.wrap(address, i, container))
            i++
        }
    }

    override fun spliterator(): Spliterator<T> {
        return KStructSpliterator<T, SELF>(address, container, elementFactory, position, limit)
    }

    private class KStructSpliterator<T : Struct, SELF : KStructBuffer<T, SELF>> internal constructor(
        private val address: Long,
        private val container: ByteBuffer?,
        private val factory: T,
        private var index: Int,
        private val fence: Int
    ) : Spliterator<T> {
        override fun tryAdvance(action: Consumer<in T>): Boolean {
            Objects.requireNonNull(action)
            if (index < fence) {
                action.accept(factory.wrap(address, index++, container))
                return true
            }
            return false
        }

        override fun trySplit(): Spliterator<T>? {
            val lo = index
            val mid = lo + fence ushr 1
            return if (lo < mid) KStructSpliterator<T, SELF>(address, container, factory, lo, mid.also { index = it }) else null
        }

        override fun estimateSize(): Long {
            return (fence - index).toLong()
        }

        override fun characteristics(): Int {
            return Spliterator.ORDERED or Spliterator.IMMUTABLE or Spliterator.NONNULL or Spliterator.SIZED or Spliterator.SUBSIZED
        }

        override fun forEachRemaining(action: Consumer<in T>) {
            Objects.requireNonNull(action)
            var i = index
            try {
                while (i < fence) {
                    action.accept(factory.wrap(address, i, container))
                    i++
                }
            } finally {
                index = i
            }
        }

        override fun getComparator(): Comparator<in T> {
            throw IllegalStateException()
        }

    }

    /** Returns a sequential `Stream` with this struct buffer as its source.  */
    fun stream(): Stream<T> {
        return StreamSupport.stream(spliterator(), false)
    }

    /** Returns a parallel `Stream` with this struct buffer as its source.  */
    fun parallelStream(): Stream<T> {
        return StreamSupport.stream(spliterator(), true)
    }

    // --------------------------------------
    protected abstract val elementFactory: T

    companion object {
        private fun check(index: Int, length: Int): Int {
            if (Checks.CHECKS && (index < 0 || length <= index)) {
                throw IndexOutOfBoundsException()
            }
            return index
        }
    }
}