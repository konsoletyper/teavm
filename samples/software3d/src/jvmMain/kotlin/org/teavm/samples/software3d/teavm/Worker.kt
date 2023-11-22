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

import org.teavm.jso.JSObject
import org.teavm.jso.browser.Window
import org.teavm.jso.core.*
import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.rasterization.Raster
import org.teavm.samples.software3d.rendering.Renderer
import org.teavm.samples.software3d.scenes.geometry

fun worker() {
    val worker = RenderWorker()
    Window.worker().listenMessage {
        val dataJson = it.data as JSMapLike<*>
        when ((dataJson["type"] as JSString).stringValue()) {
            "init" -> worker.init(dataJson)
            "frame" -> worker.renderFrame(dataJson)
            "stop" -> Window.worker().close()
        }
    }
}

class RenderWorker {
    private lateinit var raster: Raster
    private lateinit var renderer: Renderer
    private lateinit var updater: (Double) -> Unit
    private var width: Int = 0
    private var height: Int = 0

    fun init(params: JSMapLike<*>) {
        val (scene, updaterF) = geometry()
        width = (params["width"] as JSNumber).intValue()
        height = (params["height"] as JSNumber).intValue()
        val step = (params["step"] as JSNumber).intValue()
        val offset = (params["offset"] as JSNumber).intValue()
        raster = Raster(width, Raster.calculateHeight(height, step, offset))
        updater = updaterF
        renderer = Renderer(scene, raster, offset, step).apply {
            projection = Matrix.projection(-1f, 1f, -1f, 1f, 2f, 10f)
            viewport = Matrix.translation(width / 2f, height / 2f, 0f) *
                    Matrix.scale(width / 2f, width / 2f, 1f)
        }
    }

    fun renderFrame(params: JSMapLike<*>) {
        val time = (params["time"] as JSNumber).doubleValue()
        val perfStart = System.nanoTime()
        updater(time)
        renderer.render()
        val perfEnd = System.nanoTime()
        val buffer = extractBuffer(raster.flip())
        Window.worker().postMessage(JSObjects.createWithoutProto<JSMapLike<JSObject>>().apply {
            set("data", buffer)
            set("time", JSNumber.valueOf((perfEnd - perfStart).toInt()))
        }, JSArray.of(buffer))
    }
}