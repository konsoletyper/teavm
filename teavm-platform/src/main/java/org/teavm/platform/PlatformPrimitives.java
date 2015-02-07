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

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface PlatformPrimitives extends JSObject {
    @JSProperty("$rt_voidcls")
    PlatformClass getVoidClass();

    @JSProperty("$rt_booleancls")
    PlatformClass getBooleanClass();

    @JSProperty("$rt_bytecls")
    PlatformClass getByteClass();

    @JSProperty("$rt_shortcls")
    PlatformClass getShortClass();

    @JSProperty("$rt_charcls")
    PlatformClass getCharClass();

    @JSProperty("$rt_intcls")
    PlatformClass getIntClass();

    @JSProperty("$rt_longcls")
    PlatformClass getLongClass();

    @JSProperty("$rt_floatcls")
    PlatformClass getFloatClass();

    @JSProperty("$rt_doublecls")
    PlatformClass getDoubleClass();
}
