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

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHandleType;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.InvocationType;

public class LambdaMetafactorySubstitutor implements BootstrapMethodSubstitutor {
    private static final int FLAG_SERIALIZABLE = 1;
    private static final int FLAG_MARKERS = 2;
    private static final int FLAG_BRIDGES = 4;
    private Map<MethodReference, Integer> lambdaIdsByMethod = new HashMap<>();
    private Map<MethodDescriptor, MethodDescriptor> descriptorCache = new HashMap<>();
    private List<String> fieldNameCache = new ArrayList<>();

    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter callerPe) {
        ValueType[] invokedType = callSite.getCalledMethod().getSignature();
        ValueType[] samMethodType = callSite.getBootstrapArguments().get(0).getMethodType();
        MethodHandle implMethod = callSite.getBootstrapArguments().get(1).getMethodHandle();
        ValueType[] instantiatedMethodType = callSite.getBootstrapArguments().get(2).getMethodType();

        ValueType.Object lambdaInterfaceType = (ValueType.Object) callSite.getCalledMethod().getResultType();
        String samName = lambdaInterfaceType.getClassName();
        ClassHierarchy hierarchy = callSite.getAgent().getClassHierarchy();
        ClassReader samClass = hierarchy.getClassSource().get(samName);

        String key = callSite.getCaller().getClassName() + "$" + callSite.getCaller().getName();
        ClassReaderSource classSource = callSite.getAgent().getClassSource();
        ClassReader callerClass = classSource.get(callSite.getCaller().getClassName());
        int id = 0;
        for (MethodReader callerMethod : callerClass.getMethods()) {
            if (callerMethod.getDescriptor().equals(callSite.getCaller().getDescriptor())) {
                break;
            }
            ++id;
        }

        int subId = lambdaIdsByMethod.getOrDefault(callSite.getCaller(), 0);
        ClassHolder implementor = new ClassHolder(key + "$lambda$_" + id + "_" + subId);
        lambdaIdsByMethod.put(callSite.getCaller(), subId + 1);

        implementor.setLevel(AccessLevel.PUBLIC);
        if (samClass != null && samClass.hasModifier(ElementModifier.INTERFACE)) {
            implementor.setParent("java.lang.Object");
            implementor.getInterfaces().add(samName);
        } else {
            implementor.setParent(samName);
        }

        int capturedVarCount = callSite.getCalledMethod().parameterCount();
        MethodHolder ctor = createConstructor(hierarchy, implementor,
                Arrays.copyOfRange(invokedType, 0, capturedVarCount), callerPe.getCurrentLocation());
        createBridge(hierarchy, implementor, callSite.getCalledMethod().getName(), instantiatedMethodType,
                samMethodType, callerPe.getCurrentLocation());

        MethodHolder worker = new MethodHolder(callSite.getCalledMethod().getName(), instantiatedMethodType);
        worker.setLevel(AccessLevel.PUBLIC);
        ProgramEmitter pe = ProgramEmitter.create(worker, callSite.getAgent().getClassHierarchy());
        pe.setCurrentLocation(callerPe.getCurrentLocation());
        ValueEmitter thisVar = pe.var(0, implementor);
        ValueEmitter[] arguments = new ValueEmitter[instantiatedMethodType.length - 1];
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = pe.var(i + 1, instantiatedMethodType[i]);
        }

        ValueType[] implementorSignature = getSignature(implMethod);
        ValueEmitter[] passedArguments = new ValueEmitter[implementorSignature.length - 1];
        for (int i = 0; i < capturedVarCount; ++i) {
            passedArguments[i] = thisVar.getField(fieldName(i), invokedType[i]);
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
                String functionInterfaceMethodName = callSite.getCalledMethod().getName();
                addWriteReplaceMethod(
                    callerPe.getCurrentLocation(),
                    hierarchy,
                    implementor,
                    ValueType.object(callSite.getCaller().getClassName()),
                    lambdaInterfaceType,
                    new MethodDescriptor(functionInterfaceMethodName, samMethodType),
                    implMethod.getKind(),
                    ValueType.object(implMethod.getClassName()),
                    new MethodDescriptor(implMethod.getName(), implMethod.signature()),
                    new MethodDescriptor(functionInterfaceMethodName, instantiatedMethodType)
                );
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
                    createBridge(hierarchy, implementor, callSite.getCalledMethod().getName(), instantiatedMethodType,
                            bridgeType, callerPe.getCurrentLocation());
                }
            }
        }

        List<String> dependencies = new ArrayList<>();
        dependencies.add(callSite.getCaller().getClassName());
        dependencies.addAll(implementor.getInterfaces());
        if (!implementor.getParent().equals("java.lang.Object")) {
            dependencies.add(implementor.getParent());
        }

        callSite.getAgent().submitClass(implementor);
        callSite.getAgent().getIncrementalCache().addDependencies(implementor.getName(),
                dependencies.toArray(new String[0]));

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
            case INVOKE_SPECIAL: {
                for (int i = 1; i < arguments.length; ++i) {
                    arguments[i] = arguments[i].cast(handle.getArgumentType(i - 1));
                }
                arguments[0] = arguments[0].cast(ValueType.object(handle.getClassName()));
                InvocationType type = handle.getKind() == MethodHandleType.INVOKE_SPECIAL
                        ? InvocationType.SPECIAL
                        : InvocationType.VIRTUAL;
                return arguments[0].invoke(type, handle.getName(), handle.getValueType(),
                        Arrays.copyOfRange(arguments, 1, arguments.length));
            }
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
            return arg.getProgramEmitter().invoke(primitiveClass, "valueOf", to, arg);
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

    private MethodHolder createConstructor(ClassHierarchy hierarchy, ClassHolder implementor, ValueType[] types,
            TextLocation location) {
        ValueType[] signature = Arrays.copyOf(types, types.length + 1);
        signature[types.length] = ValueType.VOID;
        MethodDescriptor descriptor = descriptorCache.computeIfAbsent(new MethodDescriptor("<init>", signature),
                k -> k);
        MethodHolder ctor = new MethodHolder(descriptor);
        ctor.setLevel(AccessLevel.PUBLIC);

        ProgramEmitter pe = ProgramEmitter.create(ctor, hierarchy);
        pe.setCurrentLocation(location);
        ValueEmitter thisVar = pe.var(0, implementor);
        thisVar.invokeSpecial(implementor.getParent(), "<init>");

        for (int i = 0; i < types.length; ++i) {
            FieldHolder field = new FieldHolder(fieldName(i));
            field.setLevel(AccessLevel.PRIVATE);
            field.setType(types[i]);
            implementor.addField(field);
            thisVar.setField(field.getName(), pe.var(i + 1, types[i]));
        }

        pe.exit();
        implementor.addMethod(ctor);
        return ctor;
    }

    private String fieldName(int index) {
        if (index >= fieldNameCache.size()) {
            fieldNameCache.addAll(Collections.nCopies(index - fieldNameCache.size() + 1, null));
        }
        String result = fieldNameCache.get(index);
        if (result == null) {
            result = "_" + index;
            fieldNameCache.set(index, result);
        }
        return result;
    }

    private void createBridge(ClassHierarchy hierarchy, ClassHolder implementor, String name, ValueType[] types,
            ValueType[] bridgeTypes, TextLocation location) {
        if (Arrays.equals(types, bridgeTypes)) {
            return;
        }

        MethodHolder bridge = new MethodHolder(name, bridgeTypes);
        bridge.setLevel(AccessLevel.PUBLIC);
        bridge.getModifiers().add(ElementModifier.BRIDGE);
        ProgramEmitter pe = ProgramEmitter.create(bridge, hierarchy);
        pe.setCurrentLocation(location);
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

        ValueEmitter result = thisVar.invokeSpecial(name, types[types.length - 1], arguments);
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

    private static void addWriteReplaceMethod(
        TextLocation location,
        ClassHierarchy classHierarchy,
        ClassHolder lambdaClassDefinition,
        ValueType.Object capturingClass,
        ValueType.Object functionalInterfaceClass,
        MethodDescriptor functionalInterfaceMethodDescriptor,
        MethodHandleType implMethodKind,
        ValueType.Object implClass,
        MethodDescriptor implMethodDescriptor,
        MethodDescriptor instantiatedMethodDescriptor
    ) {
        MethodHolder writeReplace =
                new MethodHolder("writeReplace", new ValueType.Object(SerializedLambda.class.getName()));
        writeReplace.setLevel(AccessLevel.PRIVATE);
        writeReplace.getModifiers().add(ElementModifier.FINAL);
        ProgramEmitter programEmitter = ProgramEmitter.create(writeReplace, classHierarchy);
        programEmitter.setCurrentLocation(location);
        Collection<FieldHolder> fields = lambdaClassDefinition.getFields();
        ValueEmitter capturedParametersArray = programEmitter.constructArray(Object.class, fields.size());
        ValueEmitter lambdaThis = programEmitter.var(0, lambdaClassDefinition);
        int index = 0;
        for (FieldHolder fieldHolder : fields) {
            ValueType fieldType = fieldHolder.getType();
            ValueEmitter fieldValue = lambdaThis.getField(fieldHolder.getName(), fieldType);
            if (fieldType instanceof ValueType.Primitive) {
                fieldValue = fieldValue.cast(((ValueType.Primitive) fieldType).getBoxedType());
            }
            capturedParametersArray.setElement(index++, fieldValue);
        }
        ValueEmitter newSerializedLambda = programEmitter.construct(
            SerializedLambda.class,
            programEmitter.constant(capturingClass),
            programEmitter.constant(functionalInterfaceClass.getClassName().replace('.', '/')),
            programEmitter.constant(functionalInterfaceMethodDescriptor.getName()),
            programEmitter.constant(functionalInterfaceMethodDescriptor.signatureToString()),
            programEmitter.constant(implMethodKind.getReferenceKind()),
            programEmitter.constant(implClass.getClassName().replace('.', '/')),
            programEmitter.constant(implMethodDescriptor.getName()),
            programEmitter.constant(implMethodDescriptor.signatureToString()),
            programEmitter.constant(instantiatedMethodDescriptor.signatureToString()),
            capturedParametersArray
        );
        newSerializedLambda.returnValue();
        lambdaClassDefinition.addMethod(writeReplace);
    }
}
