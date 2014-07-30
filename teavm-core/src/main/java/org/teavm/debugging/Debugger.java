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
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
// TODO: variable name handling
// TODO: class fields handling
public class Debugger {
    private List<DebuggerListener> listeners = new ArrayList<>();
    private JavaScriptDebugger javaScriptDebugger;
    private DebugInformationProvider debugInformationProvider;
    private List<JavaScriptBreakpoint> temporaryJsBreakpoints = new ArrayList<>();
    private Map<String, DebugInformation> debugInformationMap = new HashMap<>();
    private Map<String, List<DebugInformation>> debugInformationFileMap = new HashMap<>();
    private Map<DebugInformation, String> scriptMap = new HashMap<>();
    Map<JavaScriptBreakpoint, Breakpoint> breakpointMap = new HashMap<>();
    Set<Breakpoint> breakpoints = new HashSet<>();
    private CallFrame[] callStack;

    public Debugger(JavaScriptDebugger javaScriptDebugger, DebugInformationProvider debugInformationProvider) {
        this.javaScriptDebugger = javaScriptDebugger;
        this.debugInformationProvider = debugInformationProvider;
        javaScriptDebugger.addListener(javaScriptListener);
    }

    public void addListener(DebuggerListener listener) {
        listeners.add(listener);
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
        javaScriptDebugger.stepInto();
    }

    public void stepOut() {
        javaScriptDebugger.stepOut();
    }

    public void stepOver() {
        javaScriptDebugger.stepOver();
    }

    private List<DebugInformation> debugInformationBySource(String sourceFile) {
        List<DebugInformation> list = debugInformationFileMap.get(sourceFile);
        return list != null ? list : Collections.<DebugInformation>emptyList();
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
                    temporaryJsBreakpoints.add(jsBreakpoint);
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
        Breakpoint breakpoint = new Breakpoint(this, location);
        breakpoints.add(breakpoint);
        updateInternalBreakpoints(breakpoint);
        updateBreakpointStatus(breakpoint, false);
        return breakpoint;
    }

    public Set<Breakpoint> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    void updateInternalBreakpoints(Breakpoint breakpoint) {
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            breakpointMap.remove(jsBreakpoint);
            jsBreakpoint.destroy();
        }
        breakpoint.jsBreakpoints.clear();
        SourceLocation location = breakpoint.getLocation();
        for (DebugInformation debugInformation : debugInformationBySource(location.getFileName())) {
            Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(location);
            for (GeneratedLocation genLocation : locations) {
                JavaScriptLocation jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        genLocation.getLine(), genLocation.getColumn());
                JavaScriptBreakpoint jsBreakpoint = javaScriptDebugger.createBreakpoint(jsLocation);
                breakpoint.jsBreakpoints.add(jsBreakpoint);
                breakpointMap.put(jsBreakpoint, breakpoint);
            }
        }
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
                for (DebuggerListener listener : listeners) {
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
                    frames.add(new CallFrame(loc, method));
                }
                wasEmpty = empty;
            }
            callStack = frames.toArray(new CallFrame[0]);
        }
        return callStack.clone();
    }

    private void addScript(String name) {
        if (debugInformationMap.containsKey(name)) {
            return;
        }
        DebugInformation debugInfo = debugInformationProvider.getDebugInformation(name);
        if (debugInfo == null) {
            return;
        }
        debugInformationMap.put(name, debugInfo);
        for (String sourceFile : debugInfo.getCoveredSourceFiles()) {
            List<DebugInformation> list = debugInformationFileMap.get(sourceFile);
            if (list == null) {
                list = new ArrayList<>();
                debugInformationFileMap.put(sourceFile, list);
            }
            list.add(debugInfo);
        }
        scriptMap.put(debugInfo, name);
        for (Breakpoint breakpoint : breakpoints) {
            updateInternalBreakpoints(breakpoint);
            updateBreakpointStatus(breakpoint, true);
        }
    }

    public boolean isAttached() {
        return javaScriptDebugger.isAttached();
    }

    void destroyBreakpoint(Breakpoint breakpoint) {
        for (JavaScriptBreakpoint jsBreakpoint : breakpoint.jsBreakpoints) {
            jsBreakpoint.destroy();
            breakpointMap.remove(jsBreakpoint);
        }
        breakpoint.jsBreakpoints.clear();
        breakpoints.remove(this);
    }

    private void fireResumed() {
        for (JavaScriptBreakpoint jsBreakpoint : temporaryJsBreakpoints) {
            jsBreakpoint.destroy();
        }
        temporaryJsBreakpoints.clear();
        for (DebuggerListener listener : listeners) {
            listener.resumed();
        }
    }

    private void fireAttached() {
        for (Breakpoint breakpoint : breakpoints) {
            updateInternalBreakpoints(breakpoint);
            updateBreakpointStatus(breakpoint, false);
        }
        for (DebuggerListener listener : listeners) {
            listener.attached();
        }
    }

    private void fireDetached() {
        for (Breakpoint breakpoint : breakpoints) {
            updateBreakpointStatus(breakpoint, false);
        }
        for (DebuggerListener listener : listeners) {
            listener.detached();
        }
    }

    private void fireBreakpointChanged(JavaScriptBreakpoint jsBreakpoint) {
        Breakpoint breakpoint = breakpointMap.get(jsBreakpoint);
        if (breakpoint != null) {
            updateBreakpointStatus(breakpoint, true);
        }
    }

    private JavaScriptDebuggerListener javaScriptListener = new JavaScriptDebuggerListener() {
        @Override
        public void resumed() {
            fireResumed();
        }

        @Override
        public void paused() {
            callStack = null;
            for (DebuggerListener listener : listeners) {
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
