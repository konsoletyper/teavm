/*
 *  Copyright 2025 Alexey Andreev.
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

package org.teavm.kotlin.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.teavm.junit.SkipPlatform
import org.teavm.junit.TeaVMTestRunner
import org.teavm.junit.TestPlatform

@RunWith(TeaVMTestRunner::class)
class SerializationTest {
    @Test
    @SkipPlatform(TestPlatform.WASI, TestPlatform.WEBASSEMBLY)
    // TODO: fix issue and un-skip
    fun serialize() {
        val json = Json.encodeToJsonElement(TestClass().apply {
            a = 23
            b = "*"
        })
        assertEquals(JsonPrimitive(23), json.jsonObject["a"])
        assertEquals(JsonPrimitive("*"), json.jsonObject["b"])
    }

    @Serializable
    class TestClass {
        var a: Int = 0
        var b: String = ""
    }
}