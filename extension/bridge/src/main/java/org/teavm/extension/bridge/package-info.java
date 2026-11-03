/*
 *  Copyright 2026 Alexey Andreev.
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

/**
 * Protocol that allows to run extensions (policies from {@code org.teavm.extension.spi}) in a separate module,
 * compiled by TeaVM, while TeaVM compiler itself runs in another module (i.e. bootstrapped compiler).
 *
 * <p>Interaction happens through JS objects. Interfaces of this package are JSO interfaces, which work in both
 * directions: the side that implements interface in Java exports its implementation to JS,
 * the other side consumes the same interface as a JS object. There are two parties:
 *
 * <ul>
 *   <li><em>host</em> is the compiler; it implements {@link org.teavm.extension.bridge.BridgeHost}, which
 *     exposes compiler's {@link org.teavm.extension.ExtensionEnvironment}.</li>
 *   <li><em>guest</em> is the module that contains extensions; it implements
 *     {@link org.teavm.extension.bridge.BridgeGuest}, see {@link org.teavm.extension.bridge.guest.ExtensionGuest}
 *     as an entry point.</li>
 * </ul>
 *
 * <p>Introspected elements (classes, methods, annotations and so on) are passed as JS objects, that implement
 * {@link org.teavm.extension.bridge.BridgeElement} and its subinterfaces. Host guarantees that the same element
 * is always represented by the same JS object, so guest can maintain identity of elements, on which extensions
 * may rely. When guest passes element back to host, host receives its own object.
 */
package org.teavm.extension.bridge;
