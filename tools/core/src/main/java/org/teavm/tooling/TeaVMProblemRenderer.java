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

import java.util.Collection;
import java.util.Iterator;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.vm.TeaVM;

public final class TeaVMProblemRenderer {
    private TeaVMProblemRenderer() {
    }

    public static void describeProblems(TeaVM vm, TeaVMToolLog log) {
        describeProblems(vm.getDependencyInfo().getCallGraph(), vm.getProblemProvider(), log);
    }

    public static void describeProblems(CallGraph cg, ProblemProvider problems, TeaVMToolLog log) {
        DefaultProblemTextConsumer consumer = new DefaultProblemTextConsumer();
        for (Problem problem : problems.getProblems()) {
            consumer.clear();
            problem.render(consumer);
            StringBuilder sb = new StringBuilder();
            sb.append(consumer.getText());
            renderCallStack(cg, problem.getLocation(), sb);
            String problemText = sb.toString();
            switch (problem.getSeverity()) {
                case ERROR:
                    log.error(problemText);
                    break;
                case WARNING:
                    log.warning(problemText);
                    break;
            }
        }
    }

    public static void renderCallStack(CallGraph cg, CallLocation location, StringBuilder sb) {
        if (location == null) {
            return;
        }
        sb.append("\n    at ");
        renderCallLocation(location.getMethod(), location.getSourceLocation(), sb);
        if (location.getMethod() != null) {
            CallGraphNode node = cg.getNode(location.getMethod());
            for (int i = 0; i < 100; ++i) {
                Iterator<? extends CallSite> callSites = node.getCallerCallSites().iterator();
                if (!callSites.hasNext()) {
                    break;
                }
                CallSite callSite = callSites.next();
                sb.append("\n    at ");

                CallGraphNode caller = getCaller(callSite);
                renderCallLocation(caller.getMethod(), getLocation(callSite, caller), sb);
                node = caller;
            }
        }
    }

    private static CallGraphNode getCaller(CallSite callSite) {
        Collection<? extends CallGraphNode> callers = callSite.getCallers();
        if (callers.isEmpty()) {
            return null;
        }
        return callers.iterator().next();
    }

    private static TextLocation getLocation(CallSite callSite, CallGraphNode caller) {
        if (caller == null) {
            return null;
        }
        Collection<? extends TextLocation> locations = callSite.getLocations(caller);
        if (locations.isEmpty()) {
            return null;
        }
        return locations.iterator().next();
    }

    public static void renderCallLocation(MethodReference method, TextLocation location, StringBuilder sb) {
        if (method != null) {
            sb.append(method.getClassName() + "." + method.getName());
        } else {
            sb.append("unknown method");
        }
        if (location != null) {
            sb.append("(");
            String fileName = location.getFileName();
            if (fileName != null) {
                sb.append(fileName.substring(fileName.lastIndexOf('/') + 1));
                sb.append(':');
            }
            sb.append(location.getLine());
            sb.append(')');
        }
    }
}
