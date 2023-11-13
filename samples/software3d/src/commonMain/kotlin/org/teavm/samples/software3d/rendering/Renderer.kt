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

package org.teavm.samples.software3d.rendering

import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.rasterization.Raster
import org.teavm.samples.software3d.rasterization.Rasterizer
import org.teavm.samples.software3d.rasterization.VertexParams
import org.teavm.samples.software3d.scene.Item
import org.teavm.samples.software3d.scene.Scene

class Renderer(val scene: Scene, val raster: Raster, offset: Int, step: Int) {
    private val rasterizer = Rasterizer(raster, offset, step)

    var projection: Matrix = Matrix.identity
    var viewport: Matrix = Matrix.identity
    private var transform: Matrix = Matrix.identity
    private var v1 = VertexParams()
    private var v2 = VertexParams()
    private var v3 = VertexParams()

    fun render() {
        transform = viewport * projection * scene.camera
        raster.clear()
        rasterizer.lightPosition = scene.camera * scene.lightPosition
        rasterizer.lightColor = scene.lightColor
        rasterizer.ambient = scene.ambientColor
        for (item in scene.items) {
            renderItem(item)
        }
    }

    private fun renderItem(item: Item) {
        val itemTransform = scene.camera * item.transform
        val viewItemTransform = transform * item.transform
        for (face in item.mesh.faces) {
            itemTransform.applyTo(face.a.position, v1.orig)
            itemTransform.applyTo(face.b.position, v2.orig)
            itemTransform.applyTo(face.c.position, v3.orig)

            viewItemTransform.applyTo(face.a.position, v1.pos)
            viewItemTransform.applyTo(face.b.position, v2.pos)
            viewItemTransform.applyTo(face.c.position, v3.pos)

            itemTransform.applyDirTo(face.a.normal, v1.normal)
            itemTransform.applyDirTo(face.b.normal, v2.normal)
            itemTransform.applyDirTo(face.c.normal, v3.normal)

            v1.diffuse = face.a.diffuse
            v1.ambient = face.a.ambient
            v2.diffuse = face.b.diffuse
            v2.ambient = face.b.ambient
            v3.diffuse = face.c.diffuse
            v3.ambient = face.c.ambient

            v1.pos.normalizeW()
            v2.pos.normalizeW()
            v3.pos.normalizeW()

            rasterizer.drawTriangle(v1, v2, v3)
        }
    }
}