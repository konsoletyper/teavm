/*
 *  Copyright 2023 Alexey Andreev.
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
"use strict";

let $rt_createcls = () => {
    return {
        $array : null,
        classObject : null,
        $meta: {
            supertypes : [],
            superclass : null
        }
    };
}
let $rt_createPrimitiveCls = (name, binaryName) => {
    let cls = $rt_createcls();
    cls.$meta.primitive = true;
    cls.$meta.name = name;
    cls.$meta.binaryName = binaryName;
    cls.$meta.enum = false;
    cls.$meta.item = null;
    cls.$meta.simpleName = null;
    cls.$meta.declaringClass = null;
    cls.$meta.enclosingClass = null;
    return cls;
}
let $rt_booleancls = $rt_createPrimitiveCls("boolean", "Z");
let $rt_charcls = $rt_createPrimitiveCls("char", "C");
let $rt_bytecls = $rt_createPrimitiveCls("byte", "B");
let $rt_shortcls = $rt_createPrimitiveCls("short", "S");
let $rt_intcls = $rt_createPrimitiveCls("int", "I");
let $rt_longcls = $rt_createPrimitiveCls("long", "J");
let $rt_floatcls = $rt_createPrimitiveCls("float", "F");
let $rt_doublecls = $rt_createPrimitiveCls("double", "D");
let $rt_voidcls = $rt_createPrimitiveCls("void", "V");
