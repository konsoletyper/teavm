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
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.debug.info.LineInfo;
import org.teavm.backend.wasm.debug.info.LineInfoFileCommand;
import org.teavm.backend.wasm.debug.info.MethodInfo;
import org.teavm.common.Promise;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationProvider;
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
import org.teavm.debugging.javascript.JavaScriptScript;
import org.teavm.debugging.javascript.JavaScriptVariable;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class Debugger {
    private Set<DebuggerListener> listeners = new LinkedHashSet<>();
    private JavaScriptDebugger javaScriptDebugger;
    private DebugInformationProvider debugInformationProvider;
    private List<JavaScriptBreakpoint> temporaryBreakpoints = new ArrayList<>();
    private Map<JavaScriptScript, DebugInformation> debugInformationMap = new HashMap<>();
    private Map<String, Set<DebugInformation>> debugInformationFileMap = new HashMap<>();
    private Map<JavaScriptScript, LineInfo> wasmLineInfoMap = new HashMap<>();
    private Map<LineInfo, JavaScriptScript> wasmScriptMap = new HashMap<>();
    private Map<String, Set<LineInfo>> wasmInfoFileMap = new HashMap<>();
    private Map<DebugInformation, JavaScriptScript> scriptMap = new HashMap<>();
    private Map<JavaScriptBreakpoint, Breakpoint> breakpointMap = new HashMap<>();
    private Set<Breakpoint> breakpoints = new LinkedHashSet<>();
    private Set<? extends Breakpoint> readonlyBreakpoints = Collections.unmodifiableSet(breakpoints);
    private CallFrame[] callStack;
    private Set<String> scriptNames = new LinkedHashSet<>();
    private Set<String> allSourceFiles = new LinkedHashSet<>();

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
        var callStack = getCallStack();
        if (callStack == null || callStack.length == 0) {
            return jsStep(enterMethod);
        }
        var recentFrame = callStack[0];
        if (recentFrame.getLocation() == null || recentFrame.getLocation().getFileName() == null
                || recentFrame.getLocation().getLine() < 0) {
            return jsStep(enterMethod);
        }
        var successors = new HashSet<JavaScriptLocation>();
        boolean first = true;
        loop:
        for (var frame : callStack) {
            var script = frame.getOriginalLocation().getScript();
            switch (script.getLanguage()) {
                case JS:
                    if (!addJsBreakpoints(frame, script, enterMethod, first, successors)) {
                        break loop;
                    }
                    break;
                case WASM: {
                    var info = wasmLineInfoMap.get(script);
                    if (info != null) {
                        return enterMethod ? javaScriptDebugger.stepInto() : javaScriptDebugger.stepOver();
                    }
                    break;
                }
            }
            enterMethod = false;
            first = false;
        }

        var jsBreakpointPromises = new ArrayList<Promise<Void>>();
        for (var successor : successors) {
            jsBreakpointPromises.add(javaScriptDebugger.createBreakpoint(successor)
                    .thenVoid(temporaryBreakpoints::add));
        }
        return Promise.allVoid(jsBreakpointPromises).thenAsync(v -> javaScriptDebugger.resume());
    }

    private boolean addJsBreakpoints(CallFrame frame, JavaScriptScript script, boolean enterMethod, boolean first,
            Set<JavaScriptLocation> successors) {
        var debugInfo = debugInformationMap.get(script);
        boolean exits;
        if (frame.getLocation() != null && frame.getLocation().getFileName() != null
                && frame.getLocation().getLine() >= 0 && debugInfo != null) {
            exits = addFollowing(debugInfo, frame.getLocation(), script, new HashSet<>(), successors);
            if (enterMethod) {
                var successorFinder = new CallSiteSuccessorFinder(debugInfo, script, successors);
                var callSites = debugInfo.getCallSites(frame.getLocation());
                for (var callSite : callSites) {
                    callSite.acceptVisitor(successorFinder);
                }
            }
        } else {
            exits = true;
        }
        if (!exits) {
            return false;
        }
        if (!first && frame.getLocation() != null) {
            for (var location : debugInfo.getGeneratedLocations(frame.getLocation())) {
                successors.add(new JavaScriptLocation(script, location.getLine(), location.getColumn()));
            }
        }
        return false;
    }

    static class CallSiteSuccessorFinder implements DebuggerCallSiteVisitor {
        private DebugInformation debugInfo;
        private JavaScriptScript script;
        Set<JavaScriptLocation> locations;

        CallSiteSuccessorFinder(DebugInformation debugInfo, JavaScriptScript script,
                Set<JavaScriptLocation> locations) {
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

    private boolean addFollowing(DebugInformation debugInfo, SourceLocation location, JavaScriptScript script,
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
        var list = debugInformationFileMap.get(sourceFile);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    private List<LineInfo> wasmLineInfoBySource(String sourceFile) {
        var list = wasmInfoFileMap.get(sourceFile);
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
                var jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
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
        return allSourceFiles;
    }

    public Promise<Breakpoint> createBreakpoint(SourceLocation location) {
        var breakpoint = new Breakpoint(this, location);
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

        var promises = new ArrayList<Promise<Void>>();
        for (var jsBreakpoint : breakpoint.jsBreakpoints) {
            breakpointMap.remove(jsBreakpoint);
            promises.add(jsBreakpoint.destroy());
        }

        var jsBreakpoints = new ArrayList<JavaScriptBreakpoint>();
        var location = breakpoint.getLocation();
        for (var debugInformation : debugInformationBySource(location.getFileName())) {
            var locations = debugInformation.getGeneratedLocations(location);
            for (var genLocation : locations) {
                var jsLocation = new JavaScriptLocation(scriptMap.get(debugInformation),
                        genLocation.getLine(), genLocation.getColumn());
                promises.add(javaScriptDebugger.createBreakpoint(jsLocation).thenVoid(jsBreakpoint -> {
                    jsBreakpoints.add(jsBreakpoint);
                    breakpointMap.put(jsBreakpoint, breakpoint);
                }));
            }
        }
        for (var wasmLineInfo : wasmLineInfoBySource(location.getFileName())) {
            for (var sequence : wasmLineInfo.sequences()) {
                for (var loc : sequence.unpack().locations()) {
                    if (loc.location().line() == location.getLine()
                            && loc.location().file().fullName().equals(location.getFileName())) {
                        var jsLocation = new JavaScriptLocation(wasmScriptMap.get(wasmLineInfo),
                                0, loc.address());
                        promises.add(javaScriptDebugger.createBreakpoint(jsLocation).thenVoid(jsBreakpoint -> {
                            jsBreakpoints.add(jsBreakpoint);
                            breakpointMap.put(jsBreakpoint, breakpoint);
                        }));
                    }
                }
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
            var frames = new ArrayList<CallFrame>();
            boolean wasEmpty = false;
            for (var jsFrame : javaScriptDebugger.getCallStack()) {
                List<SourceLocationWithMethod> locations;
                DebugInformation debugInformation = null;
                switch (jsFrame.getLocation().getScript().getLanguage()) {
                    case JS:
                        debugInformation = debugInformationMap.get(jsFrame.getLocation().getScript());
                        locations = mapJsFrames(jsFrame, debugInformation);
                        break;
                    case WASM:
                        locations = mapWasmFrames(jsFrame);
                        break;
                    default:
                        locations = Collections.emptyList();
                        break;
                }
                for (var locWithMethod : locations) {
                    var loc = locWithMethod.loc;
                    var method = locWithMethod.method;
                    if (!locWithMethod.empty || !wasEmpty) {
                        frames.add(new CallFrame(this, jsFrame, loc, method, debugInformation));
                    }
                    wasEmpty = locWithMethod.empty;
                }
            }
            callStack = frames.toArray(new CallFrame[0]);
        }
        return callStack.clone();
    }

    private static class SourceLocationWithMethod {
        private final boolean empty;
        private final SourceLocation loc;
        private final MethodReference method;

        SourceLocationWithMethod(boolean empty, SourceLocation loc, MethodReference method) {
            this.empty = empty;
            this.loc = loc;
            this.method = method;
        }
    }

    private List<SourceLocationWithMethod> mapJsFrames(JavaScriptCallFrame frame,
            DebugInformation debugInformation) {
        SourceLocation loc;
        if (debugInformation != null) {
            loc = debugInformation.getSourceLocation(frame.getLocation().getLine(),
                    frame.getLocation().getColumn());
        } else {
            loc = null;
        }
        boolean empty = loc == null || (loc.getFileName() == null && loc.getLine() < 0);
        var method = !empty && debugInformation != null
                ? debugInformation.getMethodAt(frame.getLocation().getLine(), frame.getLocation().getColumn())
                : null;
        return Collections.singletonList(new SourceLocationWithMethod(empty, loc, method));
    }

    private List<SourceLocationWithMethod> mapWasmFrames(JavaScriptCallFrame frame) {
        var lineInfo = wasmLineInfoMap.get(frame.getLocation().getScript());
        if (lineInfo == null) {
            return Collections.emptyList();
        }
        var sequence = lineInfo.find(frame.getLocation().getColumn());
        if (sequence == null) {
            return Collections.emptyList();
        }
        var instructionLocation = sequence.unpack().find(frame.getLocation().getColumn());
        if (instructionLocation == null) {
            return Collections.emptyList();
        }

        var location = instructionLocation.location();
        var result = new ArrayList<SourceLocationWithMethod>();
        while (true) {
            var loc = new SourceLocation(location.file().fullName(), location.line());
            var inlining = location.inlining();
            var method = inlining != null ? inlining.method() : sequence.method();
            result.add(new SourceLocationWithMethod(false, loc, getMethodReference(method)));
            if (inlining == null) {
                break;
            }
            location = inlining.location();
        }
        return result;
    }

    private MethodReference getMethodReference(MethodInfo methodInfo) {
        return new MethodReference(methodInfo.cls().fullName(), methodInfo.name(), ValueType.VOID);
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

    private void addScript(JavaScriptScript script) {
        Promise<Void> promise;
        switch (script.getLanguage()) {
            case JS:
                promise = addJavaScriptScript(script);
                break;
            case WASM:
                promise = addWasmScript(script);
                break;
            default:
                promise = Promise.VOID;
                break;
        }
        promise.thenVoid(v -> updateBreakpoints());
    }

    private Promise<Void> addJavaScriptScript(JavaScriptScript script) {
        var debugInfo = debugInformationProvider.getDebugInformation(script.getUrl());
        if (debugInfo == null) {
            return Promise.VOID;
        }
        debugInformationMap.put(script, debugInfo);
        for (var sourceFile : debugInfo.getFilesNames()) {
            var list = debugInformationFileMap.get(sourceFile);
            if (list == null) {
                list = new HashSet<>();
                debugInformationFileMap.put(sourceFile, list);
                allSourceFiles.add(sourceFile);
            }
            list.add(debugInfo);
        }
        scriptMap.put(debugInfo, script);
        return Promise.VOID;
    }

    private Promise<Void> addWasmScript(JavaScriptScript script) {
        return script.getSource().thenVoid(source -> {
            if (source == null) {
                return;
            }
            var decoder = Base64.getDecoder();
            var reader = new DebugInfoReaderImpl(decoder.decode(source));
            var parser = reader.read();
            if (parser.getLineInfo() != null) {
                wasmLineInfoMap.put(script, parser.getLineInfo());
                wasmScriptMap.put(parser.getLineInfo(), script);
                for (var sequence : parser.getLineInfo().sequences()) {
                    for (var command : sequence.commands()) {
                        if (command instanceof LineInfoFileCommand) {
                            addWasmInfoFile(((LineInfoFileCommand) command).file().fullName(),
                                    parser.getLineInfo());
                        }
                    }
                }
            }
        });
    }

    private void addWasmInfoFile(String sourceFile, LineInfo wasmLineInfo) {
        var list = wasmInfoFileMap.get(sourceFile);
        if (list == null) {
            list = new HashSet<>();
            wasmInfoFileMap.put(sourceFile, list);
        }
        list.add(wasmLineInfo);
        allSourceFiles.add(sourceFile);
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
        public void scriptAdded(JavaScriptScript script) {
            addScript(script);
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
