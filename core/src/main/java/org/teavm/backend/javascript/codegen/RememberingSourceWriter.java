/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.codegen;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.List;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class RememberingSourceWriter extends SourceWriter {
    static final byte CLASS = 0;
    static final byte FIELD = 1;
    static final byte STATIC_FIELD = 2;
    static final byte METHOD = 3;
    static final byte METHOD_BODY = 4;
    static final byte FUNCTION = 5;
    static final byte GLOBAL = 6;
    static final byte INIT = 7;
    static final byte CLASS_INIT = 8;
    static final byte NEW_LINE = 9;
    static final byte WS = 10;
    static final byte SAME_LINE_WS = 21;
    static final byte TOKEN_BOUNDARY = 11;
    static final byte SOFT_NEW_LINE = 12;
    static final byte INDENT = 13;
    static final byte OUTDENT = 14;
    static final byte EMIT_LOCATION = 15;
    static final byte ENTER_LOCATION = 16;
    static final byte EXIT_LOCATION = 17;
    static final byte EMIT_STATEMENT_START = 18;
    static final byte EMIT_VARIABLES = 26;
    static final byte EMIT_METHOD = 19;
    static final byte EMIT_CLASS = 20;
    static final byte MARK_CLASS_START = 22;
    static final byte MARK_CLASS_END = 23;
    static final byte MARK_SECTION_START = 24;
    static final byte MARK_SECTION_END = 25;

    private boolean debug;

    private StringBuilder sb = new StringBuilder();
    private int lastWrittenChar;
    private IntArrayList intArgs = new IntArrayList();
    private ByteArrayList commands = new ByteArrayList();

    private List<String> strings = new ArrayList<>();
    private ObjectIntMap<String> stringIndexes = new ObjectIntHashMap<>();

    private List<FieldReference> fields = new ArrayList<>();
    private ObjectIntMap<FieldReference> fieldIndexes = new ObjectIntHashMap<>();

    private List<MethodDescriptor> methodDescriptors = new ArrayList<>();
    private ObjectIntMap<MethodDescriptor> methodDescriptorIndexes = new ObjectIntHashMap<>();

    private List<MethodReference> methods = new ArrayList<>();
    private ObjectIntMap<MethodReference> methodIndexes = new ObjectIntHashMap<>();

    public RememberingSourceWriter(boolean debug) {
        this.debug = debug;
    }

    public void clear() {
        sb.setLength(0);
        lastWrittenChar = 0;
        intArgs.clear();
        commands.clear();
        strings.clear();
        stringIndexes.clear();
        fields.clear();
        fieldIndexes.clear();
        methodDescriptors.clear();
        methodDescriptorIndexes.clear();
        methods.clear();
        methodIndexes.clear();
    }

    @Override
    public SourceWriter append(char value) {
        sb.append(value);
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public SourceWriter appendClass(String cls) {
        flush();
        commands.add(CLASS);
        appendStringArg(cls);
        return this;
    }

    @Override
    public SourceWriter appendField(FieldReference field) {
        flush();
        commands.add(FIELD);
        appendFieldArg(field);
        return this;
    }

    @Override
    public SourceWriter appendStaticField(FieldReference field) {
        flush();
        commands.add(STATIC_FIELD);
        appendFieldArg(field);
        return this;
    }

    @Override
    public SourceWriter appendMethod(MethodDescriptor method) {
        flush();
        commands.add(METHOD);
        appendMethodDescriptorArg(method);
        return this;
    }

    @Override
    public SourceWriter appendMethodBody(MethodReference method) {
        flush();
        commands.add(METHOD_BODY);
        appendMethodArg(method);
        return this;
    }

    @Override
    public SourceWriter appendFunction(String name) {
        flush();
        commands.add(FUNCTION);
        appendStringArg(name);
        return this;
    }

    @Override
    public SourceWriter appendGlobal(String name) {
        flush();
        commands.add(GLOBAL);
        appendStringArg(name);
        return this;
    }

    @Override
    public SourceWriter appendInit(MethodReference method) {
        flush();
        commands.add(INIT);
        appendMethodArg(method);
        return this;
    }

    @Override
    public SourceWriter appendClassInit(String className) {
        flush();
        commands.add(CLASS_INIT);
        appendStringArg(className);
        return this;
    }

    @Override
    public SourceWriter newLine() {
        flush();
        commands.add(NEW_LINE);
        return this;
    }

    @Override
    public SourceWriter ws() {
        flush();
        commands.add(WS);
        return this;
    }

    @Override
    public SourceWriter sameLineWs() {
        flush();
        commands.add(SAME_LINE_WS);
        return this;
    }

    @Override
    public SourceWriter tokenBoundary() {
        flush();
        commands.add(TOKEN_BOUNDARY);
        return this;
    }

    @Override
    public SourceWriter softNewLine() {
        flush();
        commands.add(SOFT_NEW_LINE);
        return this;
    }

    @Override
    public SourceWriter indent() {
        flush();
        commands.add(INDENT);
        return this;
    }

    @Override
    public SourceWriter outdent() {
        flush();
        commands.add(OUTDENT);
        return this;
    }

    @Override
    public SourceWriter emitLocation(String fileName, int line) {
        if (debug) {
            flush();
            commands.add(EMIT_LOCATION);
            if (fileName == null) {
                intArgs.add(-1);
            } else {
                appendStringArg(fileName);
            }
            intArgs.add(line);
        }
        return this;
    }

    @Override
    public SourceWriter enterLocation() {
        if (debug) {
            flush();
            commands.add(ENTER_LOCATION);
        }
        return this;
    }

    @Override
    public SourceWriter exitLocation() {
        if (debug) {
            flush();
            commands.add(EXIT_LOCATION);
        }
        return this;
    }

    @Override
    public SourceWriter emitStatementStart() {
        if (debug) {
            flush();
            commands.add(EMIT_STATEMENT_START);
        }
        return this;
    }

    @Override
    public SourceWriter emitVariables(String[] names, String jsName) {
        if (debug) {
            flush();
            commands.add(EMIT_VARIABLES);
            intArgs.add(names.length);
            for (var name : names) {
                appendStringArg(name);
            }
            appendStringArg(jsName);
        }
        return this;
    }

    @Override
    public void emitMethod(MethodDescriptor method) {
        if (!debug) {
            return;
        }
        flush();
        commands.add(EMIT_METHOD);
        if (method == null) {
            intArgs.add(-1);
        } else {
            appendMethodDescriptorArg(method);
        }
    }

    @Override
    public void emitClass(String className) {
        if (!debug) {
            return;
        }
        flush();
        commands.add(EMIT_CLASS);
        if (className == null) {
            intArgs.add(-1);
        } else {
            appendStringArg(className);
        }
    }

    @Override
    public void markClassStart(String className) {
        flush();
        commands.add(MARK_CLASS_START);
        appendStringArg(className);
    }

    @Override
    public void markClassEnd() {
        flush();
        commands.add(MARK_CLASS_END);
    }

    @Override
    public void markSectionStart(int id) {
        flush();
        commands.add(MARK_SECTION_START);
        intArgs.add(id);
    }

    @Override
    public void markSectionEnd() {
        flush();
        commands.add(MARK_SECTION_END);
    }

    public void flush() {
        if (lastWrittenChar == sb.length()) {
            return;
        }
        for (var i = lastWrittenChar; i < sb.length(); i += 128) {
            var j = Math.min(sb.length(), i + 128);
            var n = (j - i) - 1;
            commands.add((byte) (128 | n));
        }
        lastWrittenChar = sb.length();
    }

    public RememberedSource save() {
        flush();
        return new RememberedSource(commands.toArray(), sb.toString(), intArgs.toArray(),
                !strings.isEmpty() ? strings.toArray(new String[0]) : null,
                !fields.isEmpty() ? fields.toArray(new FieldReference[0]) : null,
                !methodDescriptors.isEmpty() ? methodDescriptors.toArray(new MethodDescriptor[0]) : null,
                !methods.isEmpty() ? methods.toArray(new MethodReference[0]) : null);
    }

    private void appendStringArg(String arg) {
        var index = stringIndexes.getOrDefault(arg, -1);
        if (index < 0) {
            index = strings.size();
            stringIndexes.put(arg, index);
            strings.add(arg);
        }
        intArgs.add(index);
    }

    private void appendFieldArg(FieldReference arg) {
        var index = fieldIndexes.getOrDefault(arg, -1);
        if (index < 0) {
            index = fields.size();
            fieldIndexes.put(arg, index);
            fields.add(arg);
        }
        intArgs.add(index);
    }

    private void appendMethodDescriptorArg(MethodDescriptor arg) {
        var index = methodDescriptorIndexes.getOrDefault(arg, -1);
        if (index < 0) {
            index = methodDescriptors.size();
            methodDescriptorIndexes.put(arg, index);
            methodDescriptors.add(arg);
        }
        intArgs.add(index);
    }

    private void appendMethodArg(MethodReference arg) {
        var index = methodIndexes.getOrDefault(arg, -1);
        if (index < 0) {
            index = methods.size();
            methodIndexes.put(arg, index);
            methods.add(arg);
        }
        intArgs.add(index);
    }
}
