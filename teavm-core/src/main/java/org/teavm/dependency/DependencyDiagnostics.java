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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.Diagnostics;
import org.teavm.model.InstructionLocation;
import org.teavm.vm.DiagnosticsProblem;
import org.teavm.vm.DiagnosticsProblemSeverity;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class DependencyDiagnostics implements Diagnostics {
    private List<DiagnosticsProblem> problems = new ArrayList<>();
    private List<DiagnosticsProblem> severeProblems = new ArrayList<>();

    @Override
    public void error(InstructionLocation location, String error) {
        DiagnosticsProblem violation = new DiagnosticsProblem(DiagnosticsProblemSeverity.ERROR, location, error);
        problems.add(violation);
        severeProblems.add(violation);
    }

    @Override
    public void error(String error) {
        error(null, error);
    }

    @Override
    public void warning(InstructionLocation location, String error) {
        DiagnosticsProblem violation = new DiagnosticsProblem(DiagnosticsProblemSeverity.WARNING, location, error);
        problems.add(violation);
    }

    @Override
    public void warning(String error) {
        warning(null, error);
    }

    public List<DiagnosticsProblem> getProblems() {
        return problems;
    }

    public List<DiagnosticsProblem> getSevereProblems() {
        return severeProblems;
    }
}
