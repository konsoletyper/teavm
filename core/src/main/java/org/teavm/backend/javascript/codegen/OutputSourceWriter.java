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

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class OutputSourceWriter extends SourceWriter implements LocationProvider {
    private static final int LET_SEQUENCE_LIMIT = 50;
    private final Appendable innerWriter;
    private int indentSize;
    private final NamingStrategy naming;
    private boolean lineStart;
    private boolean minified;
    private final int lineWidth;
    private int column;
    private int line;
    private int offset;
    private DebugInformationEmitter debugInformationEmitter = new DummyDebugInformationEmitter();
    private String classMarkClass;
    private int classMarkPos;
    private ObjectIntMap<String> classSizes = new ObjectIntHashMap<>();
    private int sectionMarkSection = -1;
    private int sectionMarkPos;
    private IntIntMap sectionSizes = new IntIntHashMap();
    private DeclarationType currentDeclarationType;
    private boolean expectingDeclarationName;
    private int letSequenceSize;

    OutputSourceWriter(NamingStrategy naming, Appendable innerWriter, int lineWidth) {
        this.naming = naming;
        this.innerWriter = innerWriter;
        this.lineWidth = lineWidth;
    }

    public void setDebugInformationEmitter(DebugInformationEmitter debugInformationEmitter) {
        this.debugInformationEmitter = debugInformationEmitter;
        debugInformationEmitter.setLocationProvider(this);
    }

    void setMinified(boolean minified) {
        this.minified = minified;
    }

    public void finish() {
        finishLet();
    }

    private void finishLetImplicitly() {
        if (currentDeclarationType == null) {
            finishLet();
        }
    }

    private void finishLet() {
        if (letSequenceSize > 0) {
            letSequenceSize = 0;
            try {
                innerWriter.append(";\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public SourceWriter append(char value) {
        appendIndent();
        finishLetImplicitly();
        try {
            innerWriter.append(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (value == '\n') {
            newLine();
        } else {
            column++;
            offset++;
        }
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) {
        int last = start;
        for (int i = start; i < end; ++i) {
            if (csq.charAt(i) == '\n') {
                appendSingleLine(csq, last, i);
                newLine();
                last = i + 1;
            }
        }
        appendSingleLine(csq, last, end);
        return this;
    }

    private void appendSingleLine(CharSequence csq, int start, int end) {
        if (start == end) {
            return;
        }
        finishLetImplicitly();
        appendIndent();
        column += end - start;
        offset += end - start;
        try {
            innerWriter.append(csq, start, end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SourceWriter appendClass(String cls) {
        return appendDeclaration(naming.className(cls));
    }

    @Override
    public SourceWriter appendField(FieldReference field) {
        return append(naming.instanceFieldName(field));
    }

    @Override
    public SourceWriter appendStaticField(FieldReference field) {
        return appendDeclaration(naming.fieldName(field));
    }

    @Override
    public SourceWriter appendVirtualMethod(MethodDescriptor method) {
        return append(naming.instanceMethodName(method));
    }

    @Override
    public SourceWriter appendMethod(MethodReference method) {
        return appendDeclaration(naming.methodName(method));
    }

    @Override
    public SourceWriter appendFunction(String name) {
        return appendDeclaration(naming.functionName(name));
    }

    @Override
    public SourceWriter appendGlobal(String name) {
        return append(name);
    }

    @Override
    public SourceWriter appendInit(MethodReference method) {
        return appendDeclaration(naming.initializerName(method));
    }

    @Override
    public SourceWriter appendClassInit(String className) {
        return appendDeclaration(naming.classInitializerName(className));
    }

    @Override
    public SourceWriter startVariableDeclaration() {
        checkStartDeclaration();
        currentDeclarationType = DeclarationType.VARIABLE;
        expectingDeclarationName = true;
        return this;
    }

    @Override
    public SourceWriter startFunctionDeclaration() {
        checkStartDeclaration();
        currentDeclarationType = DeclarationType.FUNCTION;
        expectingDeclarationName = true;
        return this;
    }

    @Override
    public SourceWriter declareVariable() {
        checkStartDeclaration();
        currentDeclarationType = DeclarationType.VARIABLE_WITHOUT_VALUE;
        expectingDeclarationName = true;
        return this;
    }

    private void checkStartDeclaration() {
        if (currentDeclarationType != null) {
            throw new IllegalStateException();
        }
    }

    @Override
    public SourceWriter endDeclaration() {
        if (currentDeclarationType == null || expectingDeclarationName) {
            throw new IllegalStateException();
        }
        switch (currentDeclarationType) {
            case FUNCTION:
                newLine();
                break;
            case VARIABLE:
                if (letSequenceSize == 0) {
                    append(';').softNewLine();
                } else if (letSequenceSize >= LET_SEQUENCE_LIMIT) {
                    finishLet();
                }
                break;
            case VARIABLE_WITHOUT_VALUE:
                throw new IllegalStateException();
        }
        currentDeclarationType = null;
        return this;
    }

    private SourceWriter appendDeclaration(ScopedName name) {
        if (!expectingDeclarationName) {
            return appendName(name);
        }
        expectingDeclarationName = false;
        switch (currentDeclarationType) {
            case FUNCTION:
                finishLet();
                if (name.scoped) {
                    append(naming.additionalScopeName()).append('.').append(name.name).ws()
                            .append('=').ws().append("function");
                } else {
                    append("function ").append(name.name);
                }
                break;
            case VARIABLE:
                if (name.scoped) {
                    finishLet();
                    append(naming.additionalScopeName()).append('.');
                } else {
                    if (letSequenceSize++ == 0) {
                        append("let ");
                    } else {
                        append(',').softNewLine();
                    }
                }
                append(name.name).ws().append('=').ws();
                break;
            case VARIABLE_WITHOUT_VALUE:
                if (!name.scoped) {
                    if (letSequenceSize++ == 0) {
                        append("let ");
                    } else {
                        append(',').softNewLine();
                    }
                    append(name.name);
                }
                expectingDeclarationName = false;
                currentDeclarationType = null;
                break;
        }
        return this;
    }

    private SourceWriter appendName(ScopedName name) {
        if (name.scoped) {
            append(naming.additionalScopeName());
            append('.');
        }
        append(name.name);
        return this;
    }

    private void appendIndent() {
        if (minified) {
            return;
        }
        finishLetImplicitly();
        if (lineStart) {
            try {
                for (int i = 0; i < indentSize; ++i) {
                    innerWriter.append("    ");
                    column += 4;
                    offset += 4;
                }
                lineStart = false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public SourceWriter newLine() {
        finishLetImplicitly();
        try {
            innerWriter.append('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        column = 0;
        ++line;
        ++offset;
        lineStart = true;
        return this;
    }

    @Override
    public SourceWriter ws() {
        if (column >= lineWidth) {
            newLine();
        } else {
            if (!minified) {
                finishLetImplicitly();
                try {
                    innerWriter.append(' ');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                column++;
                offset++;
            }
        }
        return this;
    }

    @Override
    public SourceWriter sameLineWs() {
        if (!minified) {
            finishLetImplicitly();
            try {
                innerWriter.append(' ');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            column++;
            offset++;
        }
        return this;
    }

    @Override
    public SourceWriter tokenBoundary() {
        if (column >= lineWidth) {
            newLine();
        }
        return this;
    }

    @Override
    public SourceWriter softNewLine() {
        if (!minified) {
            finishLetImplicitly();
            try {
                innerWriter.append('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            column = 0;
            ++offset;
            ++line;
            lineStart = true;
        }
        return this;
    }

    @Override
    public SourceWriter indent() {
        ++indentSize;
        return this;
    }

    @Override
    public SourceWriter outdent() {
        --indentSize;
        return this;
    }

    @Override
    public SourceWriter emitLocation(String fileName, int line) {
        debugInformationEmitter.emitLocation(fileName, line);
        return this;
    }

    @Override
    public SourceWriter enterLocation() {
        debugInformationEmitter.enterLocation();
        return this;
    }

    @Override
    public SourceWriter exitLocation() {
        debugInformationEmitter.exitLocation();
        return this;
    }

    @Override
    public SourceWriter emitStatementStart() {
        debugInformationEmitter.emitStatementStart();
        return this;
    }

    @Override
    public SourceWriter emitVariables(String[] names, String jsName) {
        debugInformationEmitter.emitVariable(names, jsName);
        return this;
    }

    @Override
    public void emitMethod(MethodDescriptor method) {
        debugInformationEmitter.emitMethod(method);
    }

    @Override
    public void emitClass(String className) {
        debugInformationEmitter.emitClass(className);
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void markClassStart(String className) {
        classMarkClass = className;
        classMarkPos = offset;
    }

    @Override
    public void markClassEnd() {
        if (classMarkClass != null) {
            var size = offset - classMarkPos;
            if (size > 0) {
                var currentSize = classSizes.get(classMarkClass);
                classSizes.put(classMarkClass, currentSize + size);
            }
            classMarkClass = null;
        }
    }

    @Override
    public void markSectionStart(int id) {
        sectionMarkSection = id;
        sectionMarkPos = offset;
    }

    @Override
    public void markSectionEnd() {
        if (sectionMarkSection >= 0) {
            int size = offset - sectionMarkPos;
            if (size > 0) {
                var currentSize = sectionSizes.get(sectionMarkSection);
                sectionSizes.put(sectionMarkSection, currentSize + size);
            }
            sectionMarkSection = -1;
        }
    }

    public Collection<String> getClassesInStats() {
        var result = new ArrayList<String>();
        for (var cursor : classSizes.keys()) {
            result.add(cursor.value);
        }
        return result;
    }

    public int getClassSize(String className) {
        return classSizes.get(className);
    }

    public int getSectionSize(int sectionId) {
        return sectionSizes.get(sectionId);
    }

    private enum DeclarationType {
        FUNCTION,
        VARIABLE,
        VARIABLE_WITHOUT_VALUE
    }
}
