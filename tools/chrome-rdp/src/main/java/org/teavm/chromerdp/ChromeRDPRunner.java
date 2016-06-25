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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.DebuggerListener;
import org.teavm.debugging.Variable;
import org.teavm.debugging.information.URLDebugInformationProvider;

public final class ChromeRDPRunner {
    private ChromeRDPServer server;
    private Debugger debugger;
    private Map<Breakpoint, Integer> breakpointIds = new WeakHashMap<>();
    private int currentFrame;
    private int breakpointIdGen;
    private volatile Runnable attachListener;
    private volatile Runnable suspendListener;
    private volatile Runnable resumeListener;

    private ChromeRDPRunner() {
        server = new ChromeRDPServer();
        server.setPort(2357);
        ChromeRDPDebugger jsDebugger = new ChromeRDPDebugger();
        server.setExchangeConsumer(jsDebugger);

        new Thread(server::start).start();
        debugger = new Debugger(jsDebugger, new URLDebugInformationProvider(""));
        debugger.addListener(listener);
    }

    private DebuggerListener listener = new DebuggerListener() {
        @Override
        public void resumed() {
            if (resumeListener != null) {
                resumeListener.run();
            }
        }

        @Override
        public void paused() {
            CallFrame[] stack = debugger.getCallStack();
            if (stack.length > 0) {
                System.out.println();
                System.out.println("Suspended at " + stack[0].getLocation());
            }
            currentFrame = 0;
            if (suspendListener != null) {
                suspendListener.run();
            }
        }

        @Override
        public void breakpointStatusChanged(Breakpoint breakpoint) {
        }

        @Override
        public void attached() {
            if (attachListener != null) {
                attachListener.run();
            }
        }

        @Override
        public void detached() {
        }
    };

    public static void main(String[] args) throws IOException {
        ChromeRDPRunner runner = new ChromeRDPRunner();
        try {
            runner.acceptInput();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    public void acceptInput() throws IOException, InterruptedException {
        if (!debugger.isAttached()) {
            System.out.println("Waiting for remote process to attach...");
            CountDownLatch latch = new CountDownLatch(1);
            attachListener = latch::countDown;
            if (!debugger.isAttached()) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    return;
                }
            }
            attachListener = null;
            System.out.println("Attached");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        loop: while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            line = line.trim();
            String[] parts = Arrays.stream(line.split(" +"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (parts.length == 0) {
                continue;
            }

            switch (parts[0]) {
                case "suspend":
                    if (debugger.isSuspended()) {
                        System.out.println("Suspend command is only available when program is running");
                    } else {
                        CountDownLatch latch = new CountDownLatch(1);
                        suspendListener = latch::countDown;
                        debugger.suspend();
                        latch.await();
                        suspendListener = null;
                    }
                    break;

                case "detach":
                    break loop;

                case "continue":
                case "cont":
                case "c":
                    suspended(parts, resumeCommand);
                    break;

                case "breakpoint":
                case "break":
                case "br":
                case "bp":
                    breakpointCommand.execute(parts);
                    break;

                case "backtrace":
                case "bt":
                    suspended(parts, backtraceCommand);
                    break;

                case "frame":
                case "fr":
                case "f":
                    suspended(parts, frameCommand);
                    break;

                case "step":
                case "s":
                    suspended(parts, stepCommand);
                    break;

                case "next":
                case "n":
                    suspended(parts, nextCommand);
                    break;

                case "out":
                case "o":
                    suspended(parts, outCommand);
                    break;

                case "info":
                    suspended(parts, infoCommand);
                    break;

                default:
                    System.out.println("Unknown command");
            }
        }

        debugger.detach();
        server.stop();
    }

    private void suspended(String[] arguments, Command command) throws InterruptedException {
        if (!debugger.isSuspended()) {
            System.out.println("This command is only available when remote process is suspended");
            return;
        }
        command.execute(arguments);
    }

    private Command resumeCommand = args -> {
        CountDownLatch latch = new CountDownLatch(1);
        resumeListener = latch::countDown;
        debugger.resume();
        latch.await();
        resumeListener = null;
    };

    private Command breakpointCommand = args -> {
        if (args.length != 3) {
            System.out.println("Expected 2 arguments");
            return;
        }
        Breakpoint bp = debugger.createBreakpoint(args[1], Integer.parseInt(args[2]));
        int id = breakpointIdGen++;
        breakpointIds.put(bp, id);
        System.out.println("Breakpoint #" + id + " was set at " + bp.getLocation());
    };

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
    };

    private Command frameCommand = args -> {
        if (args.length != 2) {
            System.out.println("Expected 1 argument");
            return;
        }
        int index = Integer.parseInt(args[1]);
        int max = debugger.getCallStack().length - 1;
        if (index < 0 || index > max) {
            System.out.println("Given frame index is outside of valid range 0.." + max);
            return;
        }
        currentFrame = index;
    };

    private Command stepCommand = args -> debugger.stepInto();

    private Command nextCommand = args -> debugger.stepOver();

    private Command outCommand = args -> debugger.stepOut();

    private Command infoCommand = args -> {
        if (args.length != 2) {
            System.out.println("Expected 1 argument");
            return;
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
                break;
            }

            case "variables": {
                CallFrame frame = debugger.getCallStack()[currentFrame];
                for (Variable var : frame.getVariables().values().stream()
                        .sorted(Comparator.comparing(Variable::getName))
                        .collect(Collectors.toList())) {
                    System.out.println("    " + var.getName() + ": " + var.getValue().getType());
                }
                break;
            }

            default:
                System.out.println("Invalid argument");
        }
    };

    private interface Command {
        void execute(String[] args) throws InterruptedException;
    }
}
