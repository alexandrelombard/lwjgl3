/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system.nio

import java.util.*

/**
 * A container for data of a specific primitive type.
 *
 *
 *  A buffer is a linear, finite sequence of elements of a specific
 * primitive type.  Aside from its content, the essential properties of a
 * buffer are its capacity, limit, and position:
 *
 * <blockquote>
 *
 *
 *  A buffer's *capacity* is the number of elements it contains.  The
 * capacity of a buffer is never negative and never changes.
 *
 *
 *  A buffer's *limit* is the index of the first element that should
 * not be read or written.  A buffer's limit is never negative and is never
 * greater than its capacity.
 *
 *
 *  A buffer's *position* is the index of the next element to be
 * read or written.  A buffer's position is never negative and is never
 * greater than its limit.
 *
</blockquote> *
 *
 *
 *  There is one subclass of this class for each non-boolean primitive type.
 *
 *
 * <h2> Transferring data </h2>
 *
 *
 *  Each subclass of this class defines two categories of *get* and
 * *put* operations:
 *
 * <blockquote>
 *
 *
 *  *Relative* operations read or write one or more elements starting
 * at the current position and then increment the position by the number of
 * elements transferred.  If the requested transfer exceeds the limit then a
 * relative *get* operation throws a [BufferUnderflowException]
 * and a relative *put* operation throws a [   ]; in either case, no data is transferred.
 *
 *
 *  *Absolute* operations take an explicit element index and do not
 * affect the position.  Absolute *get* and *put* operations throw
 * an [IndexOutOfBoundsException] if the index argument exceeds the
 * limit.
 *
</blockquote> *
 *
 *
 *  Data may also, of course, be transferred in to or out of a buffer by the
 * I/O operations of an appropriate channel, which are always relative to the
 * current position.
 *
 *
 * <h2> Marking and resetting </h2>
 *
 *
 *  A buffer's *mark* is the index to which its position will be reset
 * when the [reset][.reset] method is invoked.  The mark is not always
 * defined, but when it is defined it is never negative and is never greater
 * than the position.  If the mark is defined then it is discarded when the
 * position or the limit is adjusted to a value smaller than the mark.  If the
 * mark is not defined then invoking the [reset][.reset] method causes an
 * [InvalidMarkException] to be thrown.
 *
 *
 * <h2> Invariants </h2>
 *
 *
 *  The following invariant holds for the mark, position, limit, and
 * capacity values:
 *
 * <blockquote>
 * `0` `<=`
 * *mark* `<=`
 * *position* `<=`
 * *limit* `<=`
 * *capacity*
</blockquote> *
 *
 *
 *  A newly-created buffer always has a position of zero and a mark that is
 * undefined.  The initial limit may be zero, or it may be some other value
 * that depends upon the type of the buffer and the manner in which it is
 * constructed.  Each element of a newly-allocated buffer is initialized
 * to zero.
 *
 *
 * <h2> Additional operations </h2>
 *
 *
 *  In addition to methods for accessing the position, limit, and capacity
 * values and for marking and resetting, this class also defines the following
 * operations upon buffers:
 *
 *
 *
 *  *
 *
 * [.clear] makes a buffer ready for a new sequence of
 * channel-read or relative *put* operations: It sets the limit to the
 * capacity and the position to zero.
 *
 *  *
 *
 * [.flip] makes a buffer ready for a new sequence of
 * channel-write or relative *get* operations: It sets the limit to the
 * current position and then sets the position to zero.
 *
 *  *
 *
 * [.rewind] makes a buffer ready for re-reading the data that
 * it already contains: It leaves the limit unchanged and sets the position
 * to zero.
 *
 *  *
 *
 * [.slice] creates a subsequence of a buffer: It leaves the
 * limit and the position unchanged.
 *
 *  *
 *
 * [.duplicate] creates a shallow copy of a buffer: It leaves
 * the limit and the position unchanged.
 *
 *
 *
 *
 * <h2> Read-only buffers </h2>
 *
 *
 *  Every buffer is readable, but not every buffer is writable.  The
 * mutation methods of each buffer class are specified as *optional
 * operations* that will throw a [ReadOnlyBufferException] when
 * invoked upon a read-only buffer.  A read-only buffer does not allow its
 * content to be changed, but its mark, position, and limit values are mutable.
 * Whether or not a buffer is read-only may be determined by invoking its
 * [isReadOnly][.isReadOnly] method.
 *
 *
 * <h2> Thread safety </h2>
 *
 *
 *  Buffers are not safe for use by multiple concurrent threads.  If a
 * buffer is to be used by more than one thread then access to the buffer
 * should be controlled by appropriate synchronization.
 *
 *
 * <h2> Invocation chaining </h2>
 *
 *
 *  Methods in this class that do not otherwise have a value to return are
 * specified to return the buffer upon which they are invoked.  This allows
 * method invocations to be chained; for example, the sequence of statements
 *
 * <blockquote><pre>
 * b.flip();
 * b.position(23);
 * b.limit(42);</pre></blockquote>
 *
 * can be replaced by the single, more compact statement
 *
 * <blockquote><pre>
 * b.flip().position(23).limit(42);</pre></blockquote>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
abstract class Buffer internal constructor(mark: Int, pos: Int, lim: Int, cap: Int) {
    // Invariants: mark <= position <= limit <= capacity
    private var mark = -1
    private var position = 0
    private var limit = 0
    private var capacity: Int

    // Used by heap byte buffers or direct buffers with Unsafe access
    // For heap byte buffers this field will be the address relative to the
    // array base address and offset into that array. The address might
    // not align on a word boundary for slices, nor align at a long word
    // (8 byte) boundary for byte[] allocations on 32-bit systems.
    // For direct buffers it is the start address of the memory region. The
    // address might not align on a word boundary for slices, nor when created
    // using JNI, see NewDirectByteBuffer(void*, long).
    // Should ideally be declared final
    // NOTE: hoisted here for speed in JNI GetDirectBufferAddress
    var address: Long = 0

    /**
     * Returns this buffer's capacity.
     *
     * @return  The capacity of this buffer
     */
    fun capacity(): Int {
        return capacity
    }

    /**
     * Returns this buffer's position.
     *
     * @return  The position of this buffer
     */
    fun position(): Int {
        return position
    }

    /**
     * Sets this buffer's position.  If the mark is defined and larger than the
     * new position then it is discarded.
     *
     * @param  newPosition
     * The new position value; must be non-negative
     * and no larger than the current limit
     *
     * @return  This buffer
     *
     * @throws  IllegalArgumentException
     * If the preconditions on `newPosition` do not hold
     */
    fun position(newPosition: Int): Buffer {
        if (newPosition > limit || newPosition < 0) throw createPositionException(newPosition)
        position = newPosition
        if (mark > position) mark = -1
        return this
    }

    /**
     * Verify that `0 < newPosition <= limit`
     *
     * @param newPosition
     * The new position value
     *
     * @throws IllegalArgumentException
     * If the specified position is out of bounds.
     */
    private fun createPositionException(newPosition: Int): IllegalArgumentException {
        var msg: String?
        msg = if (newPosition > limit) {
            "newPosition > limit: ($newPosition > $limit)"
        } else { // assume negative
            assert(newPosition < 0) { "newPosition expected to be negative" }
            "newPosition < 0: ($newPosition < 0)"
        }
        return IllegalArgumentException(msg)
    }

    /**
     * Returns this buffer's limit.
     *
     * @return  The limit of this buffer
     */
    fun limit(): Int {
        return limit
    }

    /**
     * Sets this buffer's limit.  If the position is larger than the new limit
     * then it is set to the new limit.  If the mark is defined and larger than
     * the new limit then it is discarded.
     *
     * @param  newLimit
     * The new limit value; must be non-negative
     * and no larger than this buffer's capacity
     *
     * @return  This buffer
     *
     * @throws  IllegalArgumentException
     * If the preconditions on `newLimit` do not hold
     */
    fun limit(newLimit: Int): Buffer {
        if (newLimit > capacity || newLimit < 0) throw createLimitException(newLimit)
        limit = newLimit
        if (position > limit) position = limit
        if (mark > limit) mark = -1
        return this
    }

    /**
     * Verify that `0 < newLimit <= capacity`
     *
     * @param newLimit
     * The new limit value
     *
     * @throws IllegalArgumentException
     * If the specified limit is out of bounds.
     */
    private fun createLimitException(newLimit: Int): IllegalArgumentException {
        var msg: String? = null
        msg = if (newLimit > capacity) {
            "newLimit > capacity: ($newLimit > $capacity)"
        } else { // assume negative
            assert(newLimit < 0) { "newLimit expected to be negative" }
            "newLimit < 0: ($newLimit < 0)"
        }
        return IllegalArgumentException(msg)
    }

    /**
     * Sets this buffer's mark at its position.
     *
     * @return  This buffer
     */
    fun mark(): Buffer {
        mark = position
        return this
    }

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     *
     *  Invoking this method neither changes nor discards the mark's
     * value.
     *
     * @return  This buffer
     *
     * @throws  InvalidMarkException
     * If the mark has not been set
     */
    fun reset(): Buffer {
        val m = mark
        if (m < 0) throw KInvalidMarkException()
        position = m
        return this
    }

    /**
     * Clears this buffer.  The position is set to zero, the limit is set to
     * the capacity, and the mark is discarded.
     *
     *
     *  Invoke this method before using a sequence of channel-read or
     * *put* operations to fill this buffer.  For example:
     *
     * <blockquote><pre>
     * buf.clear();     // Prepare buffer for reading
     * in.read(buf);    // Read data</pre></blockquote>
     *
     *
     *  This method does not actually erase the data in the buffer, but it
     * is named as if it did because it will most often be used in situations
     * in which that might as well be the case.
     *
     * @return  This buffer
     */
    fun clear(): Buffer {
        position = 0
        limit = capacity
        mark = -1
        return this
    }

    /**
     * Flips this buffer.  The limit is set to the current position and then
     * the position is set to zero.  If the mark is defined then it is
     * discarded.
     *
     *
     *  After a sequence of channel-read or *put* operations, invoke
     * this method to prepare for a sequence of channel-write or relative
     * *get* operations.  For example:
     *
     * <blockquote><pre>
     * buf.put(magic);    // Prepend header
     * in.read(buf);      // Read data into rest of buffer
     * buf.flip();        // Flip buffer
     * out.write(buf);    // Write header + data to channel</pre></blockquote>
     *
     *
     *  This method is often used in conjunction with the [ ][java.nio.ByteBuffer.compact] method when transferring data from
     * one place to another.
     *
     * @return  This buffer
     */
    fun flip(): Buffer {
        limit = position
        position = 0
        mark = -1
        return this
    }

    /**
     * Rewinds this buffer.  The position is set to zero and the mark is
     * discarded.
     *
     *
     *  Invoke this method before a sequence of channel-write or *get*
     * operations, assuming that the limit has already been set
     * appropriately.  For example:
     *
     * <blockquote><pre>
     * out.write(buf);    // Write remaining data
     * buf.rewind();      // Rewind buffer
     * buf.get(array);    // Copy data into array</pre></blockquote>
     *
     * @return  This buffer
     */
    fun rewind(): Buffer {
        position = 0
        mark = -1
        return this
    }

    /**
     * Returns the number of elements between the current position and the
     * limit.
     *
     * @return  The number of elements remaining in this buffer
     */
    fun remaining(): Int {
        return limit - position
    }

    /**
     * Tells whether there are any elements between the current position and
     * the limit.
     *
     * @return  `true` if, and only if, there is at least one element
     * remaining in this buffer
     */
    fun hasRemaining(): Boolean {
        return position < limit
    }

    /**
     * Tells whether or not this buffer is read-only.
     *
     * @return  `true` if, and only if, this buffer is read-only
     */
    abstract val isReadOnly: Boolean

    /**
     * Tells whether or not this buffer is backed by an accessible
     * array.
     *
     *
     *  If this method returns `true` then the [array][.array]
     * and [arrayOffset][.arrayOffset] methods may safely be invoked.
     *
     *
     * @return  `true` if, and only if, this buffer
     * is backed by an array and is not read-only
     *
     * @since 1.6
     */
    abstract fun hasArray(): Boolean

    /**
     * Returns the array that backs this
     * buffer&nbsp;&nbsp;*(optional operation)*.
     *
     *
     *  This method is intended to allow array-backed buffers to be
     * passed to native code more efficiently. Concrete subclasses
     * provide more strongly-typed return values for this method.
     *
     *
     *  Modifications to this buffer's content will cause the returned
     * array's content to be modified, and vice versa.
     *
     *
     *  Invoke the [hasArray][.hasArray] method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.
     *
     * @return  The array that backs this buffer
     *
     * @throws  ReadOnlyBufferException
     * If this buffer is backed by an array but is read-only
     *
     * @throws  UnsupportedOperationException
     * If this buffer is not backed by an accessible array
     *
     * @since 1.6
     */
    abstract fun array(): Any?

    /**
     * Returns the offset within this buffer's backing array of the first
     * element of the buffer&nbsp;&nbsp;*(optional operation)*.
     *
     *
     *  If this buffer is backed by an array then buffer position *p*
     * corresponds to array index *p*&nbsp;+&nbsp;`arrayOffset()`.
     *
     *
     *  Invoke the [hasArray][.hasArray] method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.
     *
     * @return  The offset within this buffer's array
     * of the first element of the buffer
     *
     * @throws  ReadOnlyBufferException
     * If this buffer is backed by an array but is read-only
     *
     * @throws  UnsupportedOperationException
     * If this buffer is not backed by an accessible array
     *
     * @since 1.6
     */
    abstract fun arrayOffset(): Int

    /**
     * Tells whether or not this buffer is
     * [*direct*](ByteBuffer.html#direct).
     *
     * @return  `true` if, and only if, this buffer is direct
     *
     * @since 1.6
     */
    abstract val isDirect: Boolean

    /**
     * Creates a new buffer whose content is a shared subsequence of
     * this buffer's content.
     *
     *
     *  The content of the new buffer will start at this buffer's current
     * position.  Changes to this buffer's content will be visible in the new
     * buffer, and vice versa; the two buffers' position, limit, and mark
     * values will be independent.
     *
     *
     *  The new buffer's position will be zero, its capacity and its limit
     * will be the number of elements remaining in this buffer, its mark will be
     * undefined. The new buffer will be direct if, and only if, this buffer is
     * direct, and it will be read-only if, and only if, this buffer is
     * read-only.
     *
     * @return  The new buffer
     *
     * @since 9
     */
    abstract fun slice(): Buffer?

    /**
     * Creates a new buffer that shares this buffer's content.
     *
     *
     *  The content of the new buffer will be that of this buffer.  Changes
     * to this buffer's content will be visible in the new buffer, and vice
     * versa; the two buffers' position, limit, and mark values will be
     * independent.
     *
     *
     *  The new buffer's capacity, limit, position and mark values will be
     * identical to those of this buffer. The new buffer will be direct if, and
     * only if, this buffer is direct, and it will be read-only if, and only if,
     * this buffer is read-only.
     *
     * @return  The new buffer
     *
     * @since 9
     */
    abstract fun duplicate(): Buffer?
    // -- Package-private methods for bounds checking, etc. --
    /**
     *
     * @return the base reference, paired with the address
     * field, which in combination can be used for unsafe access into a heap
     * buffer or direct byte buffer (and views of).
     */
    abstract fun base(): Any?

    /**
     * Checks the current position against the limit, throwing a [ ] if it is not smaller than the limit, and then
     * increments the position.
     *
     * @return  The current position value, before it is incremented
     */
    internal fun nextGetIndex(): Int {                          // package-private
        if (position >= limit) throw KBufferUnderflowException()
        return position++
    }

    internal fun nextGetIndex(nb: Int): Int {                    // package-private
        if (limit - position < nb) throw KBufferUnderflowException()
        val p = position
        position += nb
        return p
    }

    /**
     * Checks the current position against the limit, throwing a [ ] if it is not smaller than the limit, and then
     * increments the position.
     *
     * @return  The current position value, before it is incremented
     */
    internal fun nextPutIndex(): Int {                          // package-private
        if (position >= limit) throw KBufferOverflowException()
        return position++
    }

    internal fun nextPutIndex(nb: Int): Int {                    // package-private
        if (limit - position < nb) throw KBufferOverflowException()
        val p = position
        position += nb
        return p
    }

    /**
     * Checks the given index against the limit, throwing an [ ] if it is not smaller than the limit
     * or is smaller than zero.
     */
    internal fun checkIndex(i: Int): Int {                       // package-private
        if (i < 0 || i >= limit) throw IndexOutOfBoundsException()
        return i
    }

    fun checkIndex(i: Int, nb: Int): Int {               // package-private
        if (i < 0 || nb > limit - i) throw IndexOutOfBoundsException()
        return i
    }

    fun markValue(): Int {                             // package-private
        return mark
    }

    fun truncate() {                             // package-private
        mark = -1
        position = 0
        limit = 0
        capacity = 0
    }

    fun discardMark() {                          // package-private
        mark = -1
    }

    companion object {
        // Cached unsafe-access object
        val UNSAFE: jdk.internal.misc.Unsafe = jdk.internal.misc.Unsafe.getUnsafe()

        /**
         * The characteristics of Spliterators that traverse and split elements
         * maintained in Buffers.
         */
        const val SPLITERATOR_CHARACTERISTICS = Spliterator.SIZED or Spliterator.SUBSIZED or Spliterator.ORDERED

        /**
         * Returns an `IllegalArgumentException` indicating that the source
         * and target are the same `Buffer`.  Intended for use in
         * `put(src)` when the parameter is the `Buffer` on which the
         * method is being invoked.
         *
         * @return  IllegalArgumentException
         * With a message indicating equal source and target buffers
         */
        fun createSameBufferException(): IllegalArgumentException {
            return IllegalArgumentException("The source buffer is this buffer")
        }

        /**
         * Verify that the capacity is nonnegative.
         *
         * @param  capacity
         * The new buffer's capacity, in $type$s
         *
         * @throws  IllegalArgumentException
         * If the `capacity` is a negative integer
         */
        fun createCapacityException(capacity: Int): IllegalArgumentException {
            assert(capacity < 0) { "capacity expected to be negative" }
            return IllegalArgumentException(
                "capacity < 0: ("
                + capacity + " < 0)"
            )
        }

        fun checkBounds(off: Int, len: Int, size: Int) { // package-private
            if (off or len or off + len or size - (off + len) < 0) throw IndexOutOfBoundsException()
        }

        init {
            // setup access to this package in SharedSecrets
            jdk.internal.misc.SharedSecrets.setJavaNioAccess(
                object : jdk.internal.misc.JavaNioAccess() {
                    val directBufferPool: jdk.internal.misc.JavaNioAccess.BufferPool
                        get() = Bits.BUFFER_POOL

                    override fun newDirectByteBuffer(addr: Long, cap: Int, ob: Any): ByteBuffer {
                        return DirectByteBuffer(addr, cap, ob)
                    }

                    fun truncate(buf: Buffer) {
                        buf.truncate()
                    }
                })
        }
    }

    // Creates a new buffer with the given mark, position, limit, and capacity,
    // after checking invariants.
    //
    init {       // package-private
        if (cap < 0) throw createCapacityException(cap)
        capacity = cap
        limit(lim)
        position(pos)
        if (mark >= 0) {
            require(mark <= pos) {
                ("mark > position: ("
                + mark + " > " + pos + ")")
            }
            this.mark = mark
        }
    }
}
