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

function getLength(array) {
    if (array === null || array.constructor.$meta.item === 'undefined') {
        $rt_throw(teavm_javaConstructor("java.lang.IllegalArgumentException", "()V")());
    }
    return array.data.length;
}

function newInstanceImpl(type, length) {
    if (type.$meta.primitive) {
        teavm_fragment("primitiveArrays");
    }
    return $rt_createArray(type, length);
}

function getImpl(array, index) {
    var item = array.data[index];
    var type = array.constructor.$meta.item;
    if (teavm_javaMethodExists("java.lang.Boolean", "valueOf(Z)Ljava/lang/Boolean;") && type === $rt_booleancls) {
        return teavm_javaMethod("java.lang.Boolean", "valueOf(Z)Ljava/lang/Boolean;")(item);
    } else if (teavm_javaMethodExists("java.lang.Byte", "valueOf(B)Ljava/lang/Byte;") && type === $rt_bytecls) {
        return teavm_javaMethod("java.lang.Byte", "valueOf(B)Ljava/lang/Byte;")(item);
    } else if (teavm_javaMethodExists("java.lang.Short", "valueOf(S)Ljava/lang/Short;") && type === $rt_shortcls) {
        return teavm_javaMethod("java.lang.Short", "valueOf(S)Ljava/lang/Short;")(item);
    } else if (teavm_javaMethodExists("java.lang.Character", "valueOf(C)Ljava/lang/Character;")
            && type === $rt_charcls) {
        return teavm_javaMethod("java.lang.Character", "valueOf(C)Ljava/lang/Character;")(item);
    } else if (teavm_javaMethodExists("java.lang.Integer", "valueOf(I)Ljava/lang/Integer;") && type === $rt_intcls) {
        return teavm_javaMethod("java.lang.Integer", "valueOf(I)Ljava/lang/Integer;")(item);
    } else if (teavm_javaMethodExists("java.lang.Long", "valueOf(J)Ljava/lang/Long;") && type === $rt_longcls) {
        return teavm_javaMethod("java.lang.Long", "valueOf(J)Ljava/lang/Long;")(item);
    } else if (teavm_javaMethodExists("java.lang.Float", "valueOf(F)Ljava/lang/Float;") && type === $rt_floatcls) {
        return teavm_javaMethod("java.lang.Float", "valueOf(F)Ljava/lang/Float;")(item);
    } else if (teavm_javaMethodExists("java.lang.Double", "valueOf(D)Ljava/lang/Double;") && type === $rt_doublecls) {
        return teavm_javaMethod("java.lang.Double", "valueOf(D)Ljava/lang/Double;")(item);
    } else {
        return item;
    }
}

function setImpl(array, index, value) {
    var type = array.constructor.$meta.item;
    if (teavm_javaMethodExists("java.lang.Boolean", "booleanValue()Z") && type === $rt_booleancls) {
        array.data[index] = teavm_javaMethod("java.lang.Boolean", "booleanValue()Z")(value);
    } else if (teavm_javaMethodExists("java.lang.Byte", "byteValue()B") && type === $rt_booleancls) {
        array.data[index] = teavm_javaMethod("java.lang.Byte", "byteValue()B")(value);
    } else if (teavm_javaMethodExists("java.lang.Short", "shortValue()S") && type === $rt_shortcls) {
        array.data[index] = teavm_javaMethod("java.lang.Short", "shortValue()S")(value);
    } else if (teavm_javaMethodExists("java.lang.Character", "charValue()C") && type === $rt_charcls) {
        array.data[index] = teavm_javaMethod("java.lang.Character", "charValue()C")(value);
    } else if (teavm_javaMethodExists("java.lang.Integer", "intValue()I") && type === $rt_intcls) {
        array.data[index] = teavm_javaMethod("java.lang.Integer", "intValue()I")(value);
    } else if (teavm_javaMethodExists("java.lang.Long", "longValue()J") && type === $rt_longcls) {
        array.data[index] = teavm_javaMethod("java.lang.Long", "longValue()J")(value);
    } else if (teavm_javaMethodExists("java.lang.Float", "floatValue()F") && type === $rt_floatcls) {
        array.data[index] = teavm_javaMethod("java.lang.Float", "floatValue()F")(value);
    } else if (teavm_javaMethodExists("java.lang.Double", "doubleValue()D") && type === $rt_floatcls) {
        array.data[index] = teavm_javaMethod("java.lang.Double", "doubleValue()D")(value);
    } else {
        array.data[index] = value;
    }
}