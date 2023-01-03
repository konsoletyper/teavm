/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.tooling.deobfuscate.js;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.GeneratedLocation;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSRegExp;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.model.MethodReference;

public final class Deobfuscator {
    private static final JSRegExp FRAME_PATTERN = JSRegExp.create(""
            + "(^ +at ([^(]+) *\\((.+):([0-9]+):([0-9]+)\\) *$)|"
            + "(^([^@]*)@(.+):([0-9]+):([0-9]+)$)");
    private DebugInformation debugInformation;
    private String classesFileName;

    public Deobfuscator(ArrayBuffer buffer, String classesFileName) throws IOException {
        Int8Array array = Int8Array.create(buffer);
        debugInformation = DebugInformation.read(new Int8ArrayInputStream(array));
        this.classesFileName = classesFileName;
    }

    public static void main(String[] args) {
        loadDeobfuscator(args[0], args[1]);
    }

    private static void loadDeobfuscator(String fileName, String classesFileName) {
        XMLHttpRequest xhr = XMLHttpRequest.create();
        xhr.setResponseType("arraybuffer");
        xhr.onComplete(() -> {
            installDeobfuscator(xhr.getResponse().cast(), classesFileName);
        });
        xhr.open("GET", fileName);
        xhr.send();
    }

    public Frame[] deobfuscate(String stack) {
        List<Frame> frames = new ArrayList<>();
        for (String line : splitLines(stack)) {
            JSArray<JSString> groups = FRAME_PATTERN.exec(JSString.valueOf(line));
            if (groups == null) {
                continue;
            }

            int groupOffset = 1;
            if (JSObjects.isUndefined(groups.get(1))) {
                groupOffset = 6;
            }

            String functionName = groups.get(1 + groupOffset).stringValue();
            String fileName = groups.get(2 + groupOffset).stringValue();
            int lineNumber = Integer.parseInt(groups.get(3 + groupOffset).stringValue());
            int columnNumber = Integer.parseInt(groups.get(4 + groupOffset).stringValue());
            List<Frame> framesPerLine = deobfuscateFrames(debugInformation, classesFileName, fileName,
                    lineNumber, columnNumber);
            if (framesPerLine == null) {
                framesPerLine = Arrays.asList(createDefaultFrame(fileName, functionName, lineNumber));
            }
            frames.addAll(framesPerLine);
        }
        return frames.toArray(new Frame[0]);
    }

    private static void installDeobfuscator(ArrayBuffer buffer, String classesFileName) {
        Deobfuscator deobfuscator;
        try {
            deobfuscator = new Deobfuscator(buffer, classesFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        setDeobfuscateFunction(deobfuscator::deobfuscate);
        DeobfuscatorCallback callback = getCallback();
        if (callback != null) {
            callback.run();
        }
    }

    private static List<Frame> deobfuscateFrames(DebugInformation debugInformation, String classesFileName,
            String fileName, int lineNumber, int columnNumber) {
        if (!fileName.equals(classesFileName)) {
            return null;
        }

        List<Frame> result = new ArrayList<>();

        for (int layer = 0; layer < debugInformation.layerCount(); ++layer) {
            GeneratedLocation jsLocation = new GeneratedLocation(lineNumber - 1, columnNumber - 1);
            MethodReference method = debugInformation.getMethodAt(jsLocation, layer);
            if (method == null) {
                break;
            }

            SourceLocation location = debugInformation.getSourceLocation(jsLocation, layer);

            String decodedFileName = location != null ? location.getFileName() : null;
            if (decodedFileName != null) {
                decodedFileName = decodedFileName.substring(decodedFileName.lastIndexOf('/') + 1);
            }

            Frame frame = createEmptyFrame();
            frame.setClassName(method.getClassName());
            frame.setMethodName(method.getName());
            frame.setFileName(decodedFileName);
            if (location != null) {
                frame.setLineNumber(location.getLine());
            }
            result.add(frame);
        }

        if (result.isEmpty()) {
            return null;
        }
        Collections.reverse(result);
        return result;
    }

    private static Frame createDefaultFrame(String fileName, String functionName, int lineNumber) {
        Frame frame = createEmptyFrame();
        frame.setFileName(fileName);
        frame.setMethodName(functionName != null ? functionName : "<unknown function>");
        frame.setClassName("<JS>");
        frame.setLineNumber(lineNumber);
        return frame;
    }

    private static String[] splitLines(String text) {
        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int next = text.indexOf('\n', index);
            if (next < 0) {
                next = text.length();
            }
            result.add(text.substring(index, next));
            index = next + 1;
        }
        return result.toArray(new String[0]);
    }

    @JSBody(script = "return {};")
    private static native Frame createEmptyFrame();

    @JSBody(params = "f", script = "window.$rt_decodeStack = f;")
    private static native void setDeobfuscateFunction(DeobfuscateFunction f);

    @JSBody(script = "return typeof $teavm_deobfuscator_callback === 'function'"
            + "? $teavm_deobfuscator_callback : null;")
    private static native DeobfuscatorCallback getCallback();
}
