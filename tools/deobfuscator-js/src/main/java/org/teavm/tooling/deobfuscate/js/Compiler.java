/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.tooling.deobfuscate.js;

import java.io.File;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.vm.TeaVMOptimizationLevel;

public final class Compiler {
    private Compiler() {
    }

    public static void main(String[] args) throws TeaVMToolException {
        var tool = new TeaVMTool();
        var log = new ConsoleTeaVMToolLog(false);
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setMainClass(args[0]);
        tool.setEntryPointName(args[1]);
        tool.setTargetDirectory(new File(args[2]));
        tool.setTargetFileName(args[3]);
        tool.setObfuscated(true);
        tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);

        tool.generate();
        TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), log);
        if (!tool.getProblemProvider().getSevereProblems().isEmpty()) {
            System.exit(1);
        }
    }
}
