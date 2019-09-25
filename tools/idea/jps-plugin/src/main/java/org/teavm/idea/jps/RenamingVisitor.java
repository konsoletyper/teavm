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
package org.teavm.idea.jps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.teavm.asm.AnnotationVisitor;
import org.teavm.asm.ClassVisitor;
import org.teavm.asm.FieldVisitor;
import org.teavm.asm.Handle;
import org.teavm.asm.Label;
import org.teavm.asm.MethodVisitor;
import org.teavm.asm.Opcodes;
import org.teavm.asm.Type;
import org.teavm.asm.TypePath;

public class RenamingVisitor extends ClassVisitor {
    private List<Rename> renameList = new ArrayList<>();

    public RenamingVisitor(ClassVisitor inner) {
        super(Opcodes.ASM5, inner);
    }

    public void rename(String prefix, String newName) {
        renameList.add(new Rename(prefix, newName));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = renameClass(interfaces[i]);
        }
        super.visit(version, access, name, signature, renameClass(superName), interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        desc = renameValueDesc(desc);
        return new RenamingAnnotationVisitor(super.visitAnnotation(desc, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        desc = renameValueDesc(desc);
        return new RenamingAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        desc = renameMethodDesc(desc);
        return new RenamingMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new RenamingFieldVisitor(super.visitField(access, name, renameValueDesc(desc), signature, value));
    }

    class RenamingMethodVisitor extends MethodVisitor {
        RenamingMethodVisitor(MethodVisitor inner) {
            super(Opcodes.ASM5, inner);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start,
                Label[] end, int[] index, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitLocalVariableAnnotation(typeRef, typePath,
                    start, end, index, desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new RenamingAnnotationVisitor(super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new RenamingAnnotationVisitor(super.visitAnnotationDefault());
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitInsnAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitParameterAnnotation(parameter, desc, visible));
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            owner = renameClass(owner);
            desc = renameValueDesc(desc);
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                cst = Type.getType(renameType((Type) cst));
            }
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            desc = renameValueDesc(desc);
            super.visitLocalVariable(name, desc, signature, start, end, index);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            owner = renameClass(owner);
            desc = renameMethodDesc(desc);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (type.startsWith("[")) {
                type = renameValueDesc(type);
            } else {
                type = renameClass(type);
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            type = renameClass(type);
            super.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            for (int i = 0; i < local.length; ++i) {
                if (local[i] instanceof String) {
                    local[i] = renameClass((String) local[i]);
                }
            }
            for (int i = 0; i < stack.length; ++i) {
                if (stack[i] instanceof String) {
                    stack[i] = renameClass((String) stack[i]);
                }
            }
            super.visitFrame(type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            desc = renameMethodDesc(desc);
            bsm = renameHandle(bsm);
            for (int i = 0; i < bsmArgs.length; ++i) {
                Object arg = bsmArgs[i];
                if (arg instanceof Type) {
                    arg = Type.getType(renameType((Type) arg));
                } else if (arg instanceof Handle) {
                    arg = renameHandle((Handle) arg);
                }
                bsmArgs[i] = arg;
            }
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }

    class RenamingFieldVisitor extends FieldVisitor {
        RenamingFieldVisitor(FieldVisitor inner) {
            super(Opcodes.ASM5, inner);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }
    }

    class RenamingAnnotationVisitor extends AnnotationVisitor {
        RenamingAnnotationVisitor(AnnotationVisitor inner) {
            super(Opcodes.ASM5, inner);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new RenamingAnnotationVisitor(super.visitArray(name));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            desc = renameValueDesc(desc);
            return new RenamingAnnotationVisitor(super.visitAnnotation(name, desc));
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            desc = renameValueDesc(desc);
            super.visitEnum(name, desc, value);
        }
    }

    private String renameClass(String className) {
        if (className == null) {
            return null;
        }
        for (Rename rename : renameList) {
            if (className.startsWith(rename.prefix)) {
                return rename.renameTo + className.substring(rename.prefix.length());
            }
        }
        return className;
    }

    private Handle renameHandle(Handle handle) {
        String desc = handle.getDesc();
        desc = desc.startsWith("(") ? renameMethodDesc(desc) : renameValueDesc(desc);
        return new Handle(handle.getTag(), renameClass(handle.getOwner()), handle.getName(), desc);
    }

    private String renameValueDesc(String desc) {
        return renameType(Type.getType(desc));
    }

    private String renameMethodDesc(String desc) {
        return renameType(Type.getMethodType(desc));
    }

    private String renameType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                return "[" + renameValueDesc(type.getDescriptor().substring(1));
            case Type.METHOD:
                return "(" + Arrays.stream(type.getArgumentTypes()).map(this::renameType)
                        .collect(Collectors.joining("")) + ")" + renameType(type.getReturnType());
            case Type.OBJECT:
                return "L" + renameClass(type.getInternalName()) + ";";
            default:
                return type.getDescriptor();
        }
    }

    static class Rename {
        final String prefix;
        final String renameTo;

        Rename(String prefix, String renameTo) {
            this.prefix = prefix;
            this.renameTo = renameTo;
        }
    }
}
