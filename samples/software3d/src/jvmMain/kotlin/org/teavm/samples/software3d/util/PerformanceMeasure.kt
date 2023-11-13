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

package org.teavm.samples.software3d.util

class PerformanceMeasure(private val measureInterval: Long, private val update: (Long) -> Unit) {
    private var totalPerfTime = 0L
    private var totalPertTimeSecond = 0L
    private var frameCount = 0
    private var startPerfTime = 0L

    fun reset() {
        totalPerfTime = 0L
        totalPertTimeSecond = 0L
        frameCount = 0
    }

    fun startFrame() {
        startPerfTime = System.nanoTime()
    }

    fun endFrame() {
        val endPerfTime = System.nanoTime()
        reportFrame(endPerfTime - startPerfTime)
    }

    fun reportFrame(frameTime: Long) {
        totalPerfTime += frameTime / 1000
        frameCount++
        if (totalPerfTime / measureInterval != totalPertTimeSecond) {
            totalPertTimeSecond = totalPerfTime / measureInterval
            update(totalPerfTime / frameCount)
        }
    }
}