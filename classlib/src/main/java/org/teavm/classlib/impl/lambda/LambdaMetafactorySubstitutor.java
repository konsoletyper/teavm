/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.classlib.impl.lambda;

import java.util.Arrays;
import org.teavm.cache.NoCache;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHolder;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class LambdaMetafactorySubstitutor implements BootstrapMethodSubstitutor {
    private static final int FLAG_SERIALIZABLE = 1;
    private static final int FLAG_MARKERS = 2;
    private static final int FLAG_BRIDGES = 4;
    private int lambdaIndex;

    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter callerPe) {
        ValueType[] invokedType = callSite.getCalledMethod().getSignature();
        ValueType[] samMethodType = callSite.getBootstrapArguments().get(0).getMethodType();
        MethodHandle implMethod = callSite.getBootstrapArguments().get(1).getMethodHandle();
        ValueType[] instantiatedMethodType = callSite.getBootstrapArguments().get(2).getMethodType();

        String samName = ((ValueType.Object) callSite.getCalledMethod().getResultType()).getClassName();
        ClassReaderSource classSource = callSite.getAgent().getClassSource();
        ClassReader samClass = classSource.get(samName);

        ClassHolder implementor = new ClassHolder("$$LAMBDA" + (lambdaIndex++) + "$$");
        implementor.setLevel(AccessLevel.PUBLIC);
        if (samClass != null && samClass.hasModifier(ElementModifier.INTERFACE)) {
            implementor.setParent("java.lang.Object");
            implementor.getInterfaces().add(samName);
        } else {
            implementor.setParent(samName);
        }

        int capturedVarCount = callSite.getCalledMethod().parameterCount();
        MethodHolder ctor = createConstructor(classSource, implementor,
                Arrays.copyOfRange(invokedType, 0, capturedVarCount));
        ctor.getAnnotations().add(new AnnotationHolder(NoCache.class.getName()));
        createBridge(classSource, implementor, callSite.getCalledMethod().getName(), instantiatedMethodType,
                samMethodType);

        MethodHolder worker = new MethodHolder(callSite.getCalledMethod().getName(), instantiatedMethodType);
        worker.getAnnotations().add(new AnnotationHolder(NoCache.class.getName()));
        worker.setLevel(AccessLevel.PUBLIC);
        ProgramEmitter pe = ProgramEmitter.create(worker, callSite.getAgent().getClassSource());
        ValueEmitter thisVar = pe.var(0, implementor);
        ValueEmitter[] arguments = new ValueEmitter[instantiatedMethodType.length - 1];
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = pe.var(i + 1, instantiatedMethodType[i]);
        }

        ValueType[] implementorSignature = getSignature(implMethod);
        ValueEmitter[] passedArguments = new ValueEmitter[implementorSignature.length - 1];
        for (int i = 0; i < capturedVarCount; ++i) {
            passedArguments[i] = thisVar.getField("_" + i, invokedType[i]);
        }
        for (int i = 0; i < instantiatedMethodType.length - 1; ++i) {
            passedArguments[i + capturedVarCount] = tryConvertArgument(arguments[i], instantiatedMethodType[i],
                    implementorSignature[i + capturedVarCount]);
        }

        ValueEmitter result = invoke(pe, implMethod, passedArguments);
        ValueType expectedResult = instantiatedMethodType[instantiatedMethodType.length - 1];
        if (result != null && expectedResult != ValueType.VOID) {
            ValueType actualResult = implementorSignature[implementorSignature.length - 1];
            tryConvertArgument(result, actualResult, expectedResult).returnValue();
        } else {
            pe.exit();
        }

        implementor.addMethod(worker);

        // Handle altMetafactory case
        if (callSite.getBootstrapArguments().size() > 3) {
            int flags = callSite.getBootstrapArguments().get(3).getInt();

            if ((flags & FLAG_SERIALIZABLE) != 0) {
                implementor.getInterfaces().add("java.io.Serializable");
            }

            int bootstrapArgIndex = 4;
            if ((flags & FLAG_MARKERS) != 0) {
                int markerCount = callSite.getBootstrapArguments().get(bootstrapArgIndex++).getInt();
                for (int i = 0; i < markerCount; ++i) {
                    ValueType markerType = callSite.getBootstrapArguments().get(bootstrapArgIndex++).getValueType();
                    implementor.getInterfaces().add(((ValueType.Object) markerType).getClassName());
                }
            }

            if ((flags & FLAG_BRIDGES) != 0) {
                int bridgeCount = callSite.getBootstrapArguments().get(bootstrapArgIndex++).getInt();
                for (int i = 0; i < bridgeCount; ++i) {
                    ValueType[] bridgeType = callSite.getBootstrapArguments().get(bootstrapArgIndex++).getMethodType();
                    createBridge(classSource, implementor, callSite.getCalledMethod().getName(), instantiatedMethodType,
                            bridgeType);
                }
            }
        }

        callSite.getAgent().submitClass(implementor);
        return callerPe.construct(ctor.getOwnerName(), callSite.getArguments().toArray(new ValueEmitter[0]));
    }

    private ValueEmitter invoke(ProgramEmitter pe, MethodHandle handle, ValueEmitter[] arguments) {
        switch (handle.getKind()) {
            case GET_FIELD:
                return arguments[0].getField(handle.getName(), handle.getValueType());
            case GET_STATIC_FIELD:
                return pe.getField(handle.getClassName(), handle.getName(), handle.getValueType());
            case PUT_FIELD:
                arguments[0].setField(handle.getName(), arguments[0].cast(handle.getValueType()));
                return null;
            case PUT_STATIC_FIELD:
                pe.setField(handle.getClassName(), handle.getName(), arguments[0].cast(handle.getValueType()));
                return null;
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
            case INVOKE_SPECIAL:
                for (int i = 1; i < arguments.length; ++i) {
                    arguments[i] = arguments[i].cast(handle.getArgumentType(i - 1));
                }
                arguments[0] = arguments[0].cast(ValueType.object(handle.getClassName()));
                return arguments[0].invokeVirtual(handle.getName(), handle.getValueType(),
                        Arrays.copyOfRange(arguments, 1, arguments.length));
            case INVOKE_STATIC:
                for (int i = 0; i < arguments.length; ++i) {
                    arguments[i] = arguments[i].cast(handle.getArgumentType(i));
                }
                return pe.invoke(handle.getClassName(), handle.getName(), handle.getValueType(), arguments);
            case INVOKE_CONSTRUCTOR:
                return pe.construct(handle.getClassName(), arguments);
            default:
                throw new IllegalArgumentException("Unexpected handle type: " + handle.getKind());
        }
    }

    private ValueEmitter tryConvertArgument(ValueEmitter arg, ValueType from, ValueType to) {
        if (from.equals(to)) {
            return arg;
        }
        if (from instanceof ValueType.Primitive && to instanceof ValueType.Primitive) {
            return arg.cast(to);
        } else if (from instanceof ValueType.Primitive && to instanceof ValueType.Object) {
            String primitiveClass = ((ValueType.Object) to).getClassName();
            PrimitiveType toType = getWrappedPrimitive(primitiveClass);
            if (toType == null) {
                return arg;
            }
            arg = tryConvertArgument(arg, from, ValueType.primitive(toType));
            return arg.getProgramEmitter().invoke(primitiveClass, "valueOf", ValueType.primitive(toType), arg);
        } else if (from instanceof ValueType.Object && to instanceof ValueType.Primitive) {
            String primitiveClass = ((ValueType.Object) from).getClassName();
            PrimitiveType fromType = getWrappedPrimitive(primitiveClass);
            if (fromType == null) {
                return arg;
            }
            arg = arg.invokeVirtual(primitiveName(fromType) + "Value", ValueType.primitive(fromType));
            return tryConvertArgument(arg, ValueType.primitive(fromType), to);
        } else {
            return arg.cast(to);
        }
    }

    private PrimitiveType getWrappedPrimitive(String name) {
        switch (name) {
            case "java.lang.Boolean":
                return PrimitiveType.BOOLEAN;
            case "java.lang.Byte":
                return PrimitiveType.BYTE;
            case "java.lang.Short":
                return PrimitiveType.SHORT;
            case "java.lang.Character":
                return PrimitiveType.CHARACTER;
            case "java.lang.Integer":
                return PrimitiveType.INTEGER;
            case "java.lang.Long":
                return PrimitiveType.LONG;
            case "java.lang.Float":
                return PrimitiveType.FLOAT;
            case "java.lang.Double":
                return PrimitiveType.DOUBLE;
            default:
                return null;
        }
    }

    private String primitiveName(PrimitiveType type) {
        switch (type) {
            case BOOLEAN:
                return "boolean";
            case BYTE:
                return "byte";
            case SHORT:
                return "short";
            case CHARACTER:
                return "char";
            case INTEGER:
                return "int";
            case LONG:
                return "long";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            default:
                throw new IllegalArgumentException("Unexpected primitive " + type);
        }
    }

    private ValueType[] getSignature(MethodHandle handle) {
        switch (handle.getKind()) {
            case GET_FIELD:
                return new ValueType[] { ValueType.object(handle.getClassName()), handle.getValueType() };
            case GET_STATIC_FIELD:
                return new ValueType[] { handle.getValueType() };
            case PUT_FIELD:
                return new ValueType[] { ValueType.object(handle.getClassName()), handle.getValueType(),
                        ValueType.VOID };
            case PUT_STATIC_FIELD:
                return new ValueType[] { handle.getValueType(), ValueType.VOID };
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
            case INVOKE_SPECIAL: {
                ValueType[] signature = handle.signature();
                ValueType[] result = new ValueType[signature.length + 1];
                System.arraycopy(signature, 0, result, 1, signature.length);
                result[0] = ValueType.object(handle.getClassName());
                return result;
            }
            default:
                return handle.signature();
        }
    }

    private MethodHolder createConstructor(ClassReaderSource classSource, ClassHolder implementor, ValueType[] types) {
        ValueType[] signature = Arrays.copyOf(types, types.length + 1);
        signature[types.length] = ValueType.VOID;
        MethodHolder ctor = new MethodHolder("<init>", signature);
        ctor.setLevel(AccessLevel.PUBLIC);

        ProgramEmitter pe = ProgramEmitter.create(ctor, classSource);
        ValueEmitter thisVar = pe.var(0, implementor);
        thisVar.invokeSpecial(implementor.getParent(), "<init>");

        for (int i = 0; i < types.length; ++i) {
            FieldHolder field = new FieldHolder("_" + i);
            field.setLevel(AccessLevel.PRIVATE);
            field.setType(types[i]);
            implementor.addField(field);
            thisVar.setField(field.getName(), pe.var(i + 1, types[i]));
        }

        pe.exit();
        implementor.addMethod(ctor);
        return ctor;
    }

    private void createBridge(ClassReaderSource classSource, ClassHolder implementor, String name, ValueType[] types,
            ValueType[] bridgeTypes) {
        if (Arrays.equals(types, bridgeTypes)) {
            return;
        }

        MethodHolder bridge = new MethodHolder(name, bridgeTypes);
        bridge.getAnnotations().add(new AnnotationHolder(NoCache.class.getName()));
        bridge.setLevel(AccessLevel.PUBLIC);
        bridge.getModifiers().add(ElementModifier.BRIDGE);
        ProgramEmitter pe = ProgramEmitter.create(bridge, classSource);
        ValueEmitter thisVar = pe.var(0, implementor);
        ValueEmitter[] arguments = new ValueEmitter[bridgeTypes.length - 1];
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = pe.var(i + 1, bridgeTypes[i]);
        }

        for (int i = 0; i < bridgeTypes.length - 1; ++i) {
            ValueType type = types[i];
            ValueType bridgeType = bridgeTypes[i];
            if (type.equals(bridgeType)) {
                continue;
            }
            arguments[i] = arguments[i].cast(type);
        }

        ValueEmitter result = thisVar.invokeVirtual(name, types[types.length - 1], arguments);
        if (result != null) {
            if (!types[types.length - 1].equals(bridgeTypes[bridgeTypes.length - 1])) {
                result = result.cast(bridgeTypes[bridgeTypes.length - 1]);
            }
            result.returnValue();
        } else {
            pe.exit();
        }

        implementor.addMethod(bridge);
    }
}
