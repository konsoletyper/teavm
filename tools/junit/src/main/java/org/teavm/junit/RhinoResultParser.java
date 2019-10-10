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

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.GeneratedLocation;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.model.MethodReference;

final class RhinoResultParser {
    private static Pattern pattern = Pattern.compile("(([A-Za-z_$][A-Za-z0-9_$]*)\\(\\))?@.+:([0-9]+)");
    private static Pattern lineSeparator = Pattern.compile("\\r\\n|\r|\n");
    private DebugInformation debugInformation;
    private String[] script;

    RhinoResultParser(File debugFile) {
        if (debugFile != null) {
            debugInformation = getDebugInformation(debugFile);
            script = getScript(new File(debugFile.getParentFile(),
                    debugFile.getName().substring(0, debugFile.getName().length() - 9)));
        }
    }

    void parseResult(Scriptable result, TestRunCallback callback) {
        if (result == null) {
            callback.complete();
            return;
        }
        String status = result.get("status", result).toString();
        switch (status) {
            case "ok":
                callback.complete();
                break;
            case "exception": {
                String className = String.valueOf(result.get("className", result));
                if (debugInformation != null) {
                    String decodedName = debugInformation.getClassNameByJsName(className);
                    if (decodedName != null) {
                        className = decodedName;
                    }
                }
                String message = String.valueOf(result.get("message", result));

                String stack = result.get("stack", result).toString();
                StackTraceElement[] decodedStack = null;
                if (debugInformation != null) {
                    List<StackTraceElement> elements = new ArrayList<>(Arrays.asList(decodeStack(stack)));
                    List<StackTraceElement> currentElements = Arrays.asList(Thread.currentThread().getStackTrace());
                    elements.addAll(currentElements.subList(2, currentElements.size()));
                    decodedStack = elements.toArray(new StackTraceElement[0]);
                    stack = "";
                } else {
                    stack = "\n" + stack;
                }

                Throwable e;
                if (className.equals("java.lang.AssertionError")) {
                    e = new AssertionError(message + stack);
                } else {
                    e = new RuntimeException(className + ": " + message + stack);
                }
                if (decodedStack != null) {
                    e.setStackTrace(decodedStack);
                }
                callback.error(e);
                break;
            }
        }
    }

    StackTraceElement[] decodeStack(String stack) {
        List<StackTraceElement> elements = new ArrayList<>();
        for (String line : lineSeparator.split(stack)) {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String functionName = matcher.group(2);
            int jsLineNumber = Integer.parseInt(matcher.group(3)) - 1;

            String scriptLine = script[jsLineNumber];
            int jsColumn = firstNonSpace(scriptLine);

            int layer = 0;
            List<StackTraceElement> elementsByLine = new ArrayList<>();
            GeneratedLocation jsLocation = new GeneratedLocation(jsLineNumber, jsColumn);
            while (true) {
                int lineNumber = jsLineNumber;

                MethodReference method = debugInformation.getMethodAt(jsLocation, layer);
                String className;
                String methodName;

                if (method != null) {
                    className = method.getClassName();
                    methodName = method.getName();
                } else if (layer > 0) {
                    break;
                } else {
                    className = "<JS>";
                    methodName = functionName != null ? functionName : "<unknown_function>";
                }

                String fileName;
                SourceLocation location = debugInformation.getSourceLocation(jsLocation, layer);
                if (location != null && location.getFileName() != null) {
                    fileName = location.getFileName();
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    lineNumber = location.getLine();
                } else {
                    fileName = "test.js";
                    lineNumber++;
                }

                elementsByLine.add(new StackTraceElement(className, methodName, fileName, lineNumber));

                ++layer;
            }

            Collections.reverse(elementsByLine);
            elements.addAll(elementsByLine);
        }

        return elements.toArray(new StackTraceElement[0]);
    }

    private static DebugInformation getDebugInformation(File debugFile) {
        try (InputStream input = new FileInputStream(debugFile)) {
            return DebugInformation.read(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] getScript(File file) {
        List<String> lines = new ArrayList<>();
        try (InputStream input = new FileInputStream(file);
                Reader reader = new InputStreamReader(input, UTF_8);
                BufferedReader bufferedReader = new BufferedReader(reader)) {
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int firstNonSpace(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i;
    }
}
