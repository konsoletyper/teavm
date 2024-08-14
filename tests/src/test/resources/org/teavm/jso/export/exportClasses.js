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
import { A, BB } from '/tests/exportClasses/test.js';

export async function test() {
    assertEquals(23, A.foo());
    let o = BB.create(42);
    assertEquals(true, o instanceof BB);
    assertEquals(false, o instanceof A);
    assertEquals(42, o.bar);

    let p = new BB(55);
    assertEquals(true, p instanceof BB);
    assertEquals(false, p instanceof A);
    assertEquals(55, p.bar);
}