/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.teavm.common.Promise;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.DebuggerListener;
import org.teavm.debugging.Variable;
import org.teavm.debugging.information.URLDebugInformationProvider;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptVariable;

public final class ChromeRDPRunner {
    private ChromeRDPServer server;
    private Debugger debugger;
    private Map<Breakpoint, Integer> breakpointIds = new WeakHashMap<>();
    private int currentFrame;
    private int breakpointIdGen;
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    private ChromeRDPRunner() {
        server = new ChromeRDPServer();
        server.setPort(2357);
        ChromeRDPDebugger jsDebugger = new ChromeRDPDebugger(queue::offer);
        server.setExchangeConsumer(jsDebugger);

        new Thread(server::start).start();
        debugger = new Debugger(jsDebugger, new URLDebugInformationProvider(""));
        debugger.addListener(listener);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t);
            e.printStackTrace();
        });
    }

    private DebuggerListener listener = new DebuggerListener() {
        @Override
        public void resumed() {
        }

        @Override
        public void paused(Breakpoint breakpoint) {
            CallFrame[] stack = debugger.getCallStack();
            if (stack.length > 0) {
                System.out.println();
                System.out.println("Suspended at " + stack[0].getLocation());
            }
            if (breakpoint != null) {
                System.out.println("Breakpoint #" + breakpointIds.get(breakpoint) + " hit");
            }
            currentFrame = 0;
        }

        @Override
        public void breakpointStatusChanged(Breakpoint breakpoint) {
        }

        @Override
        public void attached() {
        }

        @Override
        public void detached() {
        }
    };

    public static void main(String[] args) {
        ChromeRDPRunner runner = new ChromeRDPRunner();
        try {
            runner.acceptInput();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void acceptInput() throws InterruptedException {
        boolean wasAttached = debugger.isAttached();
        if (!wasAttached) {
            System.out.println("Waiting for remote process to attach...");
        }

        while (true) {
            queue.take().run();
            if (debugger.isAttached() && !wasAttached) {
                wasAttached = true;
                System.out.println("Attached");
                new Thread(() -> {
                    try {
                        stdinThread();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else if (!debugger.isAttached() && wasAttached) {
                break;
            }
        }

        queue.offer(() -> {
            debugger.detach();
            server.stop();
        });
    }

    private void stdinThread() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            BlockingQueue<Boolean> callbackQueue = new ArrayBlockingQueue<>(1);
            queue.add(() -> {
                processSingleCommand(line).then(r -> callbackQueue.offer(r)).catchError(e -> {
                    e.printStackTrace();
                    return true;
                });
            });
            try {
                if (!callbackQueue.take()) {
                    break;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private Promise<Boolean> processSingleCommand(String line) {
        line = line.trim();
        String[] parts = Arrays.stream(line.split(" +"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (parts.length == 0) {
            return Promise.of(true);
        }

        switch (parts[0]) {
            case "suspend":
                if (debugger.isSuspended()) {
                    System.out.println("Suspend command is only available when program is running");
                    return Promise.of(true);
                } else {
                    return debugger.suspend().then(v -> true);
                }

            case "detach":
                return Promise.of(false);

            case "continue":
            case "cont":
            case "c":
                return suspended(parts, resumeCommand);

            case "breakpoint":
            case "break":
            case "br":
            case "bp":
                return breakpointCommand.execute(parts).then(v -> true);

            case "backtrace":
            case "bt":
                return suspended(parts, backtraceCommand);

            case "frame":
            case "fr":
            case "f":
                return suspended(parts, frameCommand);

            case "step":
            case "s":
                return suspended(parts, stepCommand);

            case "next":
            case "n":
                return suspended(parts, nextCommand);

            case "out":
            case "o":
                return suspended(parts, outCommand);

            case "info":
                return suspended(parts, infoCommand);

            case "print":
            case "p":
                return suspended(parts, printCommand);

            default:
                System.out.println("Unknown command");
                return Promise.of(true);
        }
    }

    private Promise<Boolean> suspended(String[] arguments, Command command) {
        if (!debugger.isSuspended()) {
            System.out.println("This command is only available when remote process is suspended");
            return Promise.of(true);
        }
        return command.execute(arguments).then(v -> true);
    }

    private Command resumeCommand = args -> debugger.resume();

    private Command breakpointCommand = args -> {
        if (args.length != 3 && args.length != 3) {
            System.out.println("Expected 2 arguments");
            return Promise.VOID;
        }

        if (args.length == 4) {
            return tryResolveJsBreakpoint(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        }

        String[] fileNames = resolveFileName(args[1]);
        if (fileNames.length == 0) {
            return tryResolveJsBreakpoint(args[1], Integer.parseInt(args[2]),
                    args.length == 3 ? 1 : Integer.parseInt(args[3]));
        } else if (fileNames.length > 1) {
            System.out.println("Ambiguous file name: " + args[1] + ". Possible names are: "
                    + Arrays.toString(fileNames));
            return Promise.VOID;
        }

        return debugger.createBreakpoint(fileNames[0], Integer.parseInt(args[2])).thenVoid(bp -> {
            int id = breakpointIdGen++;
            breakpointIds.put(bp, id);
            System.out.println("Breakpoint #" + id + " was set at " + bp.getLocation());
        });
    };

    private Promise<Void> tryResolveJsBreakpoint(String fileName, int lineNumber, int columnNumber) {
        String[] fileNames = resolveJsFileName(fileName);
        if (fileNames.length == 0) {
            System.out.println("Unknown file: " + fileName);
            return Promise.VOID;
        } else if (fileNames.length > 1) {
            System.out.println("Ambiguous file name: " + fileName + ". Possible names are: "
                    + Arrays.toString(fileNames));
            return Promise.VOID;
        }

        JavaScriptLocation location = new JavaScriptLocation(fileNames[0], lineNumber - 1, columnNumber - 1);
        return debugger.getJavaScriptDebugger().createBreakpoint(location).thenVoid(bp -> {
            System.out.println("Native breakpoint was set at " + bp.getLocation());
        });
    }

    private String[] resolveJsFileName(String fileName) {
        if (debugger.getScriptNames().contains(fileName)) {
            return new String[] { fileName };
        }

        String[] result = debugger.getScriptNames().stream()
                .filter(f -> f.endsWith(fileName) && isPrecededByPathSeparator(f, fileName))
                .toArray(String[]::new);
        if (result.length == 1) {
            return result;
        }

        return debugger.getSourceFiles().stream()
                .filter(f -> {
                    int index = f.lastIndexOf('.');
                    if (index <= 0) {
                        return false;
                    }
                    String nameWithoutExt = f.substring(0, index);
                    return nameWithoutExt.endsWith(fileName)  && isPrecededByPathSeparator(nameWithoutExt, fileName);
                })
                .toArray(String[]::new);
    }

    private String[] resolveFileName(String fileName) {
        if (debugger.getSourceFiles().contains(fileName)) {
            return new String[] { fileName };
        }

        String[] result = debugger.getSourceFiles().stream()
                .filter(f -> f.endsWith(fileName) && isPrecededByPathSeparator(f, fileName))
                .toArray(String[]::new);
        if (result.length == 1) {
            return result;
        }

        return debugger.getSourceFiles().stream()
                .filter(f -> {
                    int index = f.lastIndexOf('.');
                    if (index <= 0) {
                        return false;
                    }
                    String nameWithoutExt = f.substring(0, index);
                    return nameWithoutExt.endsWith(fileName)  && isPrecededByPathSeparator(nameWithoutExt, fileName);
                })
                .toArray(String[]::new);
    }

    private static boolean isPrecededByPathSeparator(String actualName, String specifiedName) {
        if (actualName.length() < specifiedName.length() + 1) {
            return false;
        }
        char c = actualName.charAt(actualName.length() - specifiedName.length() - 1);
        return c == '/' || c == '\\';
    }

    private Command backtraceCommand = args -> {
        CallFrame[] callStack = debugger.getCallStack();
        for (int i = 0; i < callStack.length; ++i) {
            StringBuilder sb = new StringBuilder(i == currentFrame ? " -> " : "    ");
            sb.append("#").append(i).append(": ");
            CallFrame frame = callStack[i];
            if (frame.getMethod() != null) {
                sb.append(frame.getMethod().getClassName()).append('.').append(frame.getMethod().getName());
            } else {
                sb.append("[unknown method]");
            }
            if (frame.getLocation() != null) {
                sb.append('(').append(frame.getLocation()).append(')');
            }
            System.out.println(sb.toString());
        }
        return Promise.VOID;
    };

    private Command frameCommand = args -> {
        if (args.length != 2) {
            System.out.println("Expected 1 argument");
            return Promise.VOID;
        }
        int index = Integer.parseInt(args[1]);
        int max = debugger.getCallStack().length - 1;
        if (index < 0 || index > max) {
            System.out.println("Given frame index is outside of valid range 0.." + max);
            return Promise.VOID;
        }
        currentFrame = index;
        return Promise.VOID;
    };

    private Command stepCommand = args -> debugger.stepInto();

    private Command nextCommand = args -> debugger.stepOver();

    private Command outCommand = args -> debugger.stepOut();

    private Command infoCommand = args -> {
        if (args.length != 2) {
            System.out.println("Expected 1 argument");
            return Promise.VOID;
        }

        switch (args[1]) {
            case "breakpoints": {
                List<Breakpoint> sortedBreakpoints = debugger.getBreakpoints().stream()
                        .sorted(Comparator.comparing(breakpointIds::get))
                        .collect(Collectors.toList());
                for (Breakpoint breakpoint : sortedBreakpoints) {
                    int id = breakpointIds.get(breakpoint);
                    System.out.println("    #" + id + ": " + breakpoint.getLocation());
                }
                return Promise.VOID;
            }

            case "variables": {
                CallFrame frame = debugger.getCallStack()[currentFrame];
                return printScope(frame.getVariables());
            }

            default:
                System.out.println("Invalid argument");
                return Promise.VOID;
        }
    };

    private Command printCommand = args -> {
        if (args.length != 2) {
            System.out.println("Expected 1 argument");
            return Promise.VOID;
        }

        String[] path = args[1].split("\\.");
        return followPath(path, 0, debugger.getCallStack()[currentFrame].getVariables());
    };

    private Promise<Void> followPath(String[] path, int index, Promise<Map<String, Variable>> scope) {
        String elem = path[index];
        return scope.thenAsync(map -> {
            Variable var = map.get(elem);
            if (var != null) {
                if (index == path.length - 1) {
                    return variableToString(var)
                            .thenVoid(str -> System.out.println(str))
                            .thenAsync(v -> var.getValue().getType().thenAsync(type -> type.startsWith("@")
                                    ? printJsScope(var.getValue().getOriginalValue().getProperties())
                                    : printScope(var.getValue().getProperties())));
                } else {
                    return var.getValue().getType().thenAsync(type -> type.startsWith("@")
                            ? followJsPath(path, index + 1, var.getValue().getOriginalValue().getProperties())
                            : followPath(path, index + 1, var.getValue().getProperties()));
                }
            } else {
                System.out.println("Invalid path specified");
                return Promise.VOID;
            }
        });
    }

    private Promise<Void> followJsPath(String[] path, int index,
            Promise<Map<String, ? extends JavaScriptVariable>> scope) {
        String elem = path[index];
        return scope.thenAsync(map -> {
            JavaScriptVariable var = map.get(elem);
            if (var != null) {
                if (index == path.length - 1) {
                    return jsVariableToString(var)
                            .thenVoid(str -> System.out.println(str))
                            .thenAsync(v -> printJsScope(var.getValue().getProperties()));
                } else {
                    return followJsPath(path, index + 1, var.getValue().getProperties());
                }
            } else {
                System.out.println("Invalid path specified");
                return Promise.VOID;
            }
        });
    }

    private Promise<Void> printScope(Promise<Map<String, Variable>> scope) {
        return scope
                .then(vars -> vars.values())
                .then(vars -> vars.stream()
                        .sorted(Comparator.comparing(Variable::getName))
                        .map(this::variableToString)
                        .collect(Collectors.toList())
                )
                .thenAsync(Promise::all)
                .thenVoid(vars -> {
                    for (String var : vars) {
                        System.out.println("    " + var);
                    }
                });
    }

    private Promise<Void> printJsScope(Promise<Map<String, ? extends JavaScriptVariable>> scope) {
        return scope
                .then(vars -> vars.values())
                .then(vars -> vars.stream()
                        .sorted(Comparator.comparing(JavaScriptVariable::getName))
                        .map(this::jsVariableToString)
                        .collect(Collectors.toList())
                )
                .thenAsync(Promise::all)
                .thenVoid(vars -> {
                    for (String var : vars) {
                        System.out.println("    " + var);
                    }
                });
    }

    private Promise<String> variableToString(Variable variable) {
        return variable.getValue().getType()
                .thenAsync(type -> variable.getValue().getRepresentation()
                        .then(repr -> variable.getName() + ": " + type + " (" + repr + ")"));
    }


    private Promise<String> jsVariableToString(JavaScriptVariable variable) {
        return variable.getValue().getClassName()
                .thenAsync(type -> variable.getValue().getRepresentation()
                        .then(repr -> variable.getName() + ": " + type + " (" + repr + ")"));
    }

    private interface Command {
        Promise<Void> execute(String[] args);
    }
}
