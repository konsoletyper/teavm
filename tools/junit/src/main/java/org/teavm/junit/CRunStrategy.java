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
package org.teavm.junit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CRunStrategy implements TestRunStrategy {
    private String compilerCommand;
    private String wrapperCommand;
    private ConcurrentMap<String, Compilation> compilationMap = new ConcurrentHashMap<>();

    CRunStrategy(String compilerCommand, String wrapperCommand) {
        this.compilerCommand = compilerCommand;
        this.wrapperCommand = wrapperCommand;
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        try {
            String exeName = "run_test";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                exeName += ".exe";
            }

            var sourcesDir = new File(run.getGroup().getBaseDirectory(), run.getGroup().getFileName());
            var outputFile = new File(sourcesDir, exeName);
            var compilerSuccess = compile(sourcesDir);
            if (!compilerSuccess) {
                throw new RuntimeException("C compiler error");
            }

            outputFile.setExecutable(true);
            synchronized (this) {
                var exceptionFile = File.createTempFile("teavm-test-exception", ".txt");
                try {
                    List<String> runCommand = new ArrayList<>();
                    if (wrapperCommand != null) {
                        runCommand.addAll(List.of(wrapperCommand.split(" ")));
                    }
                    runCommand.add(outputFile.getPath());
                    if (run.getArgument() != null) {
                        runCommand.add(run.getArgument());
                    }

                    var pb = new ProcessBuilder(runCommand.toArray(new String[0]))
                            .redirectInput(ProcessBuilder.Redirect.INHERIT);
                    pb.environment().put("TEAVM_TEST_EXCEPTION_FILE", exceptionFile.getAbsolutePath());
                    var process = pb.start();

                    var stdoutBytes = new ByteArrayOutputStream();
                    var stderrBytes = new ByteArrayOutputStream();
                    var stdoutThread = captureStream(process.getInputStream(), stdoutBytes);
                    var stderrThread = captureStream(process.getErrorStream(), stderrBytes);
                    int exitCode = process.waitFor();
                    stdoutThread.join();
                    stderrThread.join();

                    if (stdoutBytes.size() > 0) {
                        System.out.write(stdoutBytes.toByteArray());
                        System.out.flush();
                    }
                    if (stderrBytes.size() > 0) {
                        System.err.write(stderrBytes.toByteArray());
                        System.err.flush();
                    }

                    if (exitCode != 0) {
                        Throwable parsed = parseExceptionFile(exceptionFile);
                        if (parsed != null) {
                            sneakyThrow(parsed);
                        }
                        String message = exitCode > 128
                                ? "Test crashed with signal " + (exitCode - 128)
                                : "Test failed with exit code " + exitCode;
                        throw new RuntimeException(message);
                    }
                } finally {
                    exceptionFile.delete();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable e) throws T {
        throw (T) e;
    }

    private static Throwable parseExceptionFile(File file) {
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        var lines = new ArrayList<String>();
        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            return null;
        }
        return parseException(lines);
    }

    private static Throwable parseException(List<String> lines) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equals("TEAVM_EXCEPTION_START")) {
                start = i;
            } else if (line.equals("TEAVM_EXCEPTION_END") && start >= 0) {
                end = i;
                break;
            }
        }
        if (start < 0 || end <= start) {
            return null;
        }
        return buildException(lines, start + 1, end);
    }

    private static Throwable buildException(List<String> lines, int from, int to) {
        String className = null;
        String message = null;
        var frames = new ArrayList<StackTraceElement>();
        int causeStart = -1;

        for (int i = from; i < to; i++) {
            String line = lines.get(i);
            if (line.startsWith("TEAVM_CLASS:")) {
                className = line.substring("TEAVM_CLASS:".length());
            } else if (line.startsWith("TEAVM_MESSAGE:")) {
                String raw = line.substring("TEAVM_MESSAGE:".length());
                message = raw.isEmpty() ? null : unescape(raw);
            } else if (line.startsWith("TEAVM_AT:")) {
                StackTraceElement frame = parseStackFrame(line.substring("TEAVM_AT:".length()));
                if (frame != null) {
                    frames.add(frame);
                }
            } else if (line.equals("TEAVM_CAUSE")) {
                causeStart = i + 1;
                break;
            }
        }

        if (className == null) {
            return null;
        }

        Throwable cause = null;
        if (causeStart >= 0) {
            cause = buildException(lines, causeStart, to);
        }

        Throwable result = constructException(className, message, cause);
        result.setStackTrace(frames.toArray(new StackTraceElement[0]));
        return result;
    }

    private static Throwable constructException(String className, String message, Throwable cause) {
        try {
            Class<?> cls = Class.forName(className);
            if (Throwable.class.isAssignableFrom(cls)) {
                Throwable result = tryConstructors(cls, message, cause);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception ignored) {
            // class not found or not accessible
        }
        String fallbackMessage = className + (message != null && !message.isEmpty() ? ": " + message : "");
        return cause != null ? new RuntimeException(fallbackMessage, cause) : new RuntimeException(fallbackMessage);
    }

    private static Throwable tryConstructors(Class<?> cls, String message, Throwable cause) {
        if (cause != null) {
            try {
                return (Throwable) cls.getConstructor(String.class, Throwable.class).newInstance(message, cause);
            } catch (Exception ignored) {
                // try next
            }
        }
        try {
            Throwable result = (Throwable) cls.getConstructor(String.class).newInstance(message);
            if (cause != null) {
                result.initCause(cause);
            }
            return result;
        } catch (Exception ignored) {
            // try next
        }
        try {
            Throwable result = (Throwable) cls.getConstructor(Object.class).newInstance(message);
            if (cause != null) {
                result.initCause(cause);
            }
            return result;
        } catch (Exception ignored) {
            // try next
        }
        try {
            Throwable result = (Throwable) cls.getConstructor().newInstance();
            if (cause != null) {
                result.initCause(cause);
            }
            return result;
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static String unescape(String s) {
        if (!s.contains("\\")) {
            return s;
        }
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c); sb.append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static StackTraceElement parseStackFrame(String s) {
        int parenOpen = s.indexOf('(');
        int parenClose = s.lastIndexOf(')');
        if (parenOpen < 0 || parenClose < parenOpen) {
            return null;
        }
        String classAndMethod = s.substring(0, parenOpen);
        String fileInfo = s.substring(parenOpen + 1, parenClose);

        int lastDot = classAndMethod.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        String declaringClass = classAndMethod.substring(0, lastDot);
        String methodName = classAndMethod.substring(lastDot + 1);

        if (fileInfo.equals("Native Method")) {
            return new StackTraceElement(declaringClass, methodName, null, -2);
        }
        if (fileInfo.equals("Unknown Source")) {
            return new StackTraceElement(declaringClass, methodName, null, -1);
        }

        int colon = fileInfo.lastIndexOf(':');
        if (colon >= 0) {
            String fileName = fileInfo.substring(0, colon);
            try {
                int line = Integer.parseInt(fileInfo.substring(colon + 1));
                return new StackTraceElement(declaringClass, methodName, fileName, line);
            } catch (NumberFormatException e) {
                return new StackTraceElement(declaringClass, methodName, fileInfo, -1);
            }
        }
        return new StackTraceElement(declaringClass, methodName, fileInfo, -1);
    }

    private static Thread captureStream(java.io.InputStream source, ByteArrayOutputStream sink) {
        var thread = new Thread(() -> {
            try {
                source.transferTo(sink);
            } catch (IOException ignored) {
                // ignore
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private boolean compile(File inputDir) throws IOException, InterruptedException {
        Compilation compilation = compilationMap.computeIfAbsent(inputDir.getPath(), k -> new Compilation());
        synchronized (compilation) {
            if (!compilation.started) {
                compilation.started = true;
                compilation.success = doCompile(inputDir);
            }
        }
        return compilation.success;
    }

    private boolean doCompile(File inputDir) throws IOException, InterruptedException {
        String command = new File(compilerCommand).getAbsolutePath();
        var process = new ProcessBuilder(command)
                .directory(inputDir)
                .start();
        var stdoutBytes = new ByteArrayOutputStream();
        var stderrBytes = new ByteArrayOutputStream();
        var stdoutThread = captureStream(process.getInputStream(), stdoutBytes);
        var stderrThread = captureStream(process.getErrorStream(), stderrBytes);
        stdoutThread.join();
        stderrThread.join();
        if (stdoutBytes.size() > 0) {
            System.out.write(stdoutBytes.toByteArray());
            System.out.flush();
        }
        if (stderrBytes.size() > 0) {
            System.err.write(stderrBytes.toByteArray());
            System.err.flush();
        }
        return process.waitFor() == 0;
    }

    @Override
    public void cleanup() {
    }

    static class Compilation {
        volatile boolean started;
        volatile boolean success;
    }
}
