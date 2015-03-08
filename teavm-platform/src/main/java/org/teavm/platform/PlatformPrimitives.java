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
package org.teavm.platform;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public interface PlatformPrimitives extends JSObject {
    @JSMethod("$rt_voidcls")
    PlatformClass getVoidClass();

    @JSMethod("$rt_booleancls")
    PlatformClass getBooleanClass();

    @JSMethod("$rt_bytecls")
    PlatformClass getByteClass();

    @JSMethod("$rt_shortcls")
    PlatformClass getShortClass();

    @JSMethod("$rt_charcls")
    PlatformClass getCharClass();

    @JSMethod("$rt_intcls")
    PlatformClass getIntClass();

    @JSMethod("$rt_longcls")
    PlatformClass getLongClass();

    @JSMethod("$rt_floatcls")
    PlatformClass getFloatClass();

    @JSMethod("$rt_doublecls")
    PlatformClass getDoubleClass();
}
