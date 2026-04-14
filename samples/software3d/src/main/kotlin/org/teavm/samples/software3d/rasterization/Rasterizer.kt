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

import org.teavm.samples.software3d.geometry.Vector
import org.teavm.samples.software3d.geometry.length
import kotlin.math.ceil

class Rasterizer(val raster: Raster, val offset: Int, val step: Int) {
    var ambient: Vector = Vector.zero
    var lightPosition: Vector = Vector.zero
    var lightColor: Vector = Vector.zero

    fun drawTriangle(a: VertexParams, b: VertexParams, c: VertexParams) {
        val v1: VertexParams
        val v2: VertexParams
        val v3: VertexParams
        if (a.pos.y < b.pos.y) {
            if (a.pos.y < c.pos.y) {
                v1 = a
                if (b.pos.y < c.pos.y) {
                    v2 = b
                    v3 = c
                } else {
                    v2 = c
                    v3 = b
                }
            } else {
                v1 = c
                v2 = a
                v3 = b
            }
        } else {
            if (b.pos.y < c.pos.y) {
                v1 = b
                if (c.pos.y < a.pos.y) {
                    v2 = c
                    v3 = a
                } else {
                    v2 = a
                    v3 = c
                }
            } else {
                v1 = c
                v2 = b
                v3 = a
            }
        }

        val v3x = if (v2.pos.y == v3.pos.y) {
            v3.pos.x
        } else {
            v1.pos.x + (v2.pos.y - v1.pos.y) * (v3.pos.x - v1.pos.x) / (v3.pos.y - v1.pos.y)
        }
        if (v2.pos.x < v3x) {
            drawTrianglePart(v1, v2, v1, v3, ceil(v1.pos.y).toInt(), ceil(v2.pos.y).toInt())
            drawTrianglePart(v2, v3, v1, v3, ceil(v2.pos.y).toInt(), ceil(v3.pos.y).toInt())
        } else {
            drawTrianglePart(v1, v3, v1, v2, ceil(v1.pos.y).toInt(), ceil(v2.pos.y).toInt())
            drawTrianglePart(v1, v3, v2, v3, ceil(v2.pos.y).toInt(), ceil(v3.pos.y).toInt())
        }
    }

    private fun normalizeY(y: Int): Int = ((y + step - 1 - offset) / step) * step + offset

