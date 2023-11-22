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

package org.teavm.samples.software3d.swing

import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.rasterization.Raster
import org.teavm.samples.software3d.rendering.Renderer
import org.teavm.samples.software3d.scenes.geometry
import java.awt.*
import java.awt.image.BufferedImage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.WindowConstants

private const val SCENE_WIDTH = 1024
private const val SCENE_HEIGHT = 768

fun main() {
    val image = BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
    val imageComponent = ImageComponent(image)

    val window = JFrame().apply {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        add(imageComponent)
        pack()
        isVisible = true
    }

    EventQueue.invokeAndWait {
        window.pack()
    }

    val (scene, updater) = geometry()
    val taskCount = 1// Runtime.getRuntime().availableProcessors()
    println("Running on $taskCount CPUs")
    val rasters = (0 until taskCount).map { Raster(SCENE_WIDTH, (SCENE_HEIGHT + taskCount - 1) / taskCount) }
    val renderers = rasters.mapIndexed { index, raster ->
        Renderer(scene, raster, index, taskCount).apply {
            projection = Matrix.projection(-1f, 1f, -1f, 1f, 2f, 10f)
            viewport = Matrix.translation(SCENE_WIDTH / 2f, SCENE_HEIGHT / 2f, 0f) *
                    Matrix.scale(SCENE_WIDTH / 2f, SCENE_WIDTH / 2f, 1f)
        }
    }

    val queues = renderers.map { renderer ->
        val queue = LinkedBlockingQueue<CountDownLatch>()
        Thread {
            while (true) {
                val latch = queue.take()
                renderer.render()
                latch.countDown()
            }
        }.apply {
            isDaemon = true
            start()
        }
        queue
    }

    val startTime = System.currentTimeMillis()
    var totalTime = 0L
    var totalTimeSec = 0L
    var frames = 1
    while (true) {
        val frameStart = System.currentTimeMillis()
        val frameStartPerf = System.nanoTime()
        val currentTime = frameStart - startTime
        updater(currentTime / 1000.0)
        val latch = CountDownLatch(taskCount)
        queues.forEach { it.offer(latch) }
        latch.await()
        val frameEndPerf = System.nanoTime()
        totalTime += (frameEndPerf - frameStartPerf) / 1000
        ++frames
        if (totalTime / 1000000 != totalTimeSec) {
            totalTimeSec = totalTime / 1000000
            println("Average render time ${totalTime / frames}")
        }
        EventQueue.invokeAndWait {
            for (y in 0 until SCENE_HEIGHT) {
                val raster = rasters[y % taskCount]
                val start = raster.pointer(0, y / taskCount)
                image.setRGB(0, y, SCENE_WIDTH, 1, raster.color, start, SCENE_WIDTH)
            }
            imageComponent.repaint()
        }
    }
}

private class ImageComponent(private val image: BufferedImage) : JComponent() {
    override fun paint(g: Graphics) {
        g as Graphics2D
        g.drawImage(image as Image, 0, 0, null)
    }

    override fun getPreferredSize(): Dimension = Dimension(SCENE_WIDTH, SCENE_HEIGHT)

    override fun getMinimumSize(): Dimension = getPreferredSize()
}