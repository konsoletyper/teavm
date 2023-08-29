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
package org.teavm.jso;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Indicates that method is to have native JavaScript implementation.
 * Method only can take and return primitive values and {@link JSObject}s.
 * Note that unless method is static, it must belong to class that implements {@link JSObject}.
 * If applied to non-native method, original Java body will be overwritten by JavaScript.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 *    {@literal @}JSBody(params = { "message" }, script = "window.alert(message);")
 *    public static native void alert(String message);
 * </pre>
 *
 * <p>The motivation for <code>params</code> field is the following: Java can avoid inclusion of parameter names
 * into bytecode (for example, you can compile with no debug information). In order the JSO implementation
 * with no access to original source code could work properly, JSO forces developer to specify parameter
 * names explicitly.</p>
 *
 *
 * <h2>Type conversion</h2>
 *
 * <p>A method marked with JSBody annotation is restricted to take parameters of allowed types.
 * A type is allowed if it is either:</p>
 *
 * <ul>
 *   <li>a primitive Java type, i.e. <code>boolean</code>, <code>byte</code>, <code>short</code>, <code>char</code>,
 *       <code>int</code>, <code>long</code>, <code>float</code> or <code>double</code>;</li>
 *   <li>is <code>java.lang.String</code> class;</li>
 *   <li>an overlay type, see {@link JSObject};</li>
 *   <li>an array of allowed type.</li>
 * </ul>
 *
 * <p>Java primitives are converted to corresponding JavaScript primitives, except for <code>char</code>,
 * which does not have corresponding JavaScript representation. JSO implementation converts Java chars to JavaScript
 * numbers, and expects a JavaScript number when converting to a Java character.</p>
 *
 * <p>Java arrays are converted to JavaScript arrays and vice versa. Arrays are passed by copy.</p>
 *
 * <p><code>java.lang.String</code> objects are converted to JavaScript string.</p>
 *
 * <p>Overlay types are passed as is.</p>
 *
 *
 * <h2>Passing functions</h2>
 *
 * <p>Sometimes JavaScript functions expect you to pass a function as a parameter value. JSO allows you to
 * pass special form of overlay objects as functions. These overlay objects must be interfaces with exactly one
 * parameter and marked with {@link JSFunctor} annotation. Example:</p>
 *
 * <pre>
 *    {@literal @}JSFunctor
 *    interface TimerHandler extends JSObject {
 *        void onTimer();
 *    }
 *
 *    {@literal @}JSBody(params = { "handler", "delay" }, script = "return window.setTimeout(handler, delay);")
 *    public static native int setTimeout(TimerHandler handler, int delay);
 * </pre>
 *
 *
 * <h2>Calling Java methods from JSBody</h2>
 *
 * <p>You can call Java methods from JSBody script. You should use the following notation:
 *
 * <pre>javaMethods.get('<i>method reference</i>').invoke([instance, ] param1 [, param2 ...]);</pre>
 *
 * <p>The method reference has the following format:</p>
 *
 * <pre>
 * (<i>PackageName</i> <b>.</b>)* <i>ClassName</i> <b>.</b> <i>MethodName</i> <i>MethodDescriptor</i>
 * </pre>
 *
 * <p>where</p>
 *
 * <pre>
 * <i>PackageName</i> = <i>Identifier</i>
 * <i>ClassName</i> = <i>Identifier</i>
 * <i>MethodName</i> = <i>Identifier</i>
 * <i>MethodDescriptor</i> = <b>(</b> <i>TypeDescriptor</i> <b>)</b> <b>V</b>)
 *                         | <b>(</b> <i>TypeDescriptor</i> <b>)</b> <i>TypeDescriptor</i>
 * <i>TypeDescriptor</i> = <b>Z</b> | <b>B</b> | <b>C</b> | <b>S</b> | <b>I</b> | <b>J</b> | <b>F</b> | <b>D</b>
 *                | <b>L</b> <i>QualifiedClassName</i> <b>;</b> | <b>[</b> <i>TypeDescriptor</i>.
 * </pre>
 *
 * <p>that is similar to <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">method
 * descriptors</a> augmented with class and method names.</p>
 *
 * <p>For example,</p>
 *
 * <pre>
 *    {@literal @}JSBody(params = { "message" }, script = "javaMethods.get('org.teavm.jso.browser.Window"
 *            + ".alert(Ljava/lang/String;)V').invoke(message);")
 *    public static native void alertCaller(String message);
 * </pre>
 *
 * <p>Note that <code>get</code> method must take string constant. Dynamic resolution of Java methods may
 * not work on some platforms.</p>
 *
 * @see JSObject
 * @author Alexey Andreev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSBody {
    /**
     * <p>How method parameters are named inside JavaScript implementation.</p>
     */
    String[] params() default {};

    /**
     * <p>JavaScript code.</p>
     */
    String script();

    JSBodyImport[] imports() default {};
}
