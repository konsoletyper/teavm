/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.io.PrintStream;
import org.objectweb.asm.*;
import org.teavm.model.*;
import org.teavm.model.ClassReader;

/**
 *
 * @author Alexey Andreev
 */
class JCLComparisonVisitor implements ClassVisitor {
    private PrintStream out;
    private ClassReaderSource classSource;
    private boolean first = true;
    private boolean firstItem;
    private boolean pass;
    private boolean ended;
    private ClassReader classReader;

    public JCLComparisonVisitor(ClassReaderSource classSource, PrintStream out) {
        this.classSource = classSource;
        this.out = out;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ((access & Opcodes.ACC_PUBLIC) == 0) {
            return;
        }
        String javaName = name.replace('/', '.');
        if (!first) {
            out.println(",");
        }
        first = false;
        out.println("    \"" + javaName + "\" : {");
        classReader = classSource.get(javaName);
        if (classReader == null) {
            out.println("        \"implemented\" : false");
            pass = true;
        } else {
            out.println("        \"implemented\" : true,");
            out.println("        \"items\" : [");
            pass = false;
        }
        ended = false;
        firstItem = true;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (pass) {
            return null;
        }
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }
        if (!firstItem) {
            out.println(",");
        }
        firstItem = false;
        out.println("            {");
        out.println("                 \"type\" : \"field\",");
        out.println("                 \"name\" : \"" + name + "\",");
        out.println("                 \"descriptor\" : \"" + desc + "\",");
        FieldReader field = classReader.getField(name);
        out.println("                 \"implemented\" : \"" + (field != null ? "true" : "false") + "\",");
        out.print("            }");
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (pass) {
            return null;
        }
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }
        if (!firstItem) {
            out.println(",");
        }
        firstItem = false;
        out.println("            {");
        out.println("                 \"type\" : \"method\",");
        out.println("                 \"name\" : \"" + name + "\",");
        out.println("                 \"descriptor\" : \"" + desc + "\",");
        MethodReader method = classReader.getMethod(MethodDescriptor.parse(name + desc));
        out.println("                 \"implemented\" : \"" + (method != null ? "true" : "false") + "\",");
        out.print("            }");
        return null;
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    @Override
    public void visitEnd() {
        if (!ended) {
            if (!pass) {
                if (!firstItem) {
                    out.println();
                }
                out.println("        ]");
            }
            out.print("    }");
            ended = true;
        }
    }
}
