/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.core;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSPrimitiveType;
import org.teavm.jso.JSProperty;

@JSPrimitiveType("function")
public abstract class JSFunction implements JSObject {
    @JSProperty
    public abstract int getLength();

    @JSProperty
    public abstract String getName();

    public abstract Object call(Object thisArg);

    public abstract Object call(Object thisArg, Object a);

    public abstract Object call(Object thisArg, Object a, Object b);

    public abstract Object call(Object thisArg, Object a, Object b, Object c);

    public abstract Object call(Object thisArg, Object a, Object b, Object c, Object d);

    public abstract Object call(Object thisArg, Object a, Object b, Object c, Object d, Object e);

    public abstract Object call(Object thisArg, Object a, Object b, Object c, Object d, Object e, Object f);

    public abstract Object call(Object thisArg, Object a, Object b, Object c, Object d, Object e, Object f, Object g);

    public abstract Object call(Object thisArg, Object a, Object b, Object c, Object d, Object e, Object f, Object g,
            Object h);

    public abstract Object apply(Object thisArg, @JSByRef(optional = true) Object[] arguments);
}
