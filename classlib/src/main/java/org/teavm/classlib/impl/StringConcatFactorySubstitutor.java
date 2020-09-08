/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.classlib.impl;

import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.IntegerSubtype;

public class StringConcatFactorySubstitutor implements BootstrapMethodSubstitutor {
    private ReferenceCache referenceCache = new ReferenceCache();
    private static final String STRING_BUILDER = "java.lang.StringBuilder";
    private static final char VALUE_ARGUMENT = '\1';
    private static final char CONST_ARGUMENT = '\2';

    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter pe) {
        ValueEmitter sb = pe.construct(STRING_BUILDER);

        if (callSite.getBootstrapMethod().getName().equals("makeConcatWithConstants")) {
            appendArgumentWithRecipe(sb, callSite);
        } else {
            appendSimpleArguments(sb, callSite);
        }

        return sb.invokeSpecial("toString", ValueType.object("java.lang.String"));
    }

    private ValueEmitter appendSimpleArguments(ValueEmitter sb, DynamicCallSite callSite) {
        int parameterCount = callSite.getCalledMethod().parameterCount();
        for (int i = 0; i < parameterCount; ++i) {
            sb = appendArgument(sb, callSite.getCalledMethod().parameterType(i), callSite.getArguments().get(i));
        }
        return sb;
    }

    private ValueEmitter appendArgumentWithRecipe(ValueEmitter sb, DynamicCallSite callSite) {
        String recipe = callSite.getBootstrapArguments().get(0).getString();
        int charCount = recipe.length();
        int constantIndex = 0;
        int valueIndex = 0;
        int paramIndex = 0;
        StringBuilder acc = new StringBuilder();
        for (int i = 0; i < charCount; ++i) {
            char c = recipe.charAt(i);
            switch (c) {
                case VALUE_ARGUMENT: {
                    sb = flushAcc(sb, acc);
                    ValueType type = callSite.getCalledMethod().parameterType(paramIndex++);
                    sb = appendArgument(sb, type, callSite.getArguments().get(valueIndex++));
                    break;
                }
                case CONST_ARGUMENT: {
                    sb = flushAcc(sb, acc);
                    ValueType type = callSite.getCalledMethod().parameterType(paramIndex++);
                    RuntimeConstant poolConstant = callSite.getBootstrapArguments().get(1 + constantIndex++);
                    sb = appendArgument(sb, type, constant(sb.getProgramEmitter(), poolConstant));
                    break;
                }
                default:
                    acc.append(c);
                    break;
            }
        }

        sb = flushAcc(sb, acc);
        return sb;
    }

    private ValueEmitter flushAcc(ValueEmitter sb, StringBuilder acc) {
        if (acc.length() == 0) {
            return sb;
        } else if (acc.length() == 1) {
            sb = appendArgument(sb, ValueType.CHARACTER, sb.getProgramEmitter().constant(acc.charAt(0))
                    .cast(ValueType.CHARACTER));
        } else {
            sb = appendArgument(sb, ValueType.object("java.lang.Object"),
                    sb.getProgramEmitter().constant(acc.toString()));
        }
        acc.setLength(0);
        return sb;
    }

    private ValueEmitter appendArgument(ValueEmitter sb, ValueType type, ValueEmitter argument) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                    argument = argument.castToInteger(IntegerSubtype.BYTE);
                    type = ValueType.INTEGER;
                    break;
                case SHORT:
                    argument = argument.castToInteger(IntegerSubtype.SHORT);
                    type = ValueType.INTEGER;
                    break;
                default:
                    break;
            }
        } else {
            type = ValueType.object("java.lang.Object");
        }
        MethodReference method = referenceCache.getCached(new MethodReference(STRING_BUILDER, "append", type,
                ValueType.object(STRING_BUILDER)));
        return sb.invokeSpecial(method, argument);
    }

    private ValueEmitter constant(ProgramEmitter pe, RuntimeConstant value) {
        switch (value.getKind()) {
            case RuntimeConstant.STRING:
                return pe.constant(value.getString());
            case RuntimeConstant.TYPE:
                return pe.constant(value.getValueType());
            case RuntimeConstant.INT:
                return pe.constant(value.getInt());
            case RuntimeConstant.LONG:
                return pe.constant(value.getLong());
            case RuntimeConstant.FLOAT:
                return pe.constant(value.getFloat());
            case RuntimeConstant.DOUBLE:
                return pe.constant(value.getDouble());
            default:
                throw new IllegalArgumentException("Unsupported constant type: " + value.getKind());
        }
    }
}