    private fun drawTrianglePart(
            s1: VertexParams, e1: VertexParams,
            s2: VertexParams, e2: VertexParams,
            sy: Int, ey: Int
    ) {
        val nsy = normalizeY(sy).coerceAtLeast(0)
        val ney = normalizeY(ey).coerceAtMost(raster.height * step)
        if (ney <= 0 || nsy >= raster.height * step) {
            return
        }

        val d1x = e1.pos.x - s1.pos.x
        val d1y = e1.pos.y - s1.pos.y
        val d1z = e1.pos.z - s1.pos.z
        val d2x = e2.pos.x - s2.pos.x
        val d2y = e2.pos.y - s2.pos.y
        val d2z = e2.pos.z - s2.pos.z

        var y = nsy
        while (y < ney) {
            val k1 = if (d1y == 0f) 0f else (y - s1.pos.y) / d1y
            val k2 = if (d2y == 0f) 0f else (y - s2.pos.y) / d2y

            val sx = s1.pos.x + d1x * k1
            val ex = s2.pos.x + d2x * k2
            val startIntX = ceil(sx).toInt().coerceAtLeast(0)
            val endIntX = ceil(ex).toInt().coerceAtMost(raster.width)
            if (startIntX >= endIntX || startIntX >= raster.width || endIntX <= 0) {
                y += step
                continue
            }

            val sz = s1.pos.z + d1z * k1
            val sar = s1.ambient.x + (e1.ambient.x - s1.ambient.x) * k1
            val sag = s1.ambient.y + (e1.ambient.y - s1.ambient.y) * k1
            val sab = s1.ambient.z + (e1.ambient.z - s1.ambient.z) * k1
            val sdr = s1.diffuse.x + (e1.diffuse.x - s1.diffuse.x) * k1
            val sdg = s1.diffuse.y + (e1.diffuse.y - s1.diffuse.y) * k1
            val sdb = s1.diffuse.z + (e1.diffuse.z - s1.diffuse.z) * k1
            val snx = s1.normal.x + (e1.normal.x - s1.normal.x) * k1
            val sny = s1.normal.y + (e1.normal.y - s1.normal.y) * k1
            val snz = s1.normal.z + (e1.normal.z - s1.normal.z) * k1
            val sox = s1.orig.x + (e1.orig.x - s1.orig.x) * k1
            val soy = s1.orig.y + (e1.orig.y - s1.orig.y) * k1
            val soz = s1.orig.z + (e1.orig.z - s1.orig.z) * k1

            val ez = s2.pos.z + d2z * k2
            val ear = s2.ambient.x + (e2.ambient.x - s2.ambient.x) * k2
            val eag = s2.ambient.y + (e2.ambient.y - s2.ambient.y) * k2
            val eab = s2.ambient.z + (e2.ambient.z - s2.ambient.z) * k2
            val edr = s2.diffuse.x + (e2.diffuse.x - s2.diffuse.x) * k2
            val edg = s2.diffuse.y + (e2.diffuse.y - s2.diffuse.y) * k2
            val edb = s2.diffuse.z + (e2.diffuse.z - s2.diffuse.z) * k2
            val enx = s2.normal.x + (e2.normal.x - s2.normal.x) * k2
            val eny = s2.normal.y + (e2.normal.y - s2.normal.y) * k2
            val enz = s2.normal.z + (e2.normal.z - s2.normal.z) * k2
            val eox = s2.orig.x + (e2.orig.x - s2.orig.x) * k2
            val eoy = s2.orig.y + (e2.orig.y - s2.orig.y) * k2
            val eoz = s2.orig.z + (e2.orig.z - s2.orig.z) * k2

            var ptr = raster.pointer(startIntX, y / step)
            var x = startIntX
            while (x < endIntX) {
                val z = sz + (ez - sz) * (x - sx) / (ex - sx)
                if (z > raster.depth[ptr]) {
                    raster.depth[ptr] = z

                    val k = if (sx == ex) 0f else (x - sx) / (ex - sx)
                    val ar = sar + (ear - sar) * k
                    val ag = sag + (eag - sag) * k
                    val ab = sab + (eab - sab) * k
                    val dr = sdr + (edr - sdr) * k
                    val dg = sdg + (edg - sdg) * k
                    val db = sdb + (edb - sdb) * k
                    val nx = snx + (enx - snx) * k
                    val ny = sny + (eny - sny) * k
                    val nz = snz + (enz - snz) * k
                    val ox = sox + (eox - sox) * k
                    val oy = soy + (eoy - soy) * k
                    val oz = soz + (eoz - soz) * k

                    val lightDirX = lightPosition.x - ox
                    val lightDirY = lightPosition.y - oy
                    val lightDirZ = lightPosition.z - oz
                    val lightDirLength = length(lightDirX, lightDirY, lightDirZ)
                    val normalLength = length(nx, ny, nz)
                    var cosAngle = (nx * lightDirX + ny * lightDirY + nz * lightDirZ) / (lightDirLength * normalLength)
                    cosAngle = cosAngle.coerceAtLeast(0f)

                    val r = ar * ambient.x + dr * lightColor.x * cosAngle
                    val g = ag * ambient.y + dg * lightColor.y * cosAngle
                    val b = ab * ambient.z + db * lightColor.z * cosAngle
                    val intR = (r * 255).toInt().coerceIn(0, 255)
                    val intG = (g * 255).toInt().coerceIn(0, 255)
                    val intB = (b * 255).toInt().coerceIn(0, 255)
                    raster.color[ptr] = intB or (intG shl 8) or (intR shl 16) or (255 shl 24)
                }
                ++ptr
                ++x
            }

            y += step
        }
    }
}