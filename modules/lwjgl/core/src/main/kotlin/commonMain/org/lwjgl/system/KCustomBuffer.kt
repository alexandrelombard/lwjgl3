/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

import org.lwjgl.system.nio.*

/** Base class of custom buffers with an API that mirrors `java.nio` for convenience.  */
abstract class KCustomBuffer<SELF : KCustomBuffer<SELF>?> protected constructor(
    address: Long,
    protected var container: KByteBuffer?,
    protected var mark: Int,
    protected var position: Int,
    protected var limit: Int,
    protected var capacity: Int
) : Pointer.Default(address) {

    /** Returns the `sizeof` a single element in the buffer.  */
    abstract fun sizeof(): Int

    /** Returns the buffer's base address. [INTERNAL USE ONLY]  */
    fun address0(): Long {
        return address
    }

    /** Returns the memory address at the current buffer position.  */
    override fun address(): Long {
        return address + Integer.toUnsignedLong(position) * sizeof()
    }

    /** Returns the memory address at the specified buffer position.  */
    fun address(position: Int): Long {
        return address + Integer.toUnsignedLong(position) * sizeof()
    }

    /**
     * Frees the buffer allocation.
     *
     *
     * This method should not be used if the memory backing this buffer is not owned by the buffer.
     */
    fun free() {
        MemoryUtil.nmemFree(address)
    }

    /**
     * Returns this buffer's capacity.
     *
     * @return the capacity of this buffer
     */
    fun capacity(): Int {
        return capacity
    }

    /**
     * Returns this buffer's position.
     *
     * @return the position of this buffer
     */
    fun position(): Int {
        return position
    }

    /**
     * Sets this buffer's position. If the mark is defined and larger than the new position then it is discarded.
     *
     * @param position the new position value; must be non-negative and no larger than the current limit
     *
     * @return This buffer
     *
     * @throws IllegalArgumentException If the preconditions on `newPosition` do not hold
     */
    fun position(position: Int): SELF {
        require(!(position < 0 || limit < position))
        this.position = position
        if (position < mark) {
            mark = -1
        }
        return self()
    }

    /**
     * Returns this buffer's limit.
     *
     * @return the limit of this buffer
     */
    fun limit(): Int {
        return limit
    }

    /**
     * Sets this buffer's limit. If the position is larger than the new limit then it is set to the new limit. If the mark is defined and larger than the new
     * limit then it is discarded.
     *
     * @param limit the new limit value; must be non-negative and no larger than this buffer's capacity
     *
     * @return This buffer
     *
     * @throws IllegalArgumentException If the preconditions on `newLimit` do not hold
     */
    fun limit(limit: Int): SELF {
        require(!(limit < 0 || capacity < limit))
        this.limit = limit
        if (limit < position) {
            position = limit
        }
        if (limit < mark) {
            mark = -1
        }
        return self()
    }

    /**
     * Sets this buffer's mark at its position.
     *
     * @return This buffer
     */
    fun mark(): SELF {
        mark = position
        return self()
    }

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     *
     * Invoking this method neither changes nor discards the mark's value.
     *
     * @return This buffer
     *
     * @throws InvalidMarkException If the mark has not been set
     */
    fun reset(): SELF {
        val m = mark
        if (m < 0) {
            throw KInvalidMarkException()
        }
        position = m
        return self()
    }

    /**
     * Clears this buffer. The position is set to zero, the limit is set to the capacity, and the mark is discarded.
     *
     *
     * Invoke this method before using a sequence of channel-read or *put* operations to fill this buffer. For example:
     *
     * <blockquote><pre>
     * buf.clear();     // Prepare buffer for reading
     * in.read(buf);    // Read data</pre></blockquote>
     *
     *
     * This method does not actually erase the data in the buffer, but it is named as if it did because it will most often be used in situations in which
     * that might as well be the case.
     *
     * @return This buffer
     */
    fun clear(): SELF {
        position = 0
        limit = capacity
        mark = -1
        return self()
    }

    /**
     * Flips this buffer. The limit is set to the current position and then the position is set to zero. If the mark is defined then it is discarded.
     *
     *
     * After a sequence of channel-read or *put* operations, invoke this method to prepare for a sequence of channel-write or relative *get*
     * operations. For example:
     *
     * <blockquote><pre>
     * buf.put(magic);    // Prepend header
     * in.read(buf);      // Read data into rest of buffer
     * buf.flip();        // Flip buffer
     * out.write(buf);    // Write header + data to channel</pre></blockquote>
     *
     *
     * This method is often used in conjunction with the [.compact] method when transferring data from one place to another.
     *
     * @return This buffer
     */
    fun flip(): SELF {
        limit = position
        position = 0
        mark = -1
        return self()
    }

    /**
     * Rewinds this buffer. The position is set to zero and the mark is discarded.
     *
     *
     * Invoke this method before a sequence of channel-write or *get* operations, assuming that the limit has already been set appropriately. For
     * example:
     *
     * <blockquote><pre>
     * out.write(buf);    // Write remaining data
     * buf.rewind();      // Rewind buffer
     * buf.get(array);    // Copy data into array</pre></blockquote>
     *
     * @return This buffer
     */
    fun rewind(): SELF {
        position = 0
        mark = -1
        return self()
    }

    /**
     * Returns the number of elements between the current position and the limit.
     *
     * @return the number of elements remaining in this buffer
     */
    fun remaining(): Int {
        return limit - position
    }

    /**
     * Tells whether there are any elements between the current position and the limit.
     *
     * @return `true` if, and only if, there is at least one element remaining in this buffer
     */
    fun hasRemaining(): Boolean {
        return position < limit
    }

    /**
     * Creates a new buffer whose content is a shared subsequence of this buffer's content.
     *
     *
     * The content of the new buffer will start at this buffer's current position. Changes to this buffer's content will be visible in the new buffer, and
     * vice versa; the two buffers' position, limit, and mark values will be independent.
     *
     *
     * The new buffer's position will be zero, its capacity and its limit will be the number of elements remaining in this buffer, and its mark will be
     * undefined. The new buffer will be read-only if, and only if, this buffer is read-only.
     *
     * @return the new buffer
     */
    fun slice(): SELF {
        val slice: SELF
        slice = try {
            UNSAFE.allocateInstance(this.javaClass) as SELF
        } catch (e: InstantiationException) {
            throw UnsupportedOperationException(e)
        }
        UNSAFE.putLong(
            slice,
            ADDRESS,
            address + Integer.toUnsignedLong(position) * sizeof()
        )
        UNSAFE.putInt(slice, BUFFER_MARK, -1)
        UNSAFE.putInt(slice, BUFFER_LIMIT, remaining())
        UNSAFE.putInt(slice, BUFFER_CAPACITY, remaining())
        UNSAFE.putObject(slice, BUFFER_CONTAINER, container)
        return slice
    }

    /**
     * Returns a slice of this buffer between `(buffer.position() + offset)` and `(buffer.position() + offset + capacity)`.
     *
     *
     * The position and limit of this buffer are preserved after a call to this method.
     *
     * @param offset   the slice offset, it must be  `this.remaining()`
     * @param capacity the slice length, it must be  `this.capacity() - (this.position() + offset)`
     *
     * @return the sliced buffer
     */
    fun slice(offset: Int, capacity: Int): SELF {
        val position = position + offset
        require(!(offset < 0 || limit < offset))
        require(!(capacity < 0 || this.capacity - position < capacity))
        val slice: SELF
        slice = try {
            UNSAFE.allocateInstance(this.javaClass) as SELF
        } catch (e: InstantiationException) {
            throw UnsupportedOperationException(e)
        }
        UNSAFE.putLong(
            slice,
            ADDRESS,
            address + Integer.toUnsignedLong(position) * sizeof()
        )
        UNSAFE.putInt(slice, BUFFER_MARK, -1)
        UNSAFE.putInt(slice, BUFFER_LIMIT, capacity)
        UNSAFE.putInt(slice, BUFFER_CAPACITY, capacity)
        UNSAFE.putObject(slice, BUFFER_CONTAINER, container)
        return slice
    }

    /**
     * Creates a new buffer that shares this buffer's content.
     *
     *
     * The content of the new buffer will be that of this buffer. Changes to this buffer's content will be visible in the new buffer, and vice versa; the
     * two buffers' position, limit, and mark values will be independent.
     *
     *
     * The new buffer's capacity, limit and position will be identical to those of this buffer.
     *
     * @return the new buffer
     */
    fun duplicate(): SELF {
        val dup: SELF
        dup = try {
            UNSAFE.allocateInstance(this.javaClass) as SELF
        } catch (e: InstantiationException) {
            throw UnsupportedOperationException(e)
        }
        UNSAFE.putLong(dup, ADDRESS, address)
        UNSAFE.putInt(dup, BUFFER_MARK, mark)
        UNSAFE.putInt(dup, BUFFER_POSITION, position)
        UNSAFE.putInt(dup, BUFFER_LIMIT, limit)
        UNSAFE.putInt(dup, BUFFER_CAPACITY, capacity)
        UNSAFE.putObject(dup, BUFFER_CONTAINER, container)
        return dup
    }
    // -- Bulk get operations --
    /**
     * Relative bulk *put* method&nbsp;&nbsp;*(optional operation)*.
     *
     *
     * This method transfers the elements remaining in the specified source buffer into this buffer. If there are more elements remaining in the source
     * buffer than in this buffer, that is, if `src.remaining()`&nbsp;`&gt;`&nbsp;`remaining()`, then no elements are transferred and a
     * [BufferOverflowException] is thrown.
     *
     *
     * Otherwise, this method copies *n*&nbsp;=&nbsp;`src.remaining()` elements from the specified buffer into this buffer, starting at each
     * buffer's current position. The positions of both buffers are then incremented by *n*.
     *
     *
     * In other words, an invocation of this method of the form `dst.put(src)` has exactly the same effect as the loop
     *
     * <pre>
     * while (src.hasRemaining())
     * dst.put(src.get()); </pre>
     *
     *
     * except that it first checks that there is sufficient space in this buffer and it is potentially much more efficient.
     *
     * @param src the source buffer from which elements are to be read; must not be this buffer
     *
     * @return This buffer
     *
     * @throws BufferOverflowException If there is insufficient space in this buffer for the remaining elements in the source buffer
     * @throws IllegalArgumentException         If the source buffer is this buffer
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    fun put(src: SELF): SELF {
        require(!(src === this))
        val n = src!!.remaining()
        if (remaining() < n) {
            throw KBufferOverflowException()
        }
        MemoryUtil.memCopy(src.address(), this.address(), Integer.toUnsignedLong(n) * sizeof())
        position += n
        return self()
    }

    /**
     * Compacts this buffer&nbsp;&nbsp;*(optional operation)*.
     *
     *
     * The elements between the buffer's current position and its limit, if any, are copied to the beginning of the buffer. That is, the element at index
     * *p*&nbsp;=&nbsp;`position()` is copied to index zero, the element at index *p*&nbsp;+&nbsp;1 is copied to index one, and so forth until
     * the element at index `limit()`&nbsp;-&nbsp;1 is copied to index *n*&nbsp;=&nbsp;`limit()`&nbsp;-&nbsp;`1`&nbsp;-&nbsp;
     * *p*.
     * The buffer's position is then set to *n+1* and its limit is set to its capacity. The mark, if defined, is discarded.
     *
     *
     * The buffer's position is set to the number of elements copied, rather than to zero, so that an invocation of this method can be followed
     * immediately by an invocation of another relative *put* method.
     *
     * @return This buffer
     *
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    fun compact(): SELF {
        MemoryUtil.memCopy(address(), address, Integer.toUnsignedLong(remaining()) * sizeof())
        position(remaining())
        limit(capacity())
        mark = -1
        return self()
    }

    /**
     * Returns a string summarizing the state of this buffer.
     *
     * @return A summary string
     */
    override fun toString(): String {
        return javaClass.name + "[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]"
    }

    // -----------------------------
    protected abstract fun self(): SELF
    protected fun nextGetIndex(): Int {
        if (position < limit) {
            return position++
        }
        throw KBufferUnderflowException()
    }

    protected fun nextPutIndex(): Int {
        if (position < limit) {
            return position++
        }
        throw KBufferOverflowException()
    }

}