/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system

/**
 * Classes that implement this interface are associated with one or more native resources. These resources must be explicitly freed when a class instance is
 * no longer used, by calling the [.free] method.
 *
 *
 * This interface extends [AutoCloseable], which means that implementations may be used as resources in try-with-resources statements.
 */
interface KNativeResource : AutoCloseable {
    /** Frees any native resources held by this object.  */
    fun free()
    override fun close() {
        free()
    }
}