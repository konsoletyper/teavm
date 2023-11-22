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

package org.teavm.samples.software3d.teavm

import org.teavm.interop.Address
import org.teavm.interop.Export
import org.teavm.interop.Import
import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.rasterization.Raster
import org.teavm.samples.software3d.rendering.Renderer
import org.teavm.samples.software3d.scenes.geometry

fun main() {
    println("Worker started")
}

@Export(name = "initWorker")
fun init(width: Int, height: Int, step: Int, offset: Int) {
    val (scene, updaterF) = geometry()
    raster = Raster(width, Raster.calculateHeight(height, step, offset))
    updater = updaterF
    renderer = Renderer(scene, raster, offset, step).apply {
        projection = Matrix.projection(-1f, 1f, -1f, 1f, 2f, 10f)
        viewport = Matrix.translation(width / 2f, height / 2f, 0f) *
                Matrix.scale(width / 2f, width / 2f, 1f)
    }
    println("Worker initialized")
}

@Export(name = "renderFrame")
fun renderFrame(time: Double) {
    val perfStart = System.nanoTime()
    updater(time)
    renderer.render()
    val perfEnd = System.nanoTime()
    val buffer = raster.color
    sendRenderResult(Address.ofData(buffer), buffer.size * 4, (perfEnd - perfStart).toInt())
}

@Import(module = "renderer", name = "result")
external fun sendRenderResult(data: Address, dataSize: Int, time: Int)

private lateinit var raster: Raster
private lateinit var renderer: Renderer
private lateinit var updater: (Double) -> Unit
