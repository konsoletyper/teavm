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

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Matrix(
        val m11: Float, val m12: Float, val m13: Float, val m14: Float,
        val m21: Float, val m22: Float, val m23: Float, val m24: Float,
        val m31: Float, val m32: Float, val m33: Float, val m34: Float,
        val m41: Float, val m42: Float, val m43: Float, val m44: Float
) : GeneralMatrix {
    override val size: Int
        get() = 4

    override fun get(row: Int, col: Int): Float {
        val offset = row * 4 + col
        return when (offset) {
            0 -> m11
            1 -> m12
            2 -> m13
            3 -> m14
            4 -> m21
            5 -> m22
            6 -> m23
            7 -> m24
            8 -> m31
            9 -> m32
            10 -> m33
            11 -> m34
            12 -> m41
            13 -> m42
            14 -> m43
            15 -> m44
            else -> error("Wrong row/column")
        }
    }

    fun applyTo(v: Vector, result: MutableVector) {
        with(result) {
            x = m11 * v.x + m12 * v.y + m13 * v.z + m14 * v.w
            y = m21 * v.x + m22 * v.y + m23 * v.z + m24 * v.w
            z = m31 * v.x + m32 * v.y + m33 * v.z + m34 * v.w
            w = m41 * v.x + m42 * v.y + m43 * v.z + m44 * v.w
        }
    }


    fun applyDirTo(v: Vector, result: MutableVector) {
        with(result) {
            x = m11 * v.x + m12 * v.y + m13 * v.z
            y = m21 * v.x + m22 * v.y + m23 * v.z
            z = m31 * v.x + m32 * v.y + m33 * v.z
            w = m41 * v.x + m42 * v.y + m43 * v.z
        }
    }

    operator fun times(v: Vector): Vector = Vector(
            x = m11 * v.x + m12 * v.y + m13 * v.z + m14 * v.w,
            y = m21 * v.x + m22 * v.y + m23 * v.z + m24 * v.w,
            z = m31 * v.x + m32 * v.y + m33 * v.z + m34 * v.w,
            w = m41 * v.x + m42 * v.y + m43 * v.z + m44 * v.w,
    )

    operator fun times(m: Matrix): Matrix = Matrix(
            m11 = m11 * m.m11 + m12 * m.m21 + m13 * m.m31 + m14 * m.m41,
            m12 = m11 * m.m12 + m12 * m.m22 + m13 * m.m32 + m14 * m.m42,
            m13 = m11 * m.m13 + m12 * m.m23 + m13 * m.m33 + m14 * m.m43,
            m14 = m11 * m.m14 + m12 * m.m24 + m13 * m.m34 + m14 * m.m44,
            m21 = m21 * m.m11 + m22 * m.m21 + m23 * m.m31 + m24 * m.m41,
            m22 = m21 * m.m12 + m22 * m.m22 + m23 * m.m32 + m24 * m.m42,
            m23 = m21 * m.m13 + m22 * m.m23 + m23 * m.m33 + m24 * m.m43,
            m24 = m21 * m.m14 + m22 * m.m24 + m23 * m.m34 + m24 * m.m44,
            m31 = m31 * m.m11 + m32 * m.m21 + m33 * m.m31 + m34 * m.m41,
            m32 = m31 * m.m12 + m32 * m.m22 + m33 * m.m32 + m34 * m.m42,
            m33 = m31 * m.m13 + m32 * m.m23 + m33 * m.m33 + m34 * m.m43,
            m34 = m31 * m.m14 + m32 * m.m24 + m33 * m.m34 + m34 * m.m44,
            m41 = m41 * m.m11 + m42 * m.m21 + m43 * m.m31 + m44 * m.m41,
            m42 = m41 * m.m12 + m42 * m.m22 + m43 * m.m32 + m44 * m.m42,
            m43 = m41 * m.m13 + m42 * m.m23 + m43 * m.m33 + m44 * m.m43,
            m44 = m41 * m.m14 + m42 * m.m24 + m43 * m.m34 + m44 * m.m44
    )

    fun transpose(): Matrix = Matrix(
            m11 = m11, m12 = m21, m13 = m31, m14 = m41,
            m21 = m12, m22 = m22, m23 = m32, m24 = m42,
            m31 = m13, m32 = m23, m33 = m33, m34 = m43,
            m41 = m14, m42 = m24, m43 = m34, m44 = m44
    )

    fun inverse(): Matrix {
        val transposed = transpose()
        val determinant = determinant()
        return generate { i, j ->
            val minor = transposed.exclude(i, j).determinant()
            val adj = if ((i + j) % 2 == 0) minor else -minor
            adj / determinant
        }
    }

    companion object {
        fun projection(
                left: Float,
                right: Float,
                bottom: Float,
                top: Float,
                near: Float,
                far: Float
        ): Matrix = Matrix(
                m11 = 2 * near / (right - left),
                m12 = 0f,
                m13 = (right + left) / (right - left),
                m14 = 0f,
                m21 = 0f,
                m22 = 2 * near / (top - bottom),
                m23 = (top + bottom) / (top - bottom),
                m24 = 0f,
                m31 = 0f,
                m32 = 0f,
                m33 = -(far + near) / (far - near),
                m34 = -2 * far * near / (far - near),
                m41 = 0f,
                m42 = 0f,
                m43 = -1f,
                m44 = 0f
        )

        fun rotation(x: Float, y: Float, z: Float, angle: Float): Matrix {
            val length = length(x, y, z)
            val nx = x / length
            val ny = y / length
            val nz = z / length
            val c = cos(angle)
            val s = sin(angle)
            return Matrix(
                    m11 = (1 - c) * nx * nx + c,
                    m12 = (1 - c) * nx * ny - s * nz,
                    m13 = (1 - c) * nx * nz + s * ny,
                    m14 = 0f,
                    m21 = (1 - c) * nx * ny + s * nz,
                    m22 = (1 - c) * ny * ny + c,
                    m23 = (1 - c) * ny * nz - s * nx,
                    m24 = 0f,
                    m31 = (1 - c) * nx * nz - s * ny,
                    m32 = (1 - c) * ny * nz + s * nx,
                    m33 = (1 - c) * nz * nz + c,
                    m34 = 0f,
                    m41 = 0f,
                    m42 = 0f,
                    m43 = 0f,
                    m44 = 1f
            )
        }

        fun translation(x: Float, y: Float, z: Float): Matrix = Matrix(
                m11 = 1f, m12 = 0f, m13 = 0f, m14 = x,
                m21 = 0f, m22 = 1f, m23 = 0f, m24 = y,
                m31 = 0f, m32 = 0f, m33 = 1f, m34 = z,
                m41 = 0f, m42 = 0f, m43 = 0f, m44 = 1f
        )

        fun scale(x: Float, y: Float, z: Float): Matrix = Matrix(
                m11 = x,  m12 = 0f, m13 = 0f, m14 = 0f,
                m21 = 0f, m22 = y,  m23 = 0f, m24 = 0f,
                m31 = 0f, m32 = 0f, m33 = z,  m34 = 0f,
                m41 = 0f, m42 = 0f, m43 = 0f, m44 = 1f
        )

        fun lookAt(dirX: Float, dirY: Float, dirZ: Float): Matrix {
            val forward = Vector(dirX, dirY, dirZ, 0f).norm()
            val left = (Vector(0f, 1f, 0f, 0f) cross forward).norm()
            val up = forward cross left
            return Matrix(
                    m11 = left.x, m12 = up.x, m13 = forward.x, m14 = 0f,
                    m21 = left.y, m22 = up.y, m23 = forward.y, m24 = 0f,
                    m31 = left.z, m32 = up.z, m33 = forward.z, m34 = 0f,
                    m41 = 0f,     m42 = 0f,   m43 = 0f,        m44 = 1f
            )
        }

        fun lookAt(x: Float, y: Float, z: Float, dirX: Float, dirY: Float, dirZ: Float): Matrix {
            return translation(x, y, z) * lookAt(dirX, dirY, dirZ)
        }

        fun generate(generator: (Int, Int) -> Float): Matrix = Matrix(
                m11 = generator(0, 0), m12 = generator(0, 1), m13 = generator(0, 2), m14 = generator(0, 3),
                m21 = generator(1, 0), m22 = generator(1, 1), m23 = generator(1, 2), m24 = generator(1, 3),
                m31 = generator(2, 0), m32 = generator(2, 1), m33 = generator(2, 2), m34 = generator(2, 3),
                m41 = generator(3, 0), m42 = generator(3, 1), m43 = generator(3, 2), m44 = generator(3, 3)
        )

        val identity: Matrix = Matrix(
                m11 = 1f, m12 = 0f, m13 = 0f, m14 = 0f,
                m21 = 0f, m22 = 1f, m23 = 0f, m24 = 0f,
                m31 = 0f, m32 = 0f, m33 = 1f, m34 = 0f,
                m41 = 0f, m42 = 0f, m43 = 0f, m44 = 1f
        )
    }
}