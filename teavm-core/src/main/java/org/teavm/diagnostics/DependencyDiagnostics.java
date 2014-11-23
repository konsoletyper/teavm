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
package org.teavm.diagnostics;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.InstructionLocation;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class DependencyDiagnostics implements Diagnostics {
    private List<Problem> problems = new ArrayList<>();
    private List<Problem> severeProblems = new ArrayList<>();

    @Override
    public void error(MethodReference method, InstructionLocation location, String error, Object[] params) {
        Problem violation = new Problem(ProblemSeverity.ERROR, location, error);
        problems.add(violation);
        severeProblems.add(violation);
    }

    @Override
    public void error(String error) {
        error(null, error);
    }

    @Override
    public void warning(InstructionLocation location, String error) {
        Problem violation = new Problem(ProblemSeverity.WARNING, location, error);
        problems.add(violation);
    }

    @Override
    public void warning(String error) {
        warning(null, error);
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public List<Problem> getSevereProblems() {
        return severeProblems;
    }
}
