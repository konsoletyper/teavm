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

interface GeneralMatrix {
    val size: Int

    fun get(row: Int, col: Int): Float

    fun determinant(): Float {
        return if (size == 2) {
            get(0, 0) * get(1, 1) - get(1, 0) * get(0, 1)
        } else {
            (0 until size)
                    .map { j ->
                        val r = get(0, j) * exclude(0, j).determinant()
                        if (j % 2 == 0) r else -r
                    }
                    .sum()
        }
    }

    fun exclude(excludeRow: Int, excludeCol: Int): GeneralMatrix {
        val orig = this
        return object : GeneralMatrix {
            override val size: Int
                get() = orig.size - 1

            override fun get(row: Int, col: Int): Float {
                return orig.get(if (row < excludeRow) row else row + 1, if (col < excludeCol) col else col + 1)
            }
        }
    }

    fun asString(): String {
        val cells = (0 until size).map { j ->
            (0 until size).map { i ->
                get(i, j).toString()
            }
        }
        val lengths = cells.map { column -> column.maxOf { it.length } }
        val padCells = (cells zip lengths).map { (column, length) ->
            column.map { it + " ".repeat(length - it.length) }
        }

        return (0 until size).joinToString("\n") { i ->
            (0 until size).joinToString(" ") { j -> padCells[j][i] }
        }
    }
}