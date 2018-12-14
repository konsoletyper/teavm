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
package org.teavm.devserver;

import java.util.Collection;
import java.util.List;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.tooling.InstructionLocationReader;
import org.teavm.tooling.builder.BuildResult;
import org.teavm.vm.TeaVM;

class CodeServletBuildResult implements BuildResult {
    private TeaVM vm;
    private List<String> generatedFiles;
    private Collection<String> usedResources;

    public CodeServletBuildResult(TeaVM vm, List<String> generatedFiles) {
        this.vm = vm;
        this.generatedFiles = generatedFiles;
    }

    @Override
    public CallGraph getCallGraph() {
        return vm.getDependencyInfo().getCallGraph();
    }

    @Override
    public ProblemProvider getProblems() {
        return vm.getProblemProvider();
    }

    @Override
    public Collection<String> getUsedResources() {
        if (usedResources == null) {
            usedResources = InstructionLocationReader.extractUsedResources(vm);
        }
        return usedResources;
    }

    @Override
    public Collection<String> getClasses() {
        return vm.getClasses();
    }

    @Override
    public Collection<String> getGeneratedFiles() {
        return generatedFiles;
    }
}
