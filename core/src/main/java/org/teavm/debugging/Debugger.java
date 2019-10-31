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
package org.teavm.debugging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.common.Promise;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationProvider;
import org.teavm.debugging.information.DebuggerCallSite;
import org.teavm.debugging.information.DebuggerCallSiteVisitor;
import org.teavm.debugging.information.DebuggerStaticCallSite;
import org.teavm.debugging.information.DebuggerVirtualCallSite;
import org.teavm.debugging.information.GeneratedLocation;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptVariable;
import org.teavm.model.MethodReference;

public class Debugger {
    private Set<DebuggerListener> listeners = new LinkedHashSet<>();
    private JavaScriptDebugger javaScriptDebugger;
    private DebugInformationProvider debugInformationProvider;
    private List<JavaScriptBreakpoint> temporaryBreakpoints = new ArrayList<>();
    private Map<String, DebugInformation> debugInformationMap = new HashMap<>();
    private Map<String, Set<DebugInformation>> debugInformationFileMap = new HashMap<>();
    private Map<DebugInformation, String> scriptMap = new HashMap<>();
    private Map<JavaScriptBreakpoint, Breakpoint> breakpointMap = new HashMap<>();
    private Set<Breakpoint> breakpoints = new LinkedHashSet<>();
    private Set<? extends Breakpoint> readonlyBreakpoints = Collections.unmodifiableSet(breakpoints);
    private CallFrame[] callStack;
    private Set<String> scriptNames = new LinkedHashSet<>();

    public Debugger(JavaScriptDebugger javaScriptDebugger, DebugInformationProvider debugInformationProvider) {
        this.javaScriptDebugger = javaScriptDebugger;
        this.debugInformationProvider = debugInformationProvider;
        javaScriptDebugger.addListener(javaScriptListener);
    }

    public JavaScriptDebugger getJavaScriptDebugger() {
        return javaScriptDebugger;
    }

    public void addListener(DebuggerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DebuggerListener listener) {
        listeners.remove(listener);
    }

    public Promise<Void> suspend() {
        return javaScriptDebugger.suspend();
    }

    public Promise<Void> resume() {
        return javaScriptDebugger.resume();
    }

    public Promise<Void> stepInto() {
        return step(true);
    }

    public Promise<Void> stepOut() {
        return javaScriptDebugger.stepOut();
    }

    public Promise<Void> stepOver() {
        return step(false);
    }

    private Promise<Void> jsStep(boolean enterMethod) {
        return enterMethod ? javaScriptDebugger.stepInto() : javaScriptDebugger.stepOver();
    }

    private Promise<Void> step(boolean enterMethod) {
        CallFrame[] callStack = getCallStack();
        if (callStack == null || callStack.length == 0) {
            return jsStep(enterMethod);
        }
        CallFrame recentFrame = callStack[0];
        if (recentFrame.getLocation() == null || recentFrame.getLocation().getFileName() == null
                || recentFrame.getLocation().getLine() < 0) {
            return jsStep(enterMethod);
        }
        Set<JavaScriptLocation> successors = new HashSet<>();
        boolean first = true;
        for (CallFrame frame : callStack) {
            boolean exits;
            String script = frame.getOriginalLocation().getScript();
            DebugInformation debugInfo = debugInformationMap.get(script);
            if (frame.getLocation() != null && frame.getLocation().getFileName() != null
                    && frame.getLocation().getLine() >= 0 && debugInfo != null) {
                exits = addFollowing(debugInfo, frame.getLocation(), script, new HashSet<>(), successors);
                if (enterMethod) {
                    CallSiteSuccessorFinder successorFinder = new CallSiteSuccessorFinder(debugInfo, script,
                            successors);
                    DebuggerCallSite[] callSites = debugInfo.getCallSites(frame.getLocation());
                    for (DebuggerCallSite callSite : callSites) {
                        callSite.acceptVisitor(successorFinder);
                    }
                }
            } else {
                exits = true;
            }
            if (!exits) {
                break;
            }
            enterMethod = false;
            if (!first && frame.getLocation() != null) {
                for (GeneratedLocation location : debugInfo.getGeneratedLocations(frame.getLocation())) {
                    successors.add(new JavaScriptLocation(script, location.getLine(), location.getColumn()));
                }
            }
            first = false;
        }

        List<Promise<Void>> jsBreakpointPromises = new ArrayList<>();
        for (JavaScriptLocation successor : successors) {
            jsBreakpointPromises.add(javaScriptDebugger.createBreakpoint(successor)
                    .thenVoid(temporaryBreakpoints::add));
        }
        return Promise.allVoid(jsBreakpointPromises).thenAsync(v -> javaScriptDebugger.resume());
    }

