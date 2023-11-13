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

package org.teavm.samples.software3d.scenes

import org.teavm.samples.software3d.geometry.Matrix
import org.teavm.samples.software3d.geometry.Vector
import org.teavm.samples.software3d.scene.Item
import org.teavm.samples.software3d.scene.Scene
import kotlin.math.PI

fun geometry(): Pair<Scene, (Double) -> Unit> {
    val scene = Scene()

    val cubeConstantTransform = Matrix.scale(1.4f, 1.4f, 1.4f) * Matrix.rotation(1f, 0f, 0f, PI.toFloat() / 4)
    val cubeItem = Item(Meshes.cube())
    scene.items += cubeItem

    val sphere1 = Item(Meshes.sphere())
    val sphere1ConstantTransform = Matrix.translation(2f, 0f, 0f) * Matrix.scale(0.2f, 0.2f, 0.2f)
    scene.items += sphere1

    val sphere2 = Item(Meshes.sphere())
    val sphere2ConstantTransform = Matrix.translation(-3.5f, 0f, 0f) * Matrix.scale(0.3f, 0.3f, 0.3f)
    scene.items += sphere2

    scene.camera = Matrix.lookAt(0f, 2f, -7f, 0f, -0.3f, 1f).inverse()
    scene.lightColor = Vector(0.5f, 0.5f, 0.5f, 0.5f)
    scene.lightPosition = Vector(3f, 0.5f, -2f, 1f)
    scene.ambientColor = Vector(0.5f, 0.5f, 0.5f, 0.5f)

    return Pair(scene) { time ->
        val cubeVarTransform = Matrix.rotation(0f, 1f, 0f, (PI * time / 3).toFloat())
        cubeItem.transform = cubeVarTransform * cubeConstantTransform

        val sphere1VarTransform = Matrix.rotation(0f, 1f, -0.5f, (-PI * time).toFloat())
        sphere1.transform = sphere1VarTransform * sphere1ConstantTransform

        val sphere2VarTransform = Matrix.rotation(0.5f, 1f, 0.5f, (-PI * time / 10).toFloat())
        sphere2.transform = sphere2VarTransform * sphere2ConstantTransform
    }
}