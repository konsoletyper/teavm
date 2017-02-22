/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.model.transformation;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.instructions.InitClassInstruction;

public class ClassInitializerInsertionTransformer {
    private static final MethodDescriptor clinitDescriptor = new MethodDescriptor("<clinit>", void.class);
    private ClassReaderSource classes;

    public ClassInitializerInsertionTransformer(ClassReaderSource classes) {
        this.classes = classes;
    }

    public void apply(MethodReader method, Program program) {
        ClassReader cls = classes.get(method.getOwnerName());
        boolean hasClinit = cls.getMethod(clinitDescriptor) != null;
        if (needsClinitCall(method) && hasClinit) {
            BasicBlock entryBlock = program.basicBlockAt(0);
            InitClassInstruction initInsn = new InitClassInstruction();
            initInsn.setClassName(method.getOwnerName());
            entryBlock.addFirst(initInsn);
        }
    }

    private static boolean needsClinitCall(MethodReader method) {
        if (method.getName().equals("<clinit>")) {
            return false;
        }
        if (method.getName().equals("<init>")) {
            return true;
        }
        return method.hasModifier(ElementModifier.STATIC);
    }
}
