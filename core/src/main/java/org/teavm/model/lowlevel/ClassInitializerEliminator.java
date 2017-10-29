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
package org.teavm.model.lowlevel;

import java.util.HashMap;
import java.util.Map;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Program;
import org.teavm.model.instructions.InitClassInstruction;

public class ClassInitializerEliminator {
    private ClassReaderSource unprocessedClassSource;
    private Map<String, Boolean> cache = new HashMap<>();

    public ClassInitializerEliminator(ClassReaderSource unprocessedClassSource) {
        this.unprocessedClassSource = unprocessedClassSource;
    }

    public void apply(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof InitClassInstruction) {
                    if (!filter(((InitClassInstruction) insn).getClassName())) {
                        insn.delete();
                    }
                }
            }
        }
    }

    private boolean filter(String className) {
        return cache.computeIfAbsent(className, key -> clinitNeeded(key) && !isStaticInit(key) && !isStructure(key));
    }

    private boolean clinitNeeded(String className) {
        ClassReader cls = unprocessedClassSource.get(className);
        if (cls == null) {
            return true;
        }

        return cls.getMethod(new MethodDescriptor("<clinit>", void.class)) != null;
    }

    private boolean isStaticInit(String className) {
        ClassReader cls = unprocessedClassSource.get(className);
        if (cls == null) {
            return false;
        }
        return cls.getAnnotations().get(StaticInit.class.getName()) != null;
    }

    private boolean isStructure(String className) {
        while (!className.equals(Structure.class.getName())) {
            ClassReader cls = unprocessedClassSource.get(className);
            if (cls == null) {
                return false;
            }
            if (cls.getParent() == null) {
                return false;
            }
            className = cls.getParent();
        }
        return true;
    }
}
