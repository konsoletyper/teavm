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
    private DebugInformation debugInformation;
    private List<JavaScriptBreakpoint> temporaryJsBreakpoints = new ArrayList<>();
    Map<String, Breakpoint> breakpointMap = new HashMap<>();
    private CallFrame[] callStack;

    public Debugger(JavaScriptDebugger javaScriptDebugger, DebugInformation debugInformation) {
        this.javaScriptDebugger = javaScriptDebugger;
        this.debugInformation = debugInformation;
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
        if (!javaScriptDebugger.isSuspended()) {
            return;
        }
        javaScriptDebugger.stepInto();
    }

    public void stepOut() {
        if (!javaScriptDebugger.isSuspended()) {
            return;
        }
        // TODO: lookup all locations that are "out of" the current location and create temporary breakpoints
        javaScriptDebugger.stepOut();
    }

    public void stepOver() {
        if (!javaScriptDebugger.isSuspended()) {
            return;
        }
        javaScriptDebugger.stepOver();
        // TODO: lookup all locations that are "after" the current location and create temporary breakpoints
    }

    public void continueToLocation(String fileName, int line) {
        if (!javaScriptDebugger.isSuspended()) {
            return;
        }
        Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(fileName, line);
        for (GeneratedLocation location : locations) {
            JavaScriptBreakpoint jsBreakpoint = javaScriptDebugger.createBreakpoint(location);
            if (jsBreakpoint != null) {
                temporaryJsBreakpoints.add(jsBreakpoint);
            }
        }
        javaScriptDebugger.resume();
    }

    public boolean isSuspended() {
        return javaScriptDebugger.isSuspended();
    }

    public Breakpoint createBreakpoint(String fileName, int line) {
        Collection<GeneratedLocation> locations = debugInformation.getGeneratedLocations(fileName, line);
        List<JavaScriptBreakpoint> jsBreakpoints = new ArrayList<>();
        for (GeneratedLocation location : locations) {
            JavaScriptBreakpoint jsBreakpoint = javaScriptDebugger.createBreakpoint(location);
            if (jsBreakpoint != null) {
                jsBreakpoints.add(jsBreakpoint);
            }
        }
        return !jsBreakpoints.isEmpty() ? new Breakpoint(this, jsBreakpoints, fileName, line) : null;
    }

    public CallFrame[] getCallStack() {
        if (isSuspended()) {
            return null;
        }
        if (callStack == null) {
            // TODO: with inlining enabled we can have several JVM methods compiled into one JavaScript function
            // so we must consider this case.
            List<CallFrame> frames = new ArrayList<>();
            boolean wasEmpty = false;
            for (JavaScriptCallFrame jsFrame : javaScriptDebugger.getCallStack()) {
                SourceLocation loc = debugInformation.getSourceLocation(jsFrame.getLocation());
                boolean empty = loc == null || (loc.getFileName() == null && loc.getLine() < 0);
                MethodReference method = !empty ? debugInformation.getMethodAt(jsFrame.getLocation()) : null;
                if (!empty || !wasEmpty) {
                    frames.add(new CallFrame(loc, method));
                }
                wasEmpty = empty;
            }
            callStack = frames.toArray(new CallFrame[0]);
        }
        return callStack.clone();
    }

    private JavaScriptDebuggerListener javaScriptListener = new JavaScriptDebuggerListener() {
        @Override
        public void resumed() {
            for (JavaScriptBreakpoint jsBreakpoint : temporaryJsBreakpoints) {
                jsBreakpoint.destroy();
            }
            temporaryJsBreakpoints.clear();
            for (DebuggerListener listener : listeners) {
                listener.resumed();
            }
        }

        @Override
        public void paused() {
            for (DebuggerListener listener : listeners) {
                listener.paused();
            }
        }
    };
}
