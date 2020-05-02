/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

import org.lwjgl.system.nio.*
import java.util.*
import kotlin.reflect.*

/** Base class of all struct implementations.  */
abstract class KStruct protected constructor(address: Long, private val container: ByteArray) : KPointer.Default(address) {
    companion object {
        protected val DEFAULT_PACK_ALIGNMENT = if (Platform.get() === Platform.WINDOWS) 8 else 0x40000000
        protected const val DEFAULT_ALIGN_AS = 0
        private var CONTAINER: Long = 0

        // ---------------- Implementation utilities ----------------
        fun <T : KStruct> wrap(clazz: KClass<T>, address: Long): T {
            val struct: T
            struct = try {
                UNSAFE.allocateInstance(clazz.java) as T
            } catch (e: InstantiationException) {
                throw UnsupportedOperationException(e)
            }
            UNSAFE.putLong(struct, ADDRESS, address)
            return struct
        }

        fun <T : KStruct> wrap(clazz: KClass<T>, address: Long, container: KByteBuffer?): T {
            val struct: T
            struct = try {
                UNSAFE.allocateInstance(clazz.java) as T
            } catch (e: InstantiationException) {
                throw UnsupportedOperationException(e)
            }
            UNSAFE.putLong(struct, ADDRESS, address)
            UNSAFE.putObject(struct, CONTAINER, container)
            return struct
        }

        fun __checkContainer(container: KByteBuffer, sizeof: Int): KByteBuffer {
            if (Checks.CHECKS) {
                Checks.check(container, sizeof)
            }
            return container
        }

        private fun getBytes(elements: Int, elementSize: Int): Long {
            return ((elements.toLong() and 0xFFFFFFFFL) * elementSize)
        }

        fun __checkMalloc(elements: Int, elementSize: Int): Long {
            val bytes = ((elements.toLong() and 0xFFFFFFFFL) * elementSize)
            if (Checks.DEBUG) {
                require(elements >= 0) { "Invalid number of elements" }
                require(!(Pointer.BITS32 && 0xFFFFFFFFL < bytes)) { "The request allocation is too large" }
            }
            return bytes
        }

        fun __create(elements: Int, elementSize: Int): ByteBuffer {
            APIUtil.apiCheckAllocation(elements, getBytes(elements, elementSize), 0x7FFFFFFFL)
            return ByteBuffer.allocateDirect(elements * elementSize).order(ByteOrder.nativeOrder())
        }

        fun __padding(size: Int, condition: Boolean): Member {
            return __member(if (condition) size else 0, 1)
        }

        fun __member(size: Int, alignment: Int = size, forceAlignment: Boolean = false): Member {
            return Member(size, alignment, forceAlignment)
        }

        fun __array(size: Int, length: Int): Member {
            return __array(size, size, length)
        }

        fun __array(size: Int, alignment: Int, length: Int): Member {
            return Member(size * length, alignment, false)
        }

        fun __array(size: Int, alignment: Int, forceAlignment: Boolean, length: Int): Member {
            return Member(size * length, alignment, forceAlignment)
        }

        fun __union(vararg members: Member): Layout {
            return __union(DEFAULT_PACK_ALIGNMENT, DEFAULT_ALIGN_AS, *members)
        }

        fun __union(
            packAlignment: Int,
            alignas: Int,
            vararg members: Member
        ): Layout {
            val union: MutableList<Member> = ArrayList(members.size)
            var size = 0
            var alignment = alignas
            for (m in members) {
                size = Math.max(size, m.size)
                alignment = Math.max(alignment, m.getAlignment(packAlignment))
                m.offset = 0
                union.add(m)
                if (m is Layout) {
                    addNestedMembers(m, union, 0)
                }
            }
            return Layout(size, alignment, alignas != 0, union.toTypedArray())
        }

        fun __struct(vararg members: Member): Layout {
            return __struct(DEFAULT_PACK_ALIGNMENT, DEFAULT_ALIGN_AS, *members)
        }

        fun __struct(
            packAlignment: Int,
            alignas: Int,
            vararg members: Member
        ): Layout {
            val struct: MutableList<Member> = ArrayList(members.size)
            var size = 0
            var alignment = alignas
            for (m in members) {
                val memberAlignment = m.getAlignment(packAlignment)
                m.offset = align(size, memberAlignment)
                size = m.offset + m.size
                alignment = Math.max(alignment, memberAlignment)
                struct.add(m)
                if (m is Layout) {
                    addNestedMembers(m, struct, m.offset)
                }
            }

            // tail padding
            size = align(size, alignment)
            return Layout(size, alignment, alignas != 0, struct.toTypedArray())
        }

        private fun addNestedMembers(
            nested: Member,
            members: MutableList<Member>,
            offset: Int
        ) {
            val layout = nested as Layout
            for (m in layout.members) {
                m.offset += offset
                members.add(m)
            }
        }

        private fun align(offset: Int, alignment: Int): Int {
            return (offset - 1 or alignment - 1) + 1
        }

        init {
            Library.initialize()
            CONTAINER = try {
                UNSAFE.objectFieldOffset(KStruct::class.java.getDeclaredField("container"))
            } catch (t: Throwable) {
                throw UnsupportedOperationException(t)
            }
        }
    }

    /** Returns `sizeof(struct)`.  */
    abstract fun sizeof(): Int

    /** Zeroes-out the struct data.  */
    fun clear() {
        MemoryUtil.memSet(address(), 0, sizeof().toLong())
    }

    /**
     * Frees the struct allocation.
     *
     *
     * This method should not be used if the memory backing this struct is not owned by the struct.
     */
    fun free() {
        MemoryUtil.nmemFree(address())
    }

    /**
     * Returns true if the pointer member that corresponds to the specified `memberOffset` is `NULL`.
     *
     *
     * This is useful to verify that not nullable members of an untrusted struct instance are indeed not `NULL`.
     *
     * @param memberOffset the byte offset of the member to query
     *
     * @return true if the member is `NULL`
     */
    fun isNull(memberOffset: Int): Boolean {
        if (Checks.DEBUG) {
            checkMemberOffset(memberOffset)
        }
        return MemoryUtil.memGetAddress(address() + memberOffset) == MemoryUtil.NULL
    }

    fun <T : KStruct?> wrap(address: Long, index: Int, container: ByteBuffer?): T {
        val struct: T
        struct = try {
            UNSAFE.allocateInstance(this.javaClass) as T
        } catch (e: InstantiationException) {
            throw UnsupportedOperationException(e)
        }
        UNSAFE.putLong(
            struct,
            ADDRESS,
            address + Integer.toUnsignedLong(index) * sizeof()
        )
        UNSAFE.putObject(struct, CONTAINER, container)
        return struct
    }

    private fun checkMemberOffset(memberOffset: Int) {
        require(!(memberOffset < 0 || sizeof() - memberOffset < Pointer.POINTER_SIZE)) { "Invalid member offset." }
    }

    // ---------------- Struct Member Layout ----------------
    open class Member internal constructor(val size: Int, val alignment: Int, val forcedAlignment: Boolean) {
        var offset = 0

        fun getAlignment(packAlignment: Int): Int {
            return if (forcedAlignment) alignment else Math.min(alignment, packAlignment)
        }

    }

    class Layout internal constructor(
        size: Int,
        alignment: Int,
        forceAlignment: Boolean,
        val members: Array<Member>
    ) : Member(size, alignment, forceAlignment) {
        fun offsetof(member: Int): Int {
            return members[member].offset
        }

    }

}