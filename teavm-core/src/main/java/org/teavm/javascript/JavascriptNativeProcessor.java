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
package org.teavm.javascript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.javascript.ni.JSObject;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;

/**
 *
 * @author Alexey Andreev
 */
class JavascriptNativeProcessor {
    private ClassHolderSource classSource;
    private Map<String, Boolean> knownJavaScriptClasses = new HashMap<>();

    public JavascriptNativeProcessor(ClassHolderSource classSource) {
        this.classSource = classSource;
        knownJavaScriptClasses.put(JSObject.class.getName(), true);
    }

    public void processProgram(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            List<Instruction> instructions = block.getInstructions();
            for (int j = 0; j < instructions.size(); ++j) {
                Instruction insn = instructions.get(j);
                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
            }
        }
    }

    private boolean isJavaScriptClass(String className) {
        Boolean known = knownJavaScriptClasses.get(className);
        if (known == null) {
            known = exploreIfJavaScriptClass(className);
            knownJavaScriptClasses.put(className, known);
        }
        return known;
    }

    private boolean exploreIfJavaScriptClass(String className) {
        ClassHolder cls = classSource.getClassHolder(className);
        if (cls == null || !cls.getModifiers().contains(ElementModifier.INTERFACE)) {
            return false;
        }
        for (String iface : cls.getInterfaces()) {
            if (isJavaScriptClass(iface)) {
                return true;
            }
        }
        return false;
    }
}
