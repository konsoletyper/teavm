/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.classlib.impl.record;

import java.util.Objects;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.model.BasicBlock;
import org.teavm.model.MethodHandle;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ConditionEmitter;
import org.teavm.model.emit.ConditionProducer;
import org.teavm.model.emit.PhiEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.util.InvokeDynamicUtil;

public class ObjectMethodsSubstitutor implements BootstrapMethodSubstitutor {
    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter pe) {
        switch (callSite.getCalledMethod().getName()) {
            case "equals":
                return substituteEquals(callSite, pe);
            case "hashCode":
                return substituteHashCode(callSite, pe);
            case "toString":
                return substituteToString(callSite, pe);
            default:
                throw new RuntimeException("Unexpected method: " + callSite.getCalledMethod().getName());
        }
    }

    private ValueEmitter substituteEquals(DynamicCallSite callSite, ProgramEmitter pe) {
        ValueType type = callSite.getBootstrapArguments().get(0).getValueType();

        ValueEmitter thisVar = callSite.getArguments().get(0);
        ValueEmitter thatVar = callSite.getArguments().get(1);
        BasicBlock joint = pe.prepareBlock();
        PhiEmitter result = pe.phi(ValueType.INTEGER, joint);
        pe.when(thisVar.isSame(thatVar)).thenDo(() -> {
            pe.constant(1).propagateTo(result);
            pe.jump(joint);
        });
        ConditionProducer classCondition = () -> thatVar.isNull()
                .or(() -> thatVar.invokeVirtual("getClass", Class.class).isNotSame(pe.constant(type)));
        pe.when(classCondition).thenDo(() -> {
            pe.constant(0).propagateTo(result);
            pe.jump(joint);
        });

        ValueEmitter castThatVar = thatVar.cast(type);

        String names = callSite.getBootstrapArguments().get(1).getString();
        int argIndex = 2;
        int index = 0;
        while (index < names.length()) {
            int next = names.indexOf(';', index);
            if (next < 0) {
                next = names.length();
            }
            index = next + 1;
            MethodHandle getter = callSite.getBootstrapArguments().get(argIndex++).getMethodHandle();
            ValueEmitter thisField = InvokeDynamicUtil.invoke(pe, getter, thisVar);
            ValueEmitter thatField = InvokeDynamicUtil.invoke(pe, getter, castThatVar);
            pe.when(compareEquality(pe, getter.getValueType(), thisField, thatField).not()).thenDo(() -> {
                pe.constant(0).propagateTo(result);
                pe.jump(joint);
            });
        }

        pe.constant(1).propagateTo(result);
        pe.jump(joint);
        pe.enter(joint);

        return result.getValue();
    }

    private ConditionEmitter compareEquality(ProgramEmitter pe, ValueType type, ValueEmitter a, ValueEmitter b) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHARACTER:
                case INTEGER:
                case LONG:
                    return a.isEqualTo(b);
                case FLOAT:
                    return pe.invoke(Float.class, "compare", int.class, a, b).isEqualTo(pe.constant(0));
                case DOUBLE:
                    return pe.invoke(Double.class, "compare", int.class, a, b).isEqualTo(pe.constant(0));
            }
        }
        return pe.invoke(Objects.class, "equals", boolean.class, a.cast(Object.class), b.cast(Object.class)).isTrue();
    }

    private ValueEmitter substituteHashCode(DynamicCallSite callSite, ProgramEmitter pe) {
        ValueEmitter thisVar = callSite.getArguments().get(0);
        String names = callSite.getBootstrapArguments().get(1).getString();
        ValueEmitter resultVar = pe.constant(1);

        int argIndex = 2;
        int index = 0;
        while (index < names.length()) {
            int next = names.indexOf(';', index);
            if (next < 0) {
                next = names.length();
            }
            index = next + 1;
            MethodHandle getter = callSite.getBootstrapArguments().get(argIndex++).getMethodHandle();
            resultVar = resultVar.mul(31);
            ValueEmitter thisField = InvokeDynamicUtil.invoke(pe, getter, thisVar);
            resultVar = resultVar.add(hash(pe, getter.getValueType(), thisField));
        }

        return resultVar;
    }

    private ValueEmitter hash(ProgramEmitter pe, ValueType type, ValueEmitter a) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return pe.invoke(Boolean.class, "hashCode", int.class, a);
                case BYTE:
                    return pe.invoke(Byte.class, "hashCode", int.class, a);
                case SHORT:
                    return pe.invoke(Short.class, "hashCode", int.class, a);
                case CHARACTER:
                    return pe.invoke(Character.class, "hashCode", int.class, a);
                case INTEGER:
                    return pe.invoke(Integer.class, "hashCode", int.class, a);
                case LONG:
                    return pe.invoke(Long.class, "hashCode", int.class, a);
                case FLOAT:
                    return pe.invoke(Float.class, "hashCode", int.class, a);
                case DOUBLE:
                    return pe.invoke(Double.class, "hashCode", int.class, a);
            }
        }
        return pe.invoke(Objects.class, "hashCode", int.class, a.cast(Object.class));
    }

    private ValueEmitter substituteToString(DynamicCallSite callSite, ProgramEmitter pe) {
        ValueEmitter thisVar = callSite.getArguments().get(0);
        String names = callSite.getBootstrapArguments().get(1).getString();
        String className = ((ValueType.Object) thisVar.getType()).getClassName();
        String simpleName = callSite.getAgent().getClassSource().get(className).getSimpleName();
        if (simpleName == null) {
            int idx = className.lastIndexOf('.');
            simpleName = idx >= 0 ? className.substring(idx + 1) : className;
        }
        ValueEmitter resultVar = pe.construct(StringBuilder.class, pe.constant(simpleName + "["));

        int argIndex = 2;
        int index = 0;
        while (index < names.length()) {
            int next = names.indexOf(';', index);
            if (next < 0) {
                next = names.length();
            }
            String fieldName = names.substring(index, next);
            MethodHandle getter = callSite.getBootstrapArguments().get(argIndex++).getMethodHandle();

            String fieldTitle = (index == 0 ? "" : ", ") + fieldName + "=";
            resultVar = resultVar.invokeVirtual("append", StringBuilder.class, pe.constant(fieldTitle));
            ValueEmitter thisField = InvokeDynamicUtil.invoke(pe, getter, thisVar);
            if (getter.getValueType() instanceof ValueType.Primitive) {
                PrimitiveType primitive = ((ValueType.Primitive) getter.getValueType()).getKind();
                switch (primitive) {
                    case BYTE:
                    case SHORT:
                        thisField = thisField.cast(int.class);
                        break;
                    default:
                }
            } else {
                thisField = thisField.cast(Object.class);
            }
            resultVar = resultVar.invokeVirtual("append", StringBuilder.class, thisField);

            index = next + 1;
        }

        return resultVar.invokeVirtual("append", StringBuilder.class, pe.constant("]"))
                .invokeVirtual("toString", String.class);
    }
}
