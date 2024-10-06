/*
 *  Copyright 2024 Alexey Andreev.
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
const java = await (await import('/tests/primitives/provider.js')).default;

function testReturnPrimitives() {
    assertEquals(true, java.boolResult());
    assertEquals(1, java.byteResult());
    assertEquals(2, java.shortResult());
    assertEquals(3, java.intResult());
    assertApproxEquals(4.1, java.floatResult());
    assertApproxEquals(5.2, java.doubleResult());
    assertEquals("q", java.stringResult());
}

function testReturnArrays() {
    assertEquals([true, false], java.boolArrayResult());
    assertEquals([1, 2], java.byteArrayResult());
    assertEquals([2, 3], java.shortArrayResult());
    assertEquals([3, 4], java.intArrayResult());
    assertEquals([4, 5], java.floatArrayResult());
    assertEquals([5, 6], java.doubleArrayResult());
    assertEquals(["q", "w"], java.stringArrayResult());
}

function testConsumePrimitives() {
    assertEquals("bool:true", java.boolParam(true));
    assertEquals("byte:1", java.byteParam(1));
    assertEquals("short:2", java.shortParam(2));
    assertEquals("int:3", java.intParam(3));
    assertEquals("float:4.0", java.floatParam(4));
    assertEquals("double:5.0", java.doubleParam(5));
    assertEquals("string:q", java.stringParam("q"));
}

export async function test() {
    testReturnPrimitives();
    testReturnArrays();
    testConsumePrimitives();
}