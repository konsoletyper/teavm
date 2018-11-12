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
package org.teavm.metaprogramming.impl;

import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.teavm.metaprogramming.Action;
import org.teavm.metaprogramming.Computation;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.model.MethodReference;

public class MetaprogrammingInstrumentation {
    private static String lambdaMetafactory = LambdaMetafactory.class.getName().replace('.', '/');
    private static String proxyHelperType = Type.getInternalName(RuntimeHelper.class);
    private static String listDesc = Type.getDescriptor(List.class);

    public byte[] instrument(byte[] bytecode) {
        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(0);
        ClassTransformer transformer = new ClassTransformer(writer);
        reader.accept(transformer, 0);
        return writer.toByteArray();
    }

    class ClassTransformer extends ClassVisitor {
        ClassTransformer(ClassVisitor cv) {
            super(Opcodes.ASM7, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor innerVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodTransformer(innerVisitor);
        }
    }

    class MethodTransformer extends MethodVisitor {
        private boolean instrumented;

        MethodTransformer(MethodVisitor mv) {
            super(Opcodes.ASM7, mv);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (!bsm.getOwner().equals(lambdaMetafactory) || bsm.getName().equals("metafactory")) {
                Type returnType = Type.getReturnType(desc);
                if (returnType.getSort() == Type.OBJECT) {
                    if (returnType.getClassName().equals(Action.class.getName())) {
                        instrumented = true;
                        transformAction(desc, bsmArgs);
                        return;
                    } else if (returnType.getClassName().equals(Computation.class.getName())) {
                        instrumented = true;
                        transformComputation(desc, bsmArgs);
                        return;
                    }
                }
            }
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        private void transformAction(String desc, Object[] bsmArgs) {
            transformArguments(desc);
            transformMethod((Handle) bsmArgs[1]);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ActionImpl.class), "create",
                    "(" + Type.getDescriptor(List.class) + Type.getDescriptor(MethodReference.class) + ")"
                            + Type.getDescriptor(ActionImpl.class), false);
        }

        private void transformComputation(String desc, Object[] bsmArgs) {
            transformArguments(desc);
            transformMethod((Handle) bsmArgs[1]);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ComputationImpl.class), "create",
                    "(" + Type.getDescriptor(List.class) + Type.getDescriptor(MethodReference.class) + ")"
                            + Type.getDescriptor(ComputationImpl.class), false);
        }

        private void transformArguments(String desc) {
            Type[] argTypes = Type.getArgumentTypes(desc);
            String arrayListType = Type.getInternalName(ArrayList.class);

            super.visitTypeInsn(Opcodes.NEW, arrayListType);
            super.visitInsn(Opcodes.DUP);
            super.visitIntInsn(Opcodes.SIPUSH, argTypes.length);
            super.visitMethodInsn(Opcodes.INVOKESPECIAL, arrayListType, "<init>", "(I)V", false);

            for (int i = argTypes.length - 1; i >= 0; --i) {
                transformArgument(argTypes[i]);
            }

            super.visitInsn(Opcodes.DUP);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "reverse",
                    "(" + listDesc + ")V", false);
        }

        private void transformArgument(Type type) {
            switch (type.getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.SHORT:
                case Type.CHAR:
                case Type.INT:
                    transformArgument("I");
                    break;
                case Type.LONG:
                    transformArgument("L");
                    break;
                case Type.FLOAT:
                    transformArgument("F");
                    break;
                case Type.DOUBLE:
                    transformArgument("D");
                    break;
                default:
                    transformArgument("Ljava/lang/Object;");
                    break;
            }
        }

        private void transformArgument(String desc) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, proxyHelperType, "add",
                    "(" + desc + listDesc + ")" + listDesc, false);
        }

        private void transformMethod(Handle handle) {
            super.visitTypeInsn(Opcodes.NEW, Type.getInternalName(MethodReference.class));
            super.visitInsn(Opcodes.DUP);

            Type ownerType = Type.getType("L" + handle.getOwner() + ";");
            super.visitLdcInsn(ownerType);
            super.visitLdcInsn(handle.getName());

            Type[] argTypes = Type.getArgumentTypes(handle.getDesc());
            Type resultType = Type.getReturnType(handle.getDesc());
            super.visitIntInsn(Opcodes.SIPUSH, argTypes.length + 1);
            super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            for (int i = 0; i < argTypes.length; ++i) {
                super.visitInsn(Opcodes.DUP);
                super.visitIntInsn(Opcodes.SIPUSH, i);
                emitClassLiteral(argTypes[i]);
                super.visitInsn(Opcodes.AASTORE);
            }
            super.visitInsn(Opcodes.DUP);
            super.visitIntInsn(Opcodes.SIPUSH, argTypes.length);
            emitClassLiteral(resultType);
            super.visitInsn(Opcodes.AASTORE);

            super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(MethodReference.class),
                    "<init>", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)V", false);
        }

        private void emitClassLiteral(Type type) {
            switch (type.getSort()) {
                case Type.BOOLEAN:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.BYTE:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.SHORT:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.CHAR:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.INT:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.LONG:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.FLOAT:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.DOUBLE:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
                    break;
                case Type.VOID:
                    super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
                    break;
                default:
                    super.visitLdcInsn(type);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC && owner.equals(Type.getInternalName(Metaprogramming.class))) {
                owner = Type.getInternalName(MetaprogrammingImpl.class);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(instrumented ? maxStack + 9 : maxStack, maxLocals);
        }
    }
}
