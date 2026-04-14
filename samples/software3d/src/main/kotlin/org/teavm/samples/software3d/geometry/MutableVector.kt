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

package org.teavm.samples.software3d.geometry

import kotlin.jvm.JvmField

class MutableVector {
    @JvmField
    var x: Float = 0f
    @JvmField
    var y: Float = 0f
    @JvmField
    var z: Float = 0f
    @JvmField
    var w: Float = 0f

    fun set(vector: Vector) {
        x = vector.x
        y = vector.y
        z = vector.z
        w = vector.w
    }

    fun normalizeW() {
        x /= w
        y /= w
        z /= w
        w = 1f
    }
}