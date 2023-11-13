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

package org.teavm.samples.software3d.scene

import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.geometry.Vector

class Scene {
    val items: MutableList<Item> = mutableListOf()
    var camera: Matrix = Matrix.identity
    var lightPosition: Vector = Vector.zero
    var lightColor: Vector = Vector.zero
    var ambientColor: Vector = Vector.zero
}