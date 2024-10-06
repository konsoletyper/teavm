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
const { createObject, consumeObject, C } = await (await import('/tests/exportClassMembers/provider.js')).default;

export async function test() {
    let o = createObject("qwe");
    assertEquals(23, o.foo);
    assertEquals("qwe:42", o.bar());
    assertEquals("consumeObject:qwe:42", consumeObject(o));
    assertEquals(99, C.baz());
    assertEquals("I'm static", C.staticProp);
}