    static class CallSiteSuccessorFinder implements DebuggerCallSiteVisitor {
        private DebugInformation debugInfo;
        private String script;
        Set<JavaScriptLocation> locations;

        CallSiteSuccessorFinder(DebugInformation debugInfo, String script, Set<JavaScriptLocation> locations) {
            this.debugInfo = debugInfo;
            this.script = script;
            this.locations = locations;
        }

        @Override
        public void visit(DebuggerVirtualCallSite callSite) {
            for (MethodReference potentialMethod : debugInfo.getOverridingMethods(callSite.getMethod())) {
                for (GeneratedLocation loc : debugInfo.getMethodEntrances(potentialMethod)) {
                    loc = debugInfo.getStatementLocation(loc);
                    locations.add(new JavaScriptLocation(script, loc.getLine(), loc.getColumn()));
                }
            }
        }

        @Override
        public void visit(DebuggerStaticCallSite callSite) {
            for (GeneratedLocation loc : debugInfo.getMethodEntrances(callSite.getMethod())) {
                loc = debugInfo.getStatementLocation(loc);
                locations.add(new JavaScriptLocation(script, loc.getLine(), loc.getColumn()));
            }
        }
    }

    private boolean addFollowing(DebugInformation debugInfo, SourceLocation location, String script,
            Set<SourceLocation> visited, Set<JavaScriptLocation> successors) {
        if (!visited.add(location)) {
            return false;
        }
        SourceLocation[] following = debugInfo.getFollowingLines(location);
        boolean exits = false;
        if (following != null) {
            for (SourceLocation successor : following) {
                if (successor == null) {
                    exits = true;
                } else {
                    Collection<GeneratedLocation> genLocations = debugInfo.getGeneratedLocations(successor);
                    if (!genLocations.isEmpty()) {
                        for (GeneratedLocation loc : genLocations) {
                            loc = debugInfo.getStatementLocation(loc);
                            successors.add(new JavaScriptLocation(script, loc.getLine(), loc.getColumn()));
                        }
                    } else {
                        exits |= addFollowing(debugInfo, successor, script, visited, successors);
                    }
                }
            }
        }
        return exits;
    }

    private List<DebugInformation> debugInformationBySource(String sourceFile) {
        Set<DebugInformation> list = debugInformationFileMap.get(sourceFile);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    public Promise<Void> continueToLocation(SourceLocation location) {
        return continueToLocation(location.getFileName(), location.getLine());
    }

    public Promise<Void> continueToLocation(String fileName, int line) {
        if (!javaScriptDebugger.isSuspended()) {
            return Promise.VOID;
        }

        List<Promise<Void>> promises = new ArrayList<>();
        for (DebugInformation debugInformation : debugInformationBySource(fileName)) {
            Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(fileName, line);
            for (GeneratedLocation location : locations) {
                JavaScriptLocation jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        location.getLine(), location.getColumn());
                promises.add(javaScriptDebugger.createBreakpoint(jsLocation).thenVoid(temporaryBreakpoints::add));
            }
        }
        return Promise.allVoid(promises).thenAsync(v -> javaScriptDebugger.resume());
    }

    public boolean isSuspended() {
        return javaScriptDebugger.isSuspended();
    }

    public Promise<Breakpoint> createBreakpoint(String file, int line) {
        return createBreakpoint(new SourceLocation(file, line));
    }

    public Collection<? extends String> getSourceFiles() {
        return debugInformationFileMap.keySet();
    }

    public Promise<Breakpoint> createBreakpoint(SourceLocation location) {
        Breakpoint breakpoint = new Breakpoint(this, location);
        breakpoints.add(breakpoint);
        return updateInternalBreakpoints(breakpoint).then(v -> {
            updateBreakpointStatus(breakpoint, false);
            return breakpoint;
        });
    }

