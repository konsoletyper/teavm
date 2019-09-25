/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.tools.classlibcomparison;

import java.util.Map;
import org.objectweb.asm.*;
import org.teavm.model.*;
import org.teavm.model.ClassReader;

class JCLComparisonVisitor extends ClassVisitor {
    private Map<String, JCLPackage> packageMap;
    private ClassReaderSource classSource;
    private ClassReader classReader;
    private JCLPackage jclPackage;
    private JCLClass jclClass;

    public JCLComparisonVisitor(ClassReaderSource classSource, Map<String, JCLPackage> packageMap) {
        super(Opcodes.ASM7);
        this.classSource = classSource;
        this.packageMap = packageMap;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            jclClass = null;
            classReader = null;
            return;
        }
        String javaName = name.replace('/', '.');
        int dotIndex = javaName.lastIndexOf('.');
        String packageName = javaName.substring(0, dotIndex);
        String simpleName = javaName.substring(dotIndex + 1);
        jclPackage = packageMap.get(packageName);
        if (jclPackage == null) {
            jclPackage = new JCLPackage(packageName);
            jclPackage.status = JCLStatus.FOUND;
            packageMap.put(packageName, jclPackage);
        }
        classReader = classSource.get(javaName);
        jclClass = new JCLClass(simpleName);
        jclClass.status = classReader != null ? JCLStatus.FOUND : JCLStatus.MISSING;
        jclClass.visibility = (access & Opcodes.ACC_PROTECTED) != 0 ? JCLVisibility.PROTECTED : JCLVisibility.PUBLIC;
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            jclClass.type = JCLClassType.ANNOTATION;
        } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
            jclClass.type = JCLClassType.INTERFACE;
        } else if ((access & Opcodes.ACC_ENUM) != 0) {
            jclClass.type = JCLClassType.ENUM;
        } else {
            jclClass.type = JCLClassType.CLASS;
        }
        jclPackage.classes.add(jclClass);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (classReader == null || (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }
        JCLItem item = new JCLItem(JCLItemType.FIELD, name + ":" + desc);
        FieldReader field = classReader.getField(name);
        item.status = field != null ? JCLStatus.FOUND : JCLStatus.MISSING;
        item.visibility = (access & Opcodes.ACC_PROTECTED) != 0 ? JCLVisibility.PROTECTED : JCLVisibility.PUBLIC;
        jclClass.items.add(item);
        if (item.status == JCLStatus.MISSING) {
            jclClass.status = JCLStatus.PARTIAL;
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (classReader == null || (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }

        JCLItem item = new JCLItem(JCLItemType.METHOD, name + desc);
        MethodReader method = findMethod(classReader, MethodDescriptor.parse(name + desc));
        if (method == null) {
            item.status = JCLStatus.MISSING;
        } else {
            if ((access & Opcodes.ACC_ABSTRACT) == 0 && method.hasModifier(ElementModifier.ABSTRACT)) {
                item.status = JCLStatus.MISSING;
            } else {
                item.status = method.getOwnerName().equals(classReader.getName())
                        ? JCLStatus.FOUND : JCLStatus.PARTIAL;
            }
        }
        item.visibility = (access & Opcodes.ACC_PROTECTED) != 0 ? JCLVisibility.PROTECTED : JCLVisibility.PUBLIC;
        jclClass.items.add(item);
        if (item.status == JCLStatus.MISSING) {
            jclClass.status = JCLStatus.PARTIAL;
        }
        return null;
    }

    private MethodReader findMethod(ClassReader cls, MethodDescriptor desc) {
        MethodReader method = cls.getMethod(desc);
        if (method != null) {
            return method;
        }
        if (cls.getParent() != null) {
            ClassReader parent = classSource.get(cls.getParent());
            if (parent != null) {
                method = findMethod(parent, desc);
                if (method != null) {
                    return method;
                }
            }
        }
        for (String ifaceName : cls.getInterfaces()) {
            ClassReader iface = classSource.get(ifaceName);
            if (iface != null) {
                method = findMethod(iface, desc);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    @Override
    public void visitNestHost(String nestHost) {
        if (jclClass != null) {
            jclClass.outer = nestHost.replace('/', '.');
        }
    }
}
