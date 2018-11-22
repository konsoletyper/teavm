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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.model.MethodReference;

final class RhinoResultParser {
    private static Pattern pattern = Pattern.compile("(([A-Za-z_$]+)\\(\\))?@.+:([0-9]+)");
    private static Pattern lineSeparator = Pattern.compile("\\r\\n|\r|\n");

    private RhinoResultParser() {
    }

    static void parseResult(Scriptable result, TestRunCallback callback, File debugFile) {
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
                DebugInformation debugInformation = getDebugInformation(debugFile);

                String className = String.valueOf(result.get("className", result));
                String decodedName = debugInformation.getClassNameByJsName(className);
                if (decodedName != null) {
                    className = decodedName;
                }
                String message = String.valueOf(result.get("message", result));

                String stack = result.get("stack", result).toString();
                String[] script = getScript(new File(debugFile.getParentFile(),
                        debugFile.getName().substring(0, debugFile.getName().length() - 9)));
                stack = decodeStack(stack, script, debugInformation);

                if (className.equals("java.lang.AssertionError")) {
                    callback.error(new AssertionError(message + stack));
                } else {
                    callback.error(new RuntimeException(className + ": " + message + stack));
                }
                break;
            }
        }
    }

    private static String decodeStack(String stack, String[] script, DebugInformation debugInformation) {
        StringBuilder sb = new StringBuilder();
        for (String line : lineSeparator.split(stack)) {
            sb.append("\n\tat ");
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                sb.append(line);
                continue;
            }

            String functionName = matcher.group(2);
            int lineNumber = Integer.parseInt(matcher.group(3)) - 1;

            String scriptLine = script[lineNumber];
            int column = firstNonSpace(scriptLine);
            MethodReference method = debugInformation.getMethodAt(lineNumber, column);

            if (method != null) {
                sb.append(method.getClassName()).append(".").append(method.getName());
            } else {
                sb.append(functionName != null ? functionName : "<unknown_function>");
            }

            sb.append("(");
            SourceLocation location = debugInformation.getSourceLocation(lineNumber, column);
            if (location != null && location.getFileName() != null) {
                String fileName = location.getFileName();
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                sb.append(fileName).append(":").append(location.getLine());
            } else {
                sb.append("test.js:").append(lineNumber + 1);
            }
            sb.append(")");
        }

        return sb.toString();
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
