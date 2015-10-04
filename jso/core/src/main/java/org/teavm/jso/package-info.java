/*
 *  Copyright 2015 Alexey Andreev.
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
 * <p>JSO is a specification that describes how Java platform can interact with JavaScript code.
 * There are two parts of this specification: JSO core, that defines mapping between Java and JavaScript
 * objects and JSO APIs, that define Java wrappers around various JavaScript and HTML5 APIs.
 * The latter part is simply application of the former one. JSO implementor must implement only the first part,
 * and it may ignore the second part, since it should work properly this way. However, it may implement
 * some of the APIs itself in some cases, for example to improve performance.</p>
 *
 * <p>The first part of JSO is directly in this package. All subpackages declare the second part of JSO.</p>
 *
 * <p>JSO does not do anything itself. It is only a set of interfaces that define interaction.
 * To use JSO in your application, you should include one of its implementations, that may exist for
 * different platforms, such as JVM, RoboVM, Android, TeaVM or bck2brwsr.</p>
 *
 *
 * <h2>JSBody annotation</h2>
 *
 * <p>The easiest way to invoke JavaScript code from Java is to define native method marked with the
 * {@link org.teavm.jso.JSBody} annotation that contains the JavaScript code.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 *    {@literal @}JSBody(params = { "message" }, script = "window.alert(message);")
 *    public static native void alert(String message);
 * </pre>
 *
 *
 * <h2>Overlay types</h2>
 *
 * <p>Often you need to pass complex values between Java and JavaScript. Primitives are usually insufficient for
 * this purposed. JSO comes with concept of overlay types, that are usable from both Java and JavaScript.
 * For detailed description, see {@link org.teavm.jso.JSObject} interface.</p>
 *
 * <p>When wrapping JavaScript APIs in Java classes, you usually write boilerplate {@link org.teavm.jso.JSBody} like
 * this:</p>
 *
 * <pre>
 * {@literal @}JSBody(params = "newChild", script = "return this.appendChild(newChild);")
 * Node appendChild(Node newChild);
 * </pre>
 *
 * <p>JSO offers shortcut annotations that help to avoid such boilerplate. They are: {@link org.teavm.jso.JSMethod},
 * {@link org.teavm.jso.JSProperty}, {@link org.teavm.jso.JSIndexer}.
 *
 */
package org.teavm.jso;
