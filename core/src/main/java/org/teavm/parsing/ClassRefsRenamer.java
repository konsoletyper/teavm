/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.parsing;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import org.teavm.interop.Remove;
import org.teavm.interop.Rename;
import org.teavm.interop.Superclass;
import org.teavm.model.AnnotationContainer;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class ClassRefsRenamer extends AbstractInstructionVisitor {
    private ReferenceCache referenceCache;
    private Function<String, String> classNameMapper;

    public ClassRefsRenamer(ReferenceCache referenceCache, Function<String, String> classNameMapper) {
        this.referenceCache = referenceCache;
        this.classNameMapper = classNameMapper;
    }

    public ClassHolder rename(ClassHolder cls) {
        ClassHolder renamedCls = new ClassHolder(classNameMapper.apply(cls.getName()));
        renamedCls.getModifiers().addAll(cls.getModifiers());
        renamedCls.setLevel(cls.getLevel());
        String parent = cls.getParent();
        AnnotationHolder superclassAnnot = cls.getAnnotations().get(Superclass.class.getName());
        if (superclassAnnot != null) {
            parent = superclassAnnot.getValues().get("value").getString();
            if (parent.isEmpty()) {
                parent = null;
            }
        }
        renamedCls.setParent(parent != null ? classNameMapper.apply(parent) : null);
        if (renamedCls.getName().equals(renamedCls.getParent())) {
            renamedCls.setParent(null);
        }
        for (MethodHolder method : cls.getMethods()) {
            if (method.getAnnotations().get(Remove.class.getName()) != null) {
                continue;
            }
            renamedCls.addMethod(rename(method));
        }
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            renamedCls.addField(rename(field));
        }
        if (cls.getOwnerName() != null) {
            renamedCls.setOwnerName(classNameMapper.apply(cls.getOwnerName()));
        }
        if (cls.getDeclaringClassName() != null) {
            renamedCls.setDeclaringClassName(classNameMapper.apply(cls.getDeclaringClassName()));
        }
        rename(cls.getAnnotations(), renamedCls.getAnnotations());
        for (String iface : cls.getInterfaces()) {
            String mappedIfaceName = classNameMapper.apply(iface);
            if (!mappedIfaceName.equals(renamedCls.getName())) {
                renamedCls.getInterfaces().add(mappedIfaceName);
            }
        }

        GenericValueType.Object genericParent = cls.getGenericParent();
        if (genericParent != null) {
            renamedCls.setGenericParent((GenericValueType.Object) rename(genericParent));
        }
        for (GenericValueType.Object genericInterface : cls.getGenericInterfaces()) {
            renamedCls.getGenericInterfaces().add((GenericValueType.Object) rename(genericInterface));
        }

        renamedCls.setGenericParameters(cls.getGenericParameters());

        return renamedCls;
    }

    public MethodHolder rename(MethodHolder method) {
        String methodName = method.getName();
        AnnotationHolder renameAnnot = method.getAnnotations().get(Rename.class.getName());
        if (renameAnnot != null) {
            methodName = renameAnnot.getValues().get("value").getString();
        }
        ValueType[] signature = method.getSignature();
        for (int i = 0; i < signature.length; ++i) {
            signature[i] = rename(signature[i]);
        }
        MethodHolder renamedMethod = new MethodHolder(referenceCache.getCached(
                new MethodDescriptor(methodName, signature)));
        renamedMethod.getModifiers().addAll(method.getModifiers());
        renamedMethod.setLevel(method.getLevel());
        renamedMethod.setProgram(method.getProgram());
        rename(method.getAnnotations(), renamedMethod.getAnnotations());
        for (int i = 0; i < method.parameterCount(); ++i) {
            rename(method.parameterAnnotation(i), renamedMethod.parameterAnnotation(i));
        }

        if (renamedMethod.getProgram() != null) {
            rename(renamedMethod.getProgram());
        }

        renamedMethod.setTypeParameters(rename(method.getTypeParameters()));
        GenericValueType genericResultType = method.getGenericResultType();
        if (genericResultType != null) {
            genericResultType = rename(method.getGenericResultType());
        }
        GenericValueType[] genericParameters = new GenericValueType[method.genericParameterCount()];
        for (int i = 0; i < genericParameters.length; ++i) {
            genericParameters[i] = rename(method.genericParameterType(i));
        }
        if (genericResultType != null) {
            renamedMethod.setGenericSignature(genericResultType, genericParameters);
        }

        return renamedMethod;
    }

    private GenericTypeParameter[] rename(GenericTypeParameter[] typeParameters) {
        for (int i = 0; i < typeParameters.length; ++i) {
            typeParameters[i] = rename(typeParameters[i]);
        }
        return typeParameters;
    }

    private GenericTypeParameter rename(GenericTypeParameter typeParameter) {
        GenericValueType.Reference classBound = typeParameter.getClassBound();
        if (classBound != null) {
            classBound = (GenericValueType.Reference) rename(classBound);
        }
        GenericValueType.Reference[] interfaceBounds = typeParameter.getInterfaceBounds();
        for (int j = 0; j < interfaceBounds.length; ++j) {
            interfaceBounds[j] = (GenericValueType.Reference) rename(interfaceBounds[j]);
        }
        return new GenericTypeParameter(typeParameter.getName(), classBound, interfaceBounds);
    }

    public FieldHolder rename(FieldHolder field) {
        FieldHolder renamedField = new FieldHolder(field.getName());
        renamedField.getModifiers().addAll(field.getModifiers());
        renamedField.setLevel(field.getLevel());
        renamedField.setType(rename(field.getType()));
        renamedField.setInitialValue(field.getInitialValue());
        rename(field.getAnnotations(), renamedField.getAnnotations());

        GenericValueType genericType = field.getGenericType();
        if (genericType != null) {
            renamedField.setGenericType(rename(genericType));
        }

        return renamedField;
    }

    private ValueType rename(ValueType type) {
        if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            return referenceCache.getCached(ValueType.arrayOf(rename(itemType)));
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return referenceCache.getCached(ValueType.object(classNameMapper.apply(className)));
        } else {
            return type;
        }
    }

    private GenericValueType rename(GenericValueType type) {
        if (type instanceof GenericValueType.Array) {
            GenericValueType itemType = ((GenericValueType.Array) type).getItemType();
            return referenceCache.getCached(new GenericValueType.Array(rename(itemType)));
        } else if (type instanceof GenericValueType.Object) {
            GenericValueType.Object object = (GenericValueType.Object) type;
            String className = classNameMapper.apply(object.getClassName());
            GenericValueType.Object parent = object.getParent();
            if (parent != null) {
                parent = (GenericValueType.Object) rename(parent);
            }
            GenericValueType.Argument[] arguments = object.getArguments();
            for (int i = 0; i < arguments.length; ++i) {
                GenericValueType.Argument argument = arguments[i];
                GenericValueType.Reference value = argument.getValue();
                if (value != null) {
                    value = (GenericValueType.Reference) rename(value);
                }
                switch (argument.getKind()) {
                    case INVARIANT:
                        arguments[i] = GenericValueType.Argument.invariant(value);
                        break;
                    case COVARIANT:
                        arguments[i] = GenericValueType.Argument.covariant(value);
                        break;
                    case CONTRAVARIANT:
                        arguments[i] = GenericValueType.Argument.contravariant(value);
                        break;
                    default:
                        break;
                }
            }

            return referenceCache.getCached(new GenericValueType.Object(parent, className, arguments));
        } else {
            return type;
        }
    }

    private ValueType[] rename(ValueType[] types) {
        return Arrays.stream(types).map(this::rename).toArray(ValueType[]::new);
    }

    private RuntimeConstant rename(RuntimeConstant cst) {
        switch (cst.getKind()) {
            case RuntimeConstant.TYPE:
                return new RuntimeConstant(rename(cst.getValueType()));
            case RuntimeConstant.METHOD:
                return new RuntimeConstant(rename(cst.getMethodType()));
            case RuntimeConstant.METHOD_HANDLE:
                return new RuntimeConstant(rename(cst.getMethodHandle()));
            default:
                return cst;
        }
    }

    private MethodHandle rename(MethodHandle handle) {
        switch (handle.getKind()) {
            case GET_FIELD:
                return MethodHandle.fieldGetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case GET_STATIC_FIELD:
                return MethodHandle.staticFieldGetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case PUT_FIELD:
                return MethodHandle.fieldSetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case PUT_STATIC_FIELD:
                return MethodHandle.staticFieldSetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case INVOKE_VIRTUAL:
                return MethodHandle.virtualCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_STATIC:
                return MethodHandle.staticCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_SPECIAL:
                return MethodHandle.specialCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_CONSTRUCTOR:
                return MethodHandle.constructorCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_INTERFACE:
                return MethodHandle.interfaceCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            default:
                break;
        }
        throw new IllegalArgumentException("Unknown method handle type: " + handle.getKind());
    }

    private void rename(AnnotationContainer source, AnnotationContainer target) {
        for (AnnotationHolder annot : source.all()) {
            if (!annot.getType().equals(Rename.class.getName())
                    && !annot.getType().equals(Superclass.class.getName())) {
                target.add(rename(annot));
            }
        }
    }

    private AnnotationHolder rename(AnnotationHolder annot) {
        AnnotationHolder renamedAnnot = new AnnotationHolder(classNameMapper.apply(annot.getType()));
        for (Map.Entry<String, AnnotationValue> entry : annot.getValues().entrySet()) {
            renamedAnnot.getValues().put(entry.getKey(), entry.getValue());
        }
        return renamedAnnot;
    }

    public void rename(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock basicBlock = program.basicBlockAt(i);
            for (Instruction insn : basicBlock) {
                insn.acceptVisitor(this);
            }
            for (TryCatchBlock tryCatch : basicBlock.getTryCatchBlocks()) {
                if (tryCatch.getExceptionType() != null) {
                    tryCatch.setExceptionType(classNameMapper.apply(tryCatch.getExceptionType()));
                }
            }
        }
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        insn.setConstant(rename(insn.getConstant()));
    }

    @Override
    public void visit(CastInstruction insn) {
        insn.setTargetType(rename(insn.getTargetType()));
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        insn.setItemType(rename(insn.getItemType()));
    }

    @Override
    public void visit(ConstructInstruction insn) {
        insn.setType(classNameMapper.apply(insn.getType()));
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        insn.setItemType(rename(insn.getItemType()));
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        String className = classNameMapper.apply(insn.getField().getClassName());
        insn.setField(referenceCache.getCached(new FieldReference(className, insn.getField().getFieldName())));
        insn.setFieldType(rename(insn.getFieldType()));
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        String className = classNameMapper.apply(insn.getField().getClassName());
        if (className != insn.getField().getClassName()) {
            insn.setField(referenceCache.getCached(new FieldReference(className, insn.getField().getFieldName())));
        }
        insn.setFieldType(rename(insn.getFieldType()));
    }
    @Override
    public void visit(InvokeInstruction insn) {
        String className = classNameMapper.apply(insn.getMethod().getClassName());
        ValueType[] signature = insn.getMethod().getSignature();
        boolean changed = true;
        for (int i = 0; i < signature.length; ++i) {
            ValueType type = signature[i];
            ValueType newType = rename(type);
            if (newType != null) {
                changed = true;
            }
            signature[i] = newType;
        }
        if (changed) {
            insn.setMethod(referenceCache.getCached(className,
                    new MethodDescriptor(insn.getMethod().getName(), signature)));
        }
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        ValueType[] signature = insn.getMethod().getSignature();
        for (int i = 0; i < signature.length; ++i) {
            signature[i] = rename(signature[i]);
        }
        insn.setMethod(new MethodDescriptor(insn.getMethod().getName(), signature));
        for (int i = 0; i < insn.getBootstrapArguments().size(); ++i) {
            insn.getBootstrapArguments().set(i, rename(insn.getBootstrapArguments().get(i)));
        }
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        insn.setType(rename(insn.getType()));
    }

    @Override
    public void visit(InitClassInstruction insn) {
        insn.setClassName(classNameMapper.apply(insn.getClassName()));
    }
}
