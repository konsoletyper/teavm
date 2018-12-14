/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.tooling;

import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.vm.TeaVM;

public class InstructionLocationReader extends AbstractInstructionReader {
    private Set<String> resources;

    public InstructionLocationReader(Set<String> resources) {
        this.resources = resources;
    }

    @Override
    public void location(TextLocation location) {
        if (location != null && location.getFileName() != null) {
            resources.add(location.getFileName());
        }
    }

    public static Set<String> extractUsedResources(TeaVM vm) {
        Set<String> resources = new LinkedHashSet<>();
        ClassReaderSource classSource = vm.getDependencyClassSource();
        InstructionLocationReader reader = new InstructionLocationReader(resources);
        for (MethodReference methodRef : vm.getMethods()) {
            ClassReader cls = classSource.get(methodRef.getClassName());
            if (cls == null) {
                continue;
            }

            MethodReader method = cls.getMethod(methodRef.getDescriptor());
            if (method == null) {
                continue;
            }

            ProgramReader program = method.getProgram();
            if (program == null) {
                continue;
            }

            for (int i = 0; i < program.basicBlockCount(); ++i) {
                program.basicBlockAt(i).readAllInstructions(reader);
            }
        }

        return resources;
    }
}
