/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system.nio

class KByteBuffer {

    private val internalBuffer: ByteArray
    var byteOrder: KByteOrder = KByteOrder.BIG_ENDIAN

    constructor(size: Int) {
        internalBuffer = ByteArray(size)
    }

    constructor(size: Int, init: (Int)->Byte) {
        internalBuffer = ByteArray(size, init)
    }

    enum class KByteOrder {
        LITTLE_ENDIAN,
        BIG_ENDIAN
    }
}