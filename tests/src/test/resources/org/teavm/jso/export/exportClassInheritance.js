/*
 *  Copyright 2025 konsoletyper.
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
const { BaseClass, Subclass, SubAbstract } = await (await import("/tests/exportClassInheritance/provider.js")).default;

export async function test() {
    assertEquals("Base.foo", new BaseClass().foo());
    assertEquals("Sub.foo", new Subclass().foo());
    assertEquals("Sub.bar", new Subclass().bar());
    //assertEquals("foo", new SubAbstract().foo);
    assertEquals(true, new Subclass() instanceof BaseClass);
    assertEquals(false, new BaseClass() instanceof Subclass);
}