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

package org.teavm.samples.software3d.kjs

import org.khronos.webgl.Int32Array
import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.rasterization.Raster
import org.teavm.samples.software3d.rendering.Renderer
import org.teavm.samples.software3d.scenes.geometry
import org.w3c.dom.DedicatedWorkerGlobalScope

fun main() {
    self.onmessage = { event ->
        val data = event.data.asDynamic()
        when (data.type as String) {
            "init" -> init(data)
            "frame" -> renderFrame(data)
            "stop" -> self.close()
        }

        Unit
    }
}

private fun init(data: dynamic) {
    val width = data.width as Int
    val height = data.height as Int
    val step = data.step as Int
    val offset = data.offset as Int
    val (scene, updaterF) = geometry()
    raster = Raster(width, Raster.calculateHeight(height, step, offset))
    updater = updaterF
    renderer = Renderer(scene, raster, offset, step).apply {
        projection = Matrix.projection(-1f, 1f, -1f, 1f, 2f, 10f)
        viewport = Matrix.translation(width / 2f, height / 2f, 0f) *
                Matrix.scale(width / 2f, width / 2f, 1f)
    }
}

private fun renderFrame(data: dynamic) {
    val time = data.time as Double
    val perfStart = self.performance.now()
    updater(time)
    renderer.render()
    val perfEnd = self.performance.now()
    val typedArray = raster.flip().asDynamic() as Int32Array
    val message = Any().asDynamic()
    message.data = typedArray.buffer
    message.time = ((perfEnd - perfStart) * 1000000).toInt()
    self.postMessage(message, arrayOf(typedArray.buffer))
}

private val self = js("self") as DedicatedWorkerGlobalScope

private lateinit var raster: Raster
private lateinit var renderer: Renderer
private lateinit var updater: (Double) -> Unit