    public Set<? extends Breakpoint> getBreakpoints() {
        return readonlyBreakpoints;
    }

    private Promise<Void> updateInternalBreakpoints(Breakpoint breakpoint) {
        if (breakpoint.isDestroyed()) {
            return Promise.VOID;
        }

        List<Promise<Void>> promises = new ArrayList<>();
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            breakpointMap.remove(jsBreakpoint);
            promises.add(jsBreakpoint.destroy());
        }

        List<JavaScriptBreakpoint> jsBreakpoints = new ArrayList<>();
        SourceLocation location = breakpoint.getLocation();
        for (DebugInformation debugInformation : debugInformationBySource(location.getFileName())) {
            Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(location);
            for (GeneratedLocation genLocation : locations) {
                JavaScriptLocation jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        genLocation.getLine(), genLocation.getColumn());
                promises.add(javaScriptDebugger.createBreakpoint(jsLocation).thenVoid(jsBreakpoint -> {
                    jsBreakpoints.add(jsBreakpoint);
                    breakpointMap.put(jsBreakpoint, breakpoint);
                }));
            }
        }
        breakpoint.jsBreakpoints = jsBreakpoints;

        return Promise.allVoid(promises);
    }

    private DebuggerListener[] getListeners() {
        return listeners.toArray(new DebuggerListener[0]);
    }

    private void updateBreakpointStatus(Breakpoint breakpoint, boolean fireEvent) {
        boolean valid = false;
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            if (jsBreakpoint.isValid()) {
                valid = true;
            }
        }
        if (breakpoint.valid != valid) {
            breakpoint.valid = valid;
            if (fireEvent) {
                for (DebuggerListener listener : getListeners()) {
                    listener.breakpointStatusChanged(breakpoint);
                }
            }
        }
    }

    public CallFrame[] getCallStack() {
        if (!isSuspended()) {
            return null;
        }
        if (callStack == null) {
            // TODO: with inlining enabled we can have several JVM methods compiled into one JavaScript function
            // so we must consider this case.
            List<CallFrame> frames = new ArrayList<>();
            boolean wasEmpty = false;
            for (JavaScriptCallFrame jsFrame : javaScriptDebugger.getCallStack()) {
                DebugInformation debugInformation = debugInformationMap.get(jsFrame.getLocation().getScript());
                SourceLocation loc;
                if (debugInformation != null) {
                    loc = debugInformation.getSourceLocation(jsFrame.getLocation().getLine(),
                            jsFrame.getLocation().getColumn());
                } else {
                    loc = null;
                }
                boolean empty = loc == null || (loc.getFileName() == null && loc.getLine() < 0);
                MethodReference method = !empty && debugInformation != null
                        ? debugInformation.getMethodAt(jsFrame.getLocation().getLine(),
                                jsFrame.getLocation().getColumn())
                        : null;
                if (!empty || !wasEmpty) {
                    frames.add(new CallFrame(this, jsFrame, loc, method, debugInformation));
                }
                wasEmpty = empty;
            }
            callStack = frames.toArray(new CallFrame[0]);
        }
        return callStack.clone();
    }

    Promise<Map<String, Variable>> createVariables(JavaScriptCallFrame jsFrame, DebugInformation debugInformation) {
        return jsFrame.getVariables().then(jsVariables -> {
            Map<String, Variable> vars = new HashMap<>();
            for (Map.Entry<String, ? extends JavaScriptVariable> entry : jsVariables.entrySet()) {
                JavaScriptVariable jsVar = entry.getValue();
                String[] names = mapVariable(entry.getKey(), jsFrame.getLocation());
                Value value = new Value(this, debugInformation, jsVar.getValue());
                for (String name : names) {
                    if (name == null) {
                        name = "js:" + jsVar.getName();
                    }
                    vars.put(name, new Variable(name, value));
                }
            }
            return Collections.unmodifiableMap(vars);
        });
    }

    private void addScript(String name) {
        if (!name.isEmpty()) {
            scriptNames.add(name);
        }
        if (debugInformationMap.containsKey(name)) {
            updateBreakpoints();
            return;
        }
        DebugInformation debugInfo = debugInformationProvider.getDebugInformation(name);
        if (debugInfo == null) {
            return;
        }
        debugInformationMap.put(name, debugInfo);
        for (String sourceFile : debugInfo.getFilesNames()) {
            Set<DebugInformation> list = debugInformationFileMap.get(sourceFile);
            if (list == null) {
                list = new HashSet<>();
                debugInformationFileMap.put(sourceFile, list);
            }
            list.add(debugInfo);
        }
        scriptMap.put(debugInfo, name);
        updateBreakpoints();
    }

    public Set<? extends String> getScriptNames() {
        return scriptNames;
    }

    private void updateBreakpoints() {
        for (Breakpoint breakpoint : breakpoints) {
            updateInternalBreakpoints(breakpoint).thenVoid(v -> updateBreakpointStatus(breakpoint, true));
        }
    }

    public boolean isAttached() {
        return javaScriptDebugger.isAttached();
    }

    public void detach() {
        javaScriptDebugger.detach();
    }

    void destroyBreakpoint(Breakpoint breakpoint) {
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            jsBreakpoint.destroy();
            breakpointMap.remove(jsBreakpoint);
        }
        breakpoint.jsBreakpoints = new ArrayList<>();
        breakpoints.remove(breakpoint);
    }

    private void fireResumed() {
        for (DebuggerListener listener : getListeners()) {
            listener.resumed();
        }
    }

    private void firePaused(JavaScriptBreakpoint breakpoint) {
        List<JavaScriptBreakpoint> temporaryBreakpoints = new ArrayList<>(this.temporaryBreakpoints);
        this.temporaryBreakpoints.clear();
        List<Promise<Void>> promises = new ArrayList<>();
        for (JavaScriptBreakpoint jsBreakpoint : temporaryBreakpoints) {
            promises.add(jsBreakpoint.destroy());
        }
        callStack = null;
        Promise.allVoid(promises).thenVoid(v -> {
            Breakpoint javaBreakpoint = null;
            if (breakpoint != null && !temporaryBreakpoints.contains(breakpoint)) {
                javaBreakpoint = breakpointMap.get(breakpoint);
            }
            for (DebuggerListener listener : getListeners()) {
                listener.paused(javaBreakpoint);
            }
        });
    }

    private void fireAttached() {
        for (Breakpoint breakpoint : breakpoints) {
            updateInternalBreakpoints(breakpoint).thenVoid(v -> updateBreakpointStatus(breakpoint, false));
        }
        for (DebuggerListener listener : getListeners()) {
            listener.attached();
        }
    }

    private void fireDetached() {
        for (Breakpoint breakpoint : breakpoints) {
            updateBreakpointStatus(breakpoint, false);
        }
        for (DebuggerListener listener : getListeners()) {
            listener.detached();
        }
    }

    private void fireBreakpointChanged(JavaScriptBreakpoint jsBreakpoint) {
        Breakpoint breakpoint = breakpointMap.get(jsBreakpoint);
        if (breakpoint != null) {
            updateBreakpointStatus(breakpoint, true);
        }
    }

    String[] mapVariable(String variable, JavaScriptLocation location) {
        DebugInformation debugInfo = debugInformationMap.get(location.getScript());
        if (debugInfo == null) {
            return new String[0];
        }
        return debugInfo.getVariableMeaningAt(location.getLine(), location.getColumn(), variable);
    }

    String mapField(String className, String jsField) {
        for (DebugInformation debugInfo : debugInformationMap.values()) {
            String meaning = debugInfo.getFieldMeaning(className, jsField);
            if (meaning != null) {
                return meaning;
            }
        }
        return null;
    }

    private JavaScriptDebuggerListener javaScriptListener = new JavaScriptDebuggerListener() {
        @Override
        public void resumed() {
            fireResumed();
        }

        @Override
        public void paused(JavaScriptBreakpoint breakpoint) {
            firePaused(breakpoint);
        }

        @Override
        public void scriptAdded(String name) {
            addScript(name);
        }

        @Override
        public void attached() {
            fireAttached();
        }

        @Override
        public void detached() {
            fireDetached();
        }

        @Override
        public void breakpointChanged(JavaScriptBreakpoint jsBreakpoint) {
            fireBreakpointChanged(jsBreakpoint);
        }
    };
}
