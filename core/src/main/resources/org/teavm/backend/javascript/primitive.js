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

let $rt_createPrimitiveCls = (name, binaryName, kind, config) => {
    let cls = () => {};
    let meta = $rt_newClassMetadata({
        name: name,
        binaryName: binaryName,
        modifiers: 1 | (1 << 4),
        primitiveKind: kind
    });
    cls[$rt_meta] = meta;
    if (typeof config === 'function') {
        config(meta);
    }
    return cls;
}
let $rt_booleancls = $rt_createPrimitiveCls("boolean", "Z", 1, meta => {
    if (teavm_javaMethodExists("java.lang.Boolean", "valueOf(Z)Ljava/lang/Boolean;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Boolean", "valueOf(Z)Ljava/lang/Boolean;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Boolean", "booleanValue()Z")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Boolean", "booleanValue()Z")(o);
    }
});
let $rt_bytecls = $rt_createPrimitiveCls("byte", "B", 2, meta => {
    if (teavm_javaMethodExists("java.lang.Byte", "valueOf(B)Ljava/lang/Byte;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Byte", "valueOf(B)Ljava/lang/Byte;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Byte", "byteValue()B")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Byte", "byteValue()B")(o);
    }

});
let $rt_shortcls = $rt_createPrimitiveCls("short", "S", 3, meta => {
    if (teavm_javaMethodExists("java.lang.Short", "valueOf(S)Ljava/lang/Short;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Short", "valueOf(S)Ljava/lang/Short;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Short", "shortValue()S")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Short", "shortValue()S")(o);
    }
});
let $rt_charcls = $rt_createPrimitiveCls("char", "C", 4, meta => {
    if (teavm_javaMethodExists("java.lang.Character", "valueOf(C)Ljava/lang/Character;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Character", "valueOf(C)Ljava/lang/Character;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Character", "charValue()C")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Character", "charValue()C")(o);
    }
});
let $rt_intcls = $rt_createPrimitiveCls("int", "I", 5, meta => {
    if (teavm_javaMethodExists("java.lang.Integer", "valueOf(I)Ljava/lang/Integer;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Integer", "valueOf(I)Ljava/lang/Integer;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Integer", "intValue()I")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Integer", "intValue()I")(o);
    }
});
let $rt_longcls = $rt_createPrimitiveCls("long", "J", 6, meta => {
    if (teavm_javaMethodExists("java.lang.Long", "valueOf(J)Ljava/lang/Long;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Long", "valueOf(J)Ljava/lang/Long;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Long", "longValue()J")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Long", "longValue()J")(o);
    }
});
let $rt_floatcls = $rt_createPrimitiveCls("float", "F", 7, meta => {
    if (teavm_javaMethodExists("java.lang.Float", "valueOf(F)Ljava/lang/Float;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Float", "valueOf(F)Ljava/lang/Float;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Float", "floatValue()F")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Float", "floatValue()F")(o);
    }
});
let $rt_doublecls = $rt_createPrimitiveCls("double", "D", 8, meta => {
    if (teavm_javaMethodExists("java.lang.Double", "valueOf(D)Ljava/lang/Double;")) {
        meta.valueToObject = o => teavm_javaMethod("java.lang.Double", "valueOf(D)Ljava/lang/Double;")(o);
    }
    if (teavm_javaMethodExists("java.lang.Double", "doubleValue()D")) {
        meta.objectToValue = o => teavm_javaMethod("java.lang.Double", "doubleValue()D")(o);
    }
});
let $rt_voidcls = $rt_createPrimitiveCls("void", "V", 9);
