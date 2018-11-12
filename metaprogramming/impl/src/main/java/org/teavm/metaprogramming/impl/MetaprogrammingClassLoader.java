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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.teavm.metaprogramming.CompileTime;

public class MetaprogrammingClassLoader extends ClassLoader {
    private MetaprogrammingInstrumentation instrumentation = new MetaprogrammingInstrumentation();
    private Map<String, Boolean> compileTimeClasses = new HashMap<>();
    private Map<String, Boolean> compileTimePackages = new HashMap<>();

    public MetaprogrammingClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isCompileTimeClass(name)) {
            return super.loadClass(name, resolve);
        } else {
            try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                byte[] array = instrumentation.instrument(IOUtils.toByteArray(new BufferedInputStream(input)));
                return defineClass(name, array, 0, array.length);
            } catch (IOException e) {
                throw new ClassNotFoundException("Error reading bytecode of class " + name, e);
            }
        }
    }

    public boolean isCompileTimeClass(String name) {
        Boolean result = compileTimeClasses.get(name);
        if (result == null) {
            result = checkIfCompileTime(name);
            compileTimeClasses.put(name, result);
        }
        return result;
    }

    private boolean checkIfCompileTime(String name) {
        String packageName = name;
        while (true) {
            int index = packageName.lastIndexOf('.');
            if (index < 0) {
                break;
            }
            packageName = packageName.substring(0, index);
            if (isCompileTimePackage(packageName)) {
                return true;
            }
        }

        String outerName = name;
        while (true) {
            int index = outerName.lastIndexOf('$');
            if (index < 0) {
                break;
            }
            outerName = outerName.substring(0, index);
            if (isCompileTimeClass(outerName)) {
                return true;
            }
        }

        CompileTimeClassVisitor visitor = new CompileTimeClassVisitor();
        try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) {
                return false;
            }
            new ClassReader(new BufferedInputStream(input))
                    .accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        } catch (IOException e) {
            return false;
        }
        if (visitor.compileTime) {
            return true;
        }

        if (visitor.parent != null && !visitor.parent.equals(name)) {
            return isCompileTimeClass(visitor.parent);
        }

        return false;
    }

    private boolean isCompileTimePackage(String name) {
        return compileTimePackages.computeIfAbsent(name, n -> checkIfCompileTimePackage(n));
    }

    private boolean checkIfCompileTimePackage(String name) {
        CompileTimeClassVisitor visitor = new CompileTimeClassVisitor();
        try (InputStream input = getResourceAsStream(name.replace('.', '/') + "/package-info.class")) {
            if (input == null) {
                return false;
            }
            new ClassReader(new BufferedInputStream(input))
                    .accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        } catch (IOException e) {
            return false;
        }
        return visitor.compileTime;
    }

    static class CompileTimeClassVisitor extends ClassVisitor {
        String parent;
        boolean compileTime;

        CompileTimeClassVisitor() {
            super(Opcodes.ASM7, null);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            this.parent = superName != null ? superName.replace('/', '.') : null;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.equals(Type.getDescriptor(CompileTime.class))) {
                compileTime = true;
            }
            return null;
        }
    }
}
