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

import org.teavm.jso.dom.html.HTMLDocument
import org.teavm.jso.dom.html.HTMLElement
import org.teavm.jso.dom.html.HTMLOptionElement
import org.teavm.jso.dom.html.HTMLSelectElement

private const val SCENE_WIDTH = 1024
private const val SCENE_HEIGHT = 768

fun main(args: Array<out String>) {
    if (args.size == 1 && args[0] == "worker") {
        worker()
    } else {
        runController()
    }
}

fun runController() {
    val performanceIndicator = HTMLDocument.current().getElementById("performance-indicator")
    var performanceIndicatorByWorkers: List<HTMLElement> = emptyList()
    val maxWorkers = Runtime.getRuntime().availableProcessors()
    var workerType = WorkerType.JS
    val controller = Controller(SCENE_WIDTH, SCENE_HEIGHT) { index, value ->
        if (index == -1) {
            performanceIndicator.innerText = value.toString()
        } else {
            performanceIndicatorByWorkers.getOrNull(index)?.let {
                it.innerText = value.toString()
            }
        }
    }
    performanceIndicatorByWorkers = recreatePerformanceIndicators(maxWorkers)
    controller.startRendering(maxWorkers, workerType)

    val cpuSelector = HTMLDocument.current().getElementById("workers") as HTMLSelectElement
    for (i in 1..maxWorkers) {
        val option = HTMLDocument.current().createElement("option") as HTMLOptionElement
        option.value = i.toString()
        option.text = i.toString()
        cpuSelector.appendChild(option)
    }
    cpuSelector.value = maxWorkers.toString()
    cpuSelector.addEventListener("change") {
        val newValue = cpuSelector.value.toInt()
        if (controller.tasks != newValue) {
            controller.startRendering(newValue, workerType)
            performanceIndicatorByWorkers = recreatePerformanceIndicators(newValue)
        }
    }

    val workerSelector = HTMLDocument.current().getElementById("worker-type") as HTMLSelectElement
    workerSelector.addEventListener("change") {
        val newValue = when (workerSelector.value) {
            "webassembly" -> WorkerType.WEBASSEMBLY
            "kjs" -> WorkerType.KOTLIN_JS
            else -> WorkerType.JS
        }
        if (workerType != newValue) {
            workerType = newValue
            controller.startRendering(controller.tasks, workerType)
        }
    }
}

private fun recreatePerformanceIndicators(count: Int): List<HTMLElement> {
    val container = HTMLDocument.current().getElementById("performance-indicators-by-workers")
    while (container.hasChildNodes()) {
        container.removeChild(container.firstChild)
    }
    return (0 until count).map {
        HTMLDocument.current().createElement("li").also {
            container.appendChild(it)
        }
    }
}
