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
import java.util.Collections;
import java.util.List;
import org.teavm.model.CallLocation;

public class AccumulationDiagnostics implements Diagnostics, ProblemProvider {
    private List<Problem> problems = new ArrayList<>();
    private List<Problem> readonlyProblems = Collections.unmodifiableList(problems);
    private List<Problem> severeProblems = new ArrayList<>();
    private List<Problem> readonlySevereProblems = Collections.unmodifiableList(severeProblems);

    @Override
    public void error(CallLocation location, String error, Object... params) {
        Problem problem = new Problem(ProblemSeverity.ERROR, location, error, params);
        problems.add(problem);
        severeProblems.add(problem);
    }

    @Override
    public void warning(CallLocation location, String error, Object... params) {
        Problem problem = new Problem(ProblemSeverity.ERROR, location, error, params);
        problems.add(problem);
    }

    @Override
    public List<Problem> getProblems() {
        return readonlyProblems;
    }

    @Override
    public List<Problem> getSevereProblems() {
        return readonlySevereProblems;
    }
}
