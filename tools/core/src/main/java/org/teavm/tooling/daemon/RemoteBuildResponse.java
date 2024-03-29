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
package org.teavm.tooling.daemon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.Problem;

public class RemoteBuildResponse implements Serializable {
    public CallGraph callGraph;
    public final List<Problem> problems = new ArrayList<>();
    public final List<Problem> severeProblems = new ArrayList<>();
    public Throwable exception;
}
