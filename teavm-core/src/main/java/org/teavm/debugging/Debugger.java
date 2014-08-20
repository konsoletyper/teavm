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

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class Debugger {
    private static final Object dummyObject = new Object();
    private ConcurrentMap<DebuggerListener, Object> listeners = new ConcurrentHashMap<>();
    private JavaScriptDebugger javaScriptDebugger;
    private DebugInformationProvider debugInformationProvider;
    private BlockingQueue<JavaScriptBreakpoint> temporaryBreakpoints = new LinkedBlockingQueue<>();
    private ConcurrentMap<String, DebugInformation> debugInformationMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ConcurrentMap<DebugInformation, Object>> debugInformationFileMap =
            new ConcurrentHashMap<>();
    private ConcurrentMap<DebugInformation, String> scriptMap = new ConcurrentHashMap<>();
    ConcurrentMap<JavaScriptBreakpoint, Breakpoint> breakpointMap = new ConcurrentHashMap<>();
    ConcurrentMap<Breakpoint, Object> breakpoints = new ConcurrentHashMap<>();
    private volatile CallFrame[] callStack;

    public Debugger(JavaScriptDebugger javaScriptDebugger, DebugInformationProvider debugInformationProvider) {
        this.javaScriptDebugger = javaScriptDebugger;
        this.debugInformationProvider = debugInformationProvider;
        javaScriptDebugger.addListener(javaScriptListener);
    }

    public void addListener(DebuggerListener listener) {
        listeners.put(listener, dummyObject);
    }

    public void removeListener(DebuggerListener listener) {
        listeners.remove(listener);
    }

    public void suspend() {
        javaScriptDebugger.suspend();
    }

    public void resume() {
        javaScriptDebugger.resume();
    }

    public void stepInto() {
        step(true);
    }

    public void stepOut() {
        javaScriptDebugger.stepOut();
    }

    public void stepOver() {
        step(false);
    }

    private void step(boolean enterMethod) {
        CallFrame[] callStack = getCallStack();
        if (callStack == null || callStack.length == 0) {
            if (enterMethod) {
                javaScriptDebugger.stepInto();
            } else {
                javaScriptDebugger.stepOver();
            }
            return;
        }
        CallFrame recentFrame = callStack[0];
        if (recentFrame.getLocation() == null || recentFrame.getLocation().getFileName() == null ||
                recentFrame.getLocation().getLine() < 0) {
            if (enterMethod) {
                javaScriptDebugger.stepInto();
            } else {
                javaScriptDebugger.stepOver();
            }
            return;
        }
        Set<JavaScriptLocation> successors = new HashSet<>();
        for (int i = 0; i < callStack.length; ++i) {
            CallFrame frame = callStack[i];
            boolean exits = false;
            DebugInformation mainDebugInfo = debugInformationMap.get(frame.originalLocation.getScript());
            GeneratedLocation genLoc = new GeneratedLocation(frame.originalLocation.getLine(),
                    frame.originalLocation.getColumn());
            MethodReference callMethod = mainDebugInfo != null ? mainDebugInfo.getCallSite(genLoc) : null;
            String script = frame.originalLocation.getScript();
            DebugInformation debugInfo = debugInformationMap.get(script);
            if (debugInfo != null) {
                SourceLocation[] following = debugInfo.getFollowingLines(frame.getLocation());
                if (following != null) {
                    for (SourceLocation successor : following) {
                        if (successor == null) {
                            exits = true;
                        } else {
                            for (GeneratedLocation loc : debugInfo.getGeneratedLocations(successor)) {
                                successors.add(new JavaScriptLocation(script, loc.getLine(), loc.getColumn()));
                            }
                        }
                    }
                }
                if (enterMethod && callMethod != null) {
                    for (MethodReference potentialMethod : debugInfo.getOverridingMethods(callMethod)) {
                        for (GeneratedLocation loc : debugInfo.getMethodEntrances(potentialMethod)) {
                            successors.add(new JavaScriptLocation(script, loc.getLine(), loc.getColumn()));
                        }
                    }
                }
            }
            if (!exits) {
                break;
            }
        }
        for (JavaScriptLocation successor : successors) {
            temporaryBreakpoints.add(javaScriptDebugger.createBreakpoint(successor));
        }
        javaScriptDebugger.resume();
    }

    private List<DebugInformation> debugInformationBySource(String sourceFile) {
        Map<DebugInformation, Object> list = debugInformationFileMap.get(sourceFile);
        return list != null ? new ArrayList<>(list.keySet()) : Collections.<DebugInformation>emptyList();
    }

    public void continueToLocation(SourceLocation location) {
        continueToLocation(location.getFileName(), location.getLine());
    }

    public void continueToLocation(String fileName, int line) {
        if (!javaScriptDebugger.isSuspended()) {
            return;
        }
        for (DebugInformation debugInformation : debugInformationBySource(fileName)) {
            Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(fileName, line);
            for (GeneratedLocation location : locations) {
                JavaScriptLocation jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        location.getLine(), location.getColumn());
                JavaScriptBreakpoint jsBreakpoint = javaScriptDebugger.createBreakpoint(jsLocation);
                if (jsBreakpoint != null) {
                    temporaryBreakpoints.add(jsBreakpoint);
                }
            }
        }
        javaScriptDebugger.resume();
    }

    public boolean isSuspended() {
        return javaScriptDebugger.isSuspended();
    }

    public Breakpoint createBreakpoint(String file, int line) {
        return createBreakpoint(new SourceLocation(file, line));
    }

    public Breakpoint createBreakpoint(SourceLocation location) {
        synchronized (breakpoints) {
            Breakpoint breakpoint = new Breakpoint(this, location);
            breakpoints.put(breakpoint, dummyObject);
            updateInternalBreakpoints(breakpoint);
            updateBreakpointStatus(breakpoint, false);
            return breakpoint;
        }
    }

    public Set<Breakpoint> getBreakpoints() {
        return new HashSet<>(breakpoints.keySet());
    }

    void updateInternalBreakpoints(Breakpoint breakpoint) {
        if (breakpoint.isDestroyed()) {
            return;
        }
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            breakpointMap.remove(jsBreakpoint);
            jsBreakpoint.destroy();
        }
        List<JavaScriptBreakpoint> jsBreakpoints = new ArrayList<>();
        SourceLocation location = breakpoint.getLocation();
        for (DebugInformation debugInformation : debugInformationBySource(location.getFileName())) {
            Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(location);
            for (GeneratedLocation genLocation : locations) {
                JavaScriptLocation jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        genLocation.getLine(), genLocation.getColumn());
                JavaScriptBreakpoint jsBreakpoint = javaScriptDebugger.createBreakpoint(jsLocation);
                jsBreakpoints.add(jsBreakpoint);
                breakpointMap.put(jsBreakpoint, breakpoint);
            }
        }
        breakpoint.jsBreakpoints = jsBreakpoints;
    }

    private DebuggerListener[] getListeners() {
        return listeners.keySet().toArray(new DebuggerListener[0]);
    }

    void updateBreakpointStatus(Breakpoint breakpoint, boolean fireEvent) {
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
                MethodReference method = !empty ? debugInformation.getMethodAt(jsFrame.getLocation().getLine(),
                        jsFrame.getLocation().getColumn()) : null;
                if (!empty || !wasEmpty) {
                    VariableMap vars = new VariableMap(jsFrame.getVariables(), this, jsFrame.getLocation());
                    frames.add(new CallFrame(jsFrame.getLocation(), loc, method, vars));
                }
                wasEmpty = empty;
            }
            callStack = frames.toArray(new CallFrame[0]);
        }
        return callStack.clone();
    }

    private void addScript(String name) {
        if (debugInformationMap.containsKey(name)) {
            updateBreakpoints();
            return;
        }
        DebugInformation debugInfo = debugInformationProvider.getDebugInformation(name);
        if (debugInfo == null) {
            updateBreakpoints();
            return;
        }
        if (debugInformationMap.putIfAbsent(name, debugInfo) != null) {
            updateBreakpoints();
            return;
        }
        for (String sourceFile : debugInfo.getCoveredSourceFiles()) {
            ConcurrentMap<DebugInformation, Object> list = debugInformationFileMap.get(sourceFile);
            if (list == null) {
                list = new ConcurrentHashMap<>();
                ConcurrentMap<DebugInformation, Object> existing = debugInformationFileMap.putIfAbsent(
                        sourceFile, list);
                if (existing != null) {
                    list = existing;
                }
            }
            list.put(debugInfo, dummyObject);
        }
        scriptMap.put(debugInfo, name);
        updateBreakpoints();
    }

    private void updateBreakpoints() {
        synchronized (breakpointMap) {
            for (Breakpoint breakpoint : breakpoints.keySet()) {
                updateInternalBreakpoints(breakpoint);
                updateBreakpointStatus(breakpoint, true);
            }
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
        breakpoints.remove(this);
    }

    private void fireResumed() {
        List<JavaScriptBreakpoint> temporaryBreakpoints = new ArrayList<>();
        this.temporaryBreakpoints.drainTo(temporaryBreakpoints);
        for (JavaScriptBreakpoint jsBreakpoint : temporaryBreakpoints) {
            jsBreakpoint.destroy();
        }
        for (DebuggerListener listener : getListeners()) {
            listener.resumed();
        }
    }

    private void fireAttached() {
        synchronized (breakpointMap) {
            for (Breakpoint breakpoint : breakpoints.keySet()) {
                updateInternalBreakpoints(breakpoint);
                updateBreakpointStatus(breakpoint, false);
            }
        }
        for (DebuggerListener listener : getListeners()) {
            listener.attached();
        }
    }

    private void fireDetached() {
        for (Breakpoint breakpoint : breakpoints.keySet()) {
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
        public void paused() {
            callStack = null;
            for (DebuggerListener listener : getListeners()) {
                listener.paused();
            }
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
