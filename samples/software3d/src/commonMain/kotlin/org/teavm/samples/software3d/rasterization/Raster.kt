/*
 *  Copyright 2023 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.samples.software3d.rasterization

import kotlin.jvm.JvmField

class Raster(@JvmField val width: Int, @JvmField val height: Int) {
    @JvmField var color: IntArray = IntArray(width * height)
    @JvmField val depth: FloatArray = FloatArray(width * height)

    fun pointer(x: Int, y: Int): Int = y * width + x

    fun clear() {
        color.fill(255 shl 24)
        depth.fill(0f)
    }

    fun flip(): IntArray {
        val result = color
        color = IntArray(width * height)
        return result
    }

    companion object {
        fun calculateHeight(height: Int, step: Int, offset: Int): Int = ((height + step - 2 - offset) / step) + 1
    }
}