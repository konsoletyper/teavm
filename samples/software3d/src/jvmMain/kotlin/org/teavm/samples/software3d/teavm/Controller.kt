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
import org.teavm.jso.canvas.CanvasRenderingContext2D
import org.teavm.jso.canvas.ImageData
import org.teavm.jso.core.JSMapLike
import org.teavm.jso.core.JSNumber
import org.teavm.jso.core.JSObjects
import org.teavm.jso.core.JSString
import org.teavm.jso.dom.html.HTMLCanvasElement
import org.teavm.jso.dom.html.HTMLDocument
import org.teavm.jso.typedarrays.ArrayBuffer
import org.teavm.jso.typedarrays.Uint8ClampedArray
import org.teavm.jso.workers.Worker
import org.teavm.samples.software3d.util.PerformanceMeasure

class Controller(
        private val width: Int,
        private val height: Int,
        private val onPerformance: (Int, Long) -> Unit,
) {
    private val canvas = HTMLDocument.current().getElementById("canvas") as HTMLCanvasElement
    private val context = canvas.getContext("2d") as CanvasRenderingContext2D
    private val startTime = System.currentTimeMillis()
    private var onRenderComplete: (Int, ArrayBuffer) -> Unit = { _, _ -> }
    private var workers: List<Worker> = emptyList()
    private var performanceMeasureByWorker: List<PerformanceMeasure> = emptyList()
    private val performanceMeasure = PerformanceMeasure(100000) { onPerformance(-1, it) }

    val tasks: Int get() = workers.size

    fun startRendering(tasks: Int, workerType: WorkerType) {
        performanceMeasure.reset()
        stopRendering()
        val scriptName = when (workerType) {
            WorkerType.JS -> "js-worker.js"
            WorkerType.WEBASSEMBLY -> "wasm-worker.js"
            WorkerType.KOTLIN_JS -> "kjs/software3d.js"
        }
        workers = (0 until tasks).map { index ->
            Worker.create(scriptName).apply {
                postMessage(JSObjects.createWithoutProto<JSMapLike<JSObject>>().apply {
                    set("type", JSString.valueOf("init"))
                    set("width", JSNumber.valueOf(width))
                    set("height", JSNumber.valueOf(height))
                    set("step", JSNumber.valueOf(tasks))
                    set("offset", JSNumber.valueOf(index))
                })
                onMessage { event ->
                    if (index < workers.size && workers[index] == event.target) {
                        val data = event.data as JSMapLike<*>
                        val buffer = data["data"] as ArrayBuffer
                        onRenderComplete(index, buffer)
                        performanceMeasureByWorker[index].reportFrame((data["time"] as JSNumber).intValue().toLong())
                    }
                }
            }
        }
        performanceMeasureByWorker = (0 until tasks).map { index ->
            PerformanceMeasure(100000) { onPerformance(index, it) }
        }
        renderFrame()
    }

    private fun stopRendering() {
        workers.forEach {
            it.postMessage(JSObjects.createWithoutProto<JSMapLike<JSObject>>().apply {
                set("type", JSString.valueOf("stop"))
            })
        }
        workers = emptyList()
    }

    private fun renderFrame() {
        val currenTime = System.currentTimeMillis()
        val frameTime = (currenTime - startTime) / 1000.0
        var pending = workers.size
        val buffers = arrayOfNulls<ArrayBuffer>(workers.size)
        performanceMeasure.startFrame()

        for (worker in workers) {
            worker.postMessage(JSObjects.createWithoutProto<JSMapLike<JSObject>>().apply {
                set("type", JSString.valueOf("frame"))
                set("time", JSNumber.valueOf(frameTime))
            })
        }
        onRenderComplete = { index, buffer ->
            if (buffers[index] == null) {
                buffers[index] = buffer
                if (--pending == 0) {
                    performanceMeasure.endFrame()
                    Window.requestAnimationFrame {
                        displayBuffers(buffers)
                        renderFrame()
                    }
                }
            }
        }
    }

    private fun displayBuffers(buffers: Array<out ArrayBuffer?>) {
        for (y in 0 until height) {
            val buffer = buffers[y % buffers.size]!!
            val array = Uint8ClampedArray.create(buffer, width * 4 * (y / buffers.size), width * 4)
            val imageData = ImageData.create(array, width, 1)
            context.putImageData(imageData, 0.0, y.toDouble())
        }
    }
}
