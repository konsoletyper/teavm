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
import kotlin.math.sqrt

class Vector(
        @JvmField val x: Float,
        @JvmField val y: Float,
        @JvmField val z: Float,
        @JvmField val w: Float
) {
    operator fun unaryMinus(): Vector {
        return Vector(-x, -y, -z, -w)
    }

    override fun toString(): String = "$x, $y, $z, $w"

    infix fun cross(other: Vector): Vector = Vector(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x,
            w = 0f
    )

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun norm(): Vector {
        val l = length()
        return Vector(x / l, y / l, z / l, w / l)
    }

    companion object {
        val zero: Vector = Vector(0f, 0f, 0f, 1f)
        val axisX: Vector = Vector(1f, 0f, 0f, 1f)
        val axisY: Vector = Vector(0f, 1f, 0f, 1f)
        val axisZ: Vector = Vector(0f, 0f, 1f, 1f)
    }
}