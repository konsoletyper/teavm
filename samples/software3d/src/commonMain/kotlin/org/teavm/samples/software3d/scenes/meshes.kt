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

package org.teavm.samples.software3d.scenes

import org.teavm.samples.software3d.geometry.Face
import org.teavm.samples.software3d.geometry.Vector
import org.teavm.samples.software3d.scene.Mesh
import org.teavm.samples.software3d.scene.Vertex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Meshes {
    fun cube(): Mesh {
        val positions = listOf(
                Vector(-1f, -1f, -1f, 1f),
                Vector(1f, -1f, -1f, 1f),
                Vector(1f, 1f, -1f, 1f),
                Vector(-1f, 1f, -1f, 1f),
                Vector(-1f, -1f, 1f, 1f),
                Vector(1f, -1f, 1f, 1f),
                Vector(1f, 1f, 1f, 1f),
                Vector(-1f, 1f, 1f, 1f)
        )
        val red = Vector(1f, 0f, 0f, 1f)
        val green = Vector(0f, 1f, 0f, 1f)
        val blue = Vector(0f, 0f, 1f, 1f)
        val yellow = Vector(1f, 1f, 0f, 1f)
        val cyan = Vector(0f, 1f, 1f, 1f)
        val magenta = Vector(1f, 0f, 1f, 1f)
        return Mesh(listOf(
                Face(
                        Vertex(positions[0], -Vector.axisZ, red, red),
                        Vertex(positions[1], -Vector.axisZ, red, red),
                        Vertex(positions[2], -Vector.axisZ, red, red)
                ),
                Face(
                        Vertex(positions[0], -Vector.axisZ, red, red),
                        Vertex(positions[2], -Vector.axisZ, red, red),
                        Vertex(positions[3], -Vector.axisZ, red, red)
                ),

                Face(
                        Vertex(positions[4], Vector.axisZ, cyan, cyan),
                        Vertex(positions[5], Vector.axisZ, cyan, cyan),
                        Vertex(positions[6], Vector.axisZ, cyan, cyan)
                ),
                Face(
                        Vertex(positions[4], Vector.axisZ, cyan, cyan),
                        Vertex(positions[7], Vector.axisZ, cyan, cyan),
                        Vertex(positions[6], Vector.axisZ, cyan, cyan)
                ),

                Face(
                        Vertex(positions[0], -Vector.axisY, yellow, yellow),
                        Vertex(positions[1], -Vector.axisY, yellow, yellow),
                        Vertex(positions[5], -Vector.axisY, yellow, yellow)
                ),
                Face(
                        Vertex(positions[0], -Vector.axisY, yellow, yellow),
                        Vertex(positions[4], -Vector.axisY, yellow, yellow),
                        Vertex(positions[5], -Vector.axisY, yellow, yellow)
                ),

                Face(
                        Vertex(positions[2], Vector.axisY, blue, blue),
                        Vertex(positions[3], Vector.axisY, blue, blue),
                        Vertex(positions[7], Vector.axisY, blue, blue)
                ),
                Face(
                        Vertex(positions[2], Vector.axisY, blue, blue),
                        Vertex(positions[6], Vector.axisY, blue, blue),
                        Vertex(positions[7], Vector.axisY, blue, blue)
                ),

                Face(
                        Vertex(positions[0], -Vector.axisX, green, green),
                        Vertex(positions[3], -Vector.axisX, green, green),
                        Vertex(positions[4], -Vector.axisX, green, green)
                ),
                Face(
                        Vertex(positions[3], -Vector.axisX, green, green),
                        Vertex(positions[4], -Vector.axisX, green, green),
                        Vertex(positions[7], -Vector.axisX, green, green)
                ),

                Face(
                        Vertex(positions[1], Vector.axisX, magenta, magenta),
                        Vertex(positions[2], Vector.axisX, magenta, magenta),
                        Vertex(positions[5], Vector.axisX, magenta, magenta)
                ),
                Face(
                        Vertex(positions[2], Vector.axisX, magenta, magenta),
                        Vertex(positions[5], Vector.axisX, magenta, magenta),
                        Vertex(positions[6], Vector.axisX, magenta, magenta)
                )
        ))
    }

    fun sphere(): Mesh {
        val count = 5
        val rows = count * 4
        val columns = count * 4
        val color = Vector(1f, 1f, 1f, 1f)
        val vertices = (0 until rows).map { i ->
            val latitude = i * PI / 2 / count
            val latX = cos(latitude)
            val latY = sin(latitude)
            (0 until columns).map { j ->
                val longitude = j * PI / 2 / count
                val longX = cos(longitude)
                val longY = sin(longitude)
                val pos = Vector(
                        x = (longX * latX).toFloat(),
                        z = (longY * latX).toFloat(),
                        y = latY.toFloat(),
                        w = 1f
                )
                Vertex(
                        position = pos,
                        normal = pos,
                        ambient = color,
                        diffuse = color
                )
            }
        }
        val faces = (0 until rows).flatMap { row ->
            (0 until columns).flatMap { col ->
                val a = vertices[row][col]
                val b = vertices[(row + 1) % rows][col]
                val c = vertices[(row + 1) % rows][(col + 1) % columns]
                val d = vertices[row][(col + 1) % columns]
                listOf(
                        Face(a, b, c),
                        Face(a, c, d)
                )
            }
        }
        return Mesh(faces)
    }
}