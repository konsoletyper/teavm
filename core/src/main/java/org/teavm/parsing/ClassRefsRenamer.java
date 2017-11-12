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
import org.teavm.common.Mapper;
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
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class ClassRefsRenamer implements InstructionVisitor {
    private Mapper<String, String> classNameMapper;

    public ClassRefsRenamer(Mapper<String, String> classNameMapper) {
        this.classNameMapper = classNameMapper;
    }

    public ClassHolder rename(ClassHolder cls) {
        ClassHolder renamedCls = new ClassHolder(classNameMapper.map(cls.getName()));
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
        renamedCls.setParent(parent != null ? classNameMapper.map(parent) : null);
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
            renamedCls.setOwnerName(classNameMapper.map(cls.getOwnerName()));
        }
        rename(cls.getAnnotations(), renamedCls.getAnnotations());
        for (String iface : cls.getInterfaces()) {
            String mappedIfaceName = classNameMapper.map(iface);
            if (!mappedIfaceName.equals(renamedCls.getName())) {
                renamedCls.getInterfaces().add(mappedIfaceName);
            }
        }
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
        MethodHolder renamedMethod = new MethodHolder(methodName, signature);
        renamedMethod.getModifiers().addAll(method.getModifiers());
        renamedMethod.setLevel(method.getLevel());
        renamedMethod.setProgram(method.getProgram());
        rename(method.getAnnotations(), renamedMethod.getAnnotations());
        rename(renamedMethod.getProgram());
        return renamedMethod;
    }

    public FieldHolder rename(FieldHolder field) {
        FieldHolder renamedField = new FieldHolder(field.getName());
        renamedField.getModifiers().addAll(field.getModifiers());
        renamedField.setLevel(field.getLevel());
        renamedField.setType(rename(field.getType()));
        renamedField.setInitialValue(field.getInitialValue());
        rename(field.getAnnotations(), renamedField.getAnnotations());
        return renamedField;
    }

    private ValueType rename(ValueType type) {
        if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            return ValueType.arrayOf(rename(itemType));
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return ValueType.object(classNameMapper.map(className));
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
                return MethodHandle.fieldGetter(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case GET_STATIC_FIELD:
                return MethodHandle.staticFieldGetter(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case PUT_FIELD:
                return MethodHandle.fieldSetter(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case PUT_STATIC_FIELD:
                return MethodHandle.staticFieldSetter(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.getValueType()));
            case INVOKE_VIRTUAL:
                return MethodHandle.virtualCaller(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_STATIC:
                return MethodHandle.staticCaller(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_SPECIAL:
                return MethodHandle.specialCaller(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_CONSTRUCTOR:
                return MethodHandle.constructorCaller(classNameMapper.map(handle.getClassName()), handle.getName(),
                        rename(handle.signature()));
            case INVOKE_INTERFACE:
                return MethodHandle.interfaceCaller(classNameMapper.map(handle.getClassName()), handle.getName(),
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
        AnnotationHolder renamedAnnot = new AnnotationHolder(classNameMapper.map(annot.getType()));
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
                    tryCatch.setExceptionType(classNameMapper.map(tryCatch.getExceptionType()));
                }
            }
        }
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        insn.setConstant(rename(insn.getConstant()));
    }

    @Override
    public void visit(NullConstantInstruction insn) {
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
    }

    @Override
    public void visit(LongConstantInstruction insn) {
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
    }

    @Override
    public void visit(StringConstantInstruction insn) {
    }

    @Override
    public void visit(BinaryInstruction insn) {
    }

    @Override
    public void visit(NegateInstruction insn) {
    }

    @Override
    public void visit(AssignInstruction insn) {
    }

    @Override
    public void visit(CastInstruction insn) {
        insn.setTargetType(rename(insn.getTargetType()));
    }

    @Override
    public void visit(CastNumberInstruction insn) {
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
    }

    @Override
    public void visit(BranchingInstruction insn) {
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
    }

    @Override
    public void visit(JumpInstruction insn) {
    }

    @Override
    public void visit(SwitchInstruction insn) {
    }

    @Override
    public void visit(ExitInstruction insn) {
    }

    @Override
    public void visit(RaiseInstruction insn) {
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        insn.setItemType(rename(insn.getItemType()));
    }

    @Override
    public void visit(ConstructInstruction insn) {
        insn.setType(classNameMapper.map(insn.getType()));
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        insn.setItemType(rename(insn.getItemType()));
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        String className = classNameMapper.map(insn.getField().getClassName());
        insn.setField(new FieldReference(className, insn.getField().getFieldName()));
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        String className = classNameMapper.map(insn.getField().getClassName());
        insn.setField(new FieldReference(className, insn.getField().getFieldName()));
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
    }

    @Override
    public void visit(GetElementInstruction insn) {
    }

    @Override
    public void visit(PutElementInstruction insn) {
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
    }

    @Override
    public void visit(InvokeInstruction insn) {
        String className = classNameMapper.map(insn.getMethod().getClassName());
        ValueType[] signature = insn.getMethod().getSignature();
        for (int i = 0; i < signature.length; ++i) {
            signature[i] = rename(signature[i]);
        }
        insn.setMethod(new MethodReference(className, new MethodDescriptor(insn.getMethod().getName(), signature)));
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
        insn.setClassName(classNameMapper.map(insn.getClassName()));
    }

    @Override
    public void visit(NullCheckInstruction insn) {
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
    }
}
