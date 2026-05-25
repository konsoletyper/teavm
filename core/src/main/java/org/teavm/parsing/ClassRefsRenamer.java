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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
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
        var newName = classNameMapper.apply(cls.getName());
        var renamedCls = !newName.equals(cls.getName()) ? new ClassHolder(newName) : cls;
        if (renamedCls != cls) {
            renamedCls.getModifiers().addAll(cls.getModifiers());
            renamedCls.setLevel(cls.getLevel());
        }
        String parent = cls.getParent();
        var superclassAnnot = cls.getAnnotations().get(Superclass.class.getName());
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

        var methodsChanged = renamedCls != cls;
        var newMethods = new ArrayList<MethodHolder>();
        for (var method : cls.getMethods()) {
            if (method.getAnnotations().get(Remove.class.getName()) != null) {
                methodsChanged = true;
                continue;
            }
            var newMethod = rename(method);
            if (newMethod != method) {
                methodsChanged = true;
            }
            newMethods.add(newMethod);
        }
        if (methodsChanged) {
            cls.removeAllMethods();
            for (var method : newMethods) {
                renamedCls.addMethod(method);
            }
        }

        var fieldsChanged = cls != renamedCls;
        var newFields = new ArrayList<FieldHolder>();
        for (var field : cls.getFields()) {
            var newField = rename(field);
            if (newField != field) {
                fieldsChanged = true;
            }
            newFields.add(rename(field));
        }
        if (fieldsChanged) {
            cls.removeAllFields();
            for (var field : newFields) {
                renamedCls.addField(field);
            }
        }
        if (cls.getOwnerName() != null) {
            renamedCls.setOwnerName(classNameMapper.apply(cls.getOwnerName()));
        }
        if (cls.getDeclaringClassName() != null) {
            renamedCls.setDeclaringClassName(classNameMapper.apply(cls.getDeclaringClassName()));
        }
        rename(cls.getAnnotations(), renamedCls.getAnnotations());

        var interfacesChanged = cls != renamedCls;
        var newInterfaces = new ArrayList<String>();
        for (var iface : cls.getInterfaces()) {
            var mappedIfaceName = classNameMapper.apply(iface);
            if (mappedIfaceName.equals(renamedCls.getName())) {
                interfacesChanged = true;
                continue;
            }
            if (!mappedIfaceName.equals(iface)) {
                interfacesChanged = true;
            }
            newInterfaces.add(mappedIfaceName);
        }
        if (interfacesChanged) {
            renamedCls.getInterfaces().clear();
            renamedCls.getInterfaces().addAll(newInterfaces);
        }

        var genericParent = cls.getGenericParent();
        if (genericParent != null) {
            renamedCls.setGenericParent((GenericValueType.Object) rename(genericParent));
        }

        var genericInterfacesChanged = cls != renamedCls;
        var newGenericInterfaces = new ArrayList<GenericValueType.Object>();
        for (var genericInterface : cls.getGenericInterfaces()) {
            var newGenericInterface = (GenericValueType.Object) rename(genericInterface);
            if (newGenericInterface != genericInterface) {
                genericInterfacesChanged = true;
            }
            newGenericInterfaces.add(newGenericInterface);
        }
        if (genericInterfacesChanged) {
            renamedCls.getGenericInterfaces().clear();
            renamedCls.getGenericInterfaces().addAll(newGenericInterfaces);
        }

        if (cls.getGenericParameters() != null) {
            renamedCls.setGenericParameters(rename(cls.getGenericParameters()));
        }

        renamedCls.getInnerClasses().addAll(cls.getInnerClasses());
        renamedCls.getInnerClasses().replaceAll(classNameMapper::apply);

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

        var newDescriptor = new MethodDescriptor(methodName, signature);
        var renamedMethod = newDescriptor.equals(method.getDescriptor())
                ? method
                : new MethodHolder(referenceCache.getCached(newDescriptor));

        if (renamedMethod != method) {
            renamedMethod.getModifiers().addAll(method.getModifiers());
            renamedMethod.setLevel(method.getLevel());
            renamedMethod.setProgram(method.getProgram());
        }
        rename(method.getAnnotations(), renamedMethod.getAnnotations());
        for (int i = 0; i < method.parameterCount(); ++i) {
            rename(method.parameterAnnotation(i), renamedMethod.parameterAnnotation(i));
        }

        var thrownTypesChanged = renamedMethod != method;
        var newThrownTypes = new ArrayList<String>();
        for (var thrownType : method.getThrownTypes()) {
            var mappedThrownType = classNameMapper.apply(thrownType);
            if (!mappedThrownType.equals(thrownType)) {
                thrownTypesChanged = true;
            }
            newThrownTypes.add(mappedThrownType);
        }
        if (thrownTypesChanged) {
            method.setThrownTypes(newThrownTypes);
        }

        if (renamedMethod.getProgram() != null) {
            rename(renamedMethod.getProgram());
        }

        renamedMethod.setTypeParameters(rename(method.getTypeParameters()));
        GenericValueType genericResultType = method.getGenericResultType();
        var genericSignatureChanged = renamedMethod != method;
        if (genericResultType != null) {
            var newGenericResultType = rename(genericResultType);
            if (newGenericResultType != genericResultType) {
                genericSignatureChanged = true;
            }
            genericResultType = newGenericResultType;
        }
        var genericParameters = method.getGenericParameterTypes();
        if (genericParameters != null) {
            var newGenericParameters = new GenericValueType[genericParameters.length];
            for (int i = 0; i < genericParameters.length; ++i) {
                var newGenericParameter = rename(genericParameters[i]);
                if (newGenericParameter != genericParameters[i]) {
                    genericSignatureChanged = true;
                }
                newGenericParameters[i] = newGenericParameter;
            }
            genericParameters = newGenericParameters;
        }
        if (genericSignatureChanged) {
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

    private GenericTypeParameter rename(GenericTypeParameter typeParam) {
        var name = classNameMapper.apply(typeParam.getName());
        var renamedClassBound = typeParam.getClassBound() != null
                ? (GenericValueType.Reference) rename(typeParam.getClassBound())
                : null;
        var renamedInterfaceBounds = typeParam.getInterfaceBounds();
        var interfacesRenamed = false;
        for (var i = 0; i < renamedInterfaceBounds.length; ++i) {
            var bound = (GenericValueType.Reference) rename(renamedInterfaceBounds[i]);
            if (!bound.equals(renamedInterfaceBounds[i])) {
                interfacesRenamed = true;
                renamedInterfaceBounds[i] = bound;
            }
        }
        if (interfacesRenamed || !Objects.equals(renamedClassBound, typeParam.getClassBound())
                || name.equals(typeParam.getName())) {
            return new GenericTypeParameter(name, renamedClassBound, renamedInterfaceBounds);
        }
        return typeParam;
    }

    public FieldHolder rename(FieldHolder field) {
        field.setType(rename(field.getType()));
        field.setInitialValue(field.getInitialValue());
        rename(field.getAnnotations(), field.getAnnotations());

        var genericType = field.getGenericType();
        if (genericType != null) {
            field.setGenericType(rename(genericType));
        }

        return field;
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
        } else if (type instanceof GenericValueType.Object object) {
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
        return switch (cst.getKind()) {
            case RuntimeConstant.TYPE -> new RuntimeConstant(rename(cst.getValueType()));
            case RuntimeConstant.METHOD -> new RuntimeConstant(rename(cst.getMethodType()));
            case RuntimeConstant.METHOD_HANDLE -> new RuntimeConstant(rename(cst.getMethodHandle()));
            default -> cst;
        };
    }

    private MethodHandle rename(MethodHandle handle) {
        return switch (handle.getKind()) {
            case GET_FIELD -> MethodHandle.fieldGetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                    rename(handle.getValueType()));
            case GET_STATIC_FIELD ->
                    MethodHandle.staticFieldGetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.getValueType()));
            case PUT_FIELD -> MethodHandle.fieldSetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                    rename(handle.getValueType()));
            case PUT_STATIC_FIELD ->
                    MethodHandle.staticFieldSetter(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.getValueType()));
            case INVOKE_VIRTUAL ->
                    MethodHandle.virtualCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.signature()));
            case INVOKE_STATIC ->
                    MethodHandle.staticCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.signature()));
            case INVOKE_SPECIAL ->
                    MethodHandle.specialCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.signature()));
            case INVOKE_CONSTRUCTOR ->
                    MethodHandle.constructorCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.signature()));
            case INVOKE_INTERFACE ->
                    MethodHandle.interfaceCaller(classNameMapper.apply(handle.getClassName()), handle.getName(),
                            rename(handle.signature()));
        };
    }

    private void rename(AnnotationContainer source, AnnotationContainer target) {
        var newAnnotations = new ArrayList<AnnotationHolder>();
        var annotationsChanged = target != source;
        for (AnnotationHolder annot : source.all()) {
            if (annot.getType().equals(Rename.class.getName())
                    || annot.getType().equals(Superclass.class.getName())) {
                annotationsChanged = true;
                continue;
            }
            var newAnnot = rename(annot);
            if (newAnnot != annot) {
                annotationsChanged = true;
            }
            newAnnotations.add(newAnnot);
        }
        if (annotationsChanged) {
            target.removeAll();
            for (var newAnnot : newAnnotations) {
                target.add(newAnnot);
            }
        }
    }

    private AnnotationHolder rename(AnnotationHolder annot) {
        var newName = classNameMapper.apply(annot.getType());
        var renamedAnnot = newName.equals(annot.getType())
                ? annot
                : new AnnotationHolder(classNameMapper.apply(annot.getType()));
        if (renamedAnnot != annot) {
            for (var entry : annot.getValues().entrySet()) {
                renamedAnnot.getValues().put(entry.getKey(), rename(entry.getValue()));
            }
        } else {
            for (var entry : annot.getValues().entrySet()) {
                entry.setValue(rename(entry.getValue()));
            }
        }
        return renamedAnnot;
    }

    private AnnotationValue rename(AnnotationValue value) {
        return switch (value.getType()) {
            case AnnotationValue.ANNOTATION -> {
                var newAnnot = rename((AnnotationHolder) value.getAnnotation());
                yield newAnnot != value.getAnnotation()
                        ? new AnnotationValue(newAnnot)
                        : value;
            }
            case AnnotationValue.LIST -> {
                var newList = new ArrayList<AnnotationValue>();
                var changed = false;
                for (var item : value.getList()) {
                    var newItem = rename(item);
                    if (newItem != item) {
                        changed = true;
                    }
                    newList.add(newItem);
                }
                yield changed ? new AnnotationValue(newList) : value;
            }
            case AnnotationValue.CLASS -> {
                var newJavaClass = rename(value.getJavaClass());
                yield newJavaClass.equals(value.getJavaClass())
                        ? value
                        : new AnnotationValue(newJavaClass);
            }
            default -> value;
        };
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
