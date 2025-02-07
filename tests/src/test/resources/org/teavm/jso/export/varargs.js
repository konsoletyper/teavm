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

const varargs = await (await import('/tests/varargs/provider.js')).default;

export async function test() {
    assertEquals("strings: a, b", varargs.strings("a", "b"));
    assertEquals("strings(23): a, b", varargs.prefixStrings(23, "a", "b"));
    assertEquals("ints: 23, 42", varargs.ints(23, 42));
    assertEquals("ints(*): 23, 42", varargs.prefixInts("*", 23, 42));
    assertEquals("objects: a, b", varargs.objects({ stringValue: "a" }, { stringValue: "b" }));

    let obj = new varargs.A();
    assertEquals("A.strings: a, b", obj.strings("a", "b"));
}
