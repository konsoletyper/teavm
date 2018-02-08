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
package org.teavm.backend.c.analyze;

import java.util.List;
import org.teavm.backend.c.generate.StringPool;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.lowlevel.CallSiteDescriptor;

public class StringPoolFiller extends AbstractInstructionReader {
    private StringPool pool;

    public StringPoolFiller(StringPool pool) {
        this.pool = pool;
    }

    public void fillFrom(ListableClassReaderSource classSource) {
        for (String className : classSource.getClassNames()) {
            addClass(classSource.get(className));
        }
    }

    public void fillCallSites(List<CallSiteDescriptor> callSites) {
        for (CallSiteDescriptor callSite : callSites) {
            if (callSite.getLocation() != null) {
                if (callSite.getLocation().getClassName() != null) {
                    pool.getStringIndex(callSite.getLocation().getClassName());
                }
                if (callSite.getLocation().getFileName() != null) {
                    pool.getStringIndex(callSite.getLocation().getFileName());
                }
                if (callSite.getLocation().getMethodName() != null) {
                    pool.getStringIndex(callSite.getLocation().getMethodName());
                }
            }
        }
    }

    private void addClass(ClassReader cls) {
        pool.getStringIndex(cls.getName());
        for (MethodReader method : cls.getMethods()) {
            ProgramReader program = method.getProgram();
            if (program != null) {
                for (BasicBlockReader block : program.getBasicBlocks()) {
                    block.readAllInstructions(this);
                }
            }
        }
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        pool.getStringIndex(cst);
    }
}
