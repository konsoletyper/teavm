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
package org.teavm.vm;

import java.util.List;
import java.util.Set;
import org.teavm.dependency.ClassDependencyInfo;
import org.teavm.dependency.FieldDependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.diagnostics.Problem;

public interface Violations {
    Set<MethodDependencyInfo> getMissingMethods();

    Set<ClassDependencyInfo> getMissingClasses();

    Set<FieldDependencyInfo> getMissingFields();

    List<Problem> getDiagnosticsProblems();

    List<Problem> getSevereDiagnosticsProblems();

    boolean hasSevereViolations();

    void checkForViolations();
}
