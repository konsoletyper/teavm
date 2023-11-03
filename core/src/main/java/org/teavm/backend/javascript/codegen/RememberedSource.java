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

import org.teavm.backend.javascript.templating.SourceFragment;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class RememberedSource implements SourceFragment {
    public static final int FILTER_TEXT = 1;
    public static final int FILTER_REF = 2;
    public static final int FILTER_DEBUG = 4;
    public static final int FILTER_STATS = 8;
    public static final int FILTER_ALL = FILTER_TEXT | FILTER_REF | FILTER_DEBUG | FILTER_STATS;

    private byte[] commands;
    private String chars;
    private int[] intArgs;
    private String[] strings;
    private FieldReference[] fields;
    private MethodDescriptor[] methodDescriptors;
    private MethodReference[] methods;

    RememberedSource(byte[] commands, String chars, int[] intArgs, String[] strings, FieldReference[] fields,
            MethodDescriptor[] methodDescriptors, MethodReference[] methods) {
        this.commands = commands;
        this.chars = chars;
        this.intArgs = intArgs;
        this.strings = strings;
        this.fields = fields;
        this.methodDescriptors = methodDescriptors;
        this.methods = methods;
    }

    public void replay(SourceWriterSink sink, int filter) {
        var commandIndex = 0;
        var charIndex = 0;
        var intArgIndex = 0;

        var commands = this.commands;
        var intArgs = this.intArgs;
        var chars = this.chars;
        while (commandIndex < commands.length) {
            var command = commands[commandIndex++];
            if ((command & 128) != 0) {
                var count = 1 + (command & 127);
                if ((filter & FILTER_TEXT) != 0) {
                    sink.append(chars, charIndex, charIndex + count);
                }
                charIndex += count;
                continue;
            }
            switch (command) {
                case RememberingSourceWriter.CLASS:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendClass(strings[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.FIELD:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendField(fields[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.STATIC_FIELD:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendStaticField(fields[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.METHOD:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendMethod(methodDescriptors[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.METHOD_BODY:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendMethodBody(methods[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.FUNCTION:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendFunction(strings[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.GLOBAL:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendGlobal(strings[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.INIT:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendInit(methods[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.CLASS_INIT:
                    if ((filter & FILTER_REF) != 0) {
                        sink.appendClassInit(strings[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.NEW_LINE:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.newLine();
                    }
                    break;

                case RememberingSourceWriter.WS:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.ws();
                    }
                    break;

                case RememberingSourceWriter.SAME_LINE_WS:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.sameLineWs();
                    }
                    break;

                case RememberingSourceWriter.TOKEN_BOUNDARY:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.tokenBoundary();
                    }
                    break;

                case RememberingSourceWriter.SOFT_NEW_LINE:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.softNewLine();
                    }
                    break;

                case RememberingSourceWriter.INDENT:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.indent();
                    }
                    break;

                case RememberingSourceWriter.OUTDENT:
                    if ((filter & FILTER_TEXT) != 0) {
                        sink.outdent();
                    }
                    break;

                case RememberingSourceWriter.EMIT_LOCATION:
                    if ((filter & FILTER_DEBUG) != 0) {
                        var fileIndex = intArgs[intArgIndex];
                        var file = fileIndex >= 0 ? strings[fileIndex] : null;
                        sink.emitLocation(file, intArgs[intArgIndex + 1]);
                    }
                    intArgIndex += 2;
                    break;

                case RememberingSourceWriter.ENTER_LOCATION:
                    if ((filter & FILTER_DEBUG) != 0) {
                        sink.enterLocation();
                    }
                    break;

                case RememberingSourceWriter.EXIT_LOCATION:
                    if ((filter & FILTER_DEBUG) != 0) {
                        sink.exitLocation();
                    }
                    break;

                case RememberingSourceWriter.EMIT_STATEMENT_START:
                    if ((filter & FILTER_DEBUG) != 0) {
                        sink.emitStatementStart();
                    }
                    break;

                case RememberingSourceWriter.EMIT_VARIABLES:
                    var count = intArgs[intArgIndex++];
                    if ((filter & FILTER_DEBUG) != 0) {
                        var names = new String[count];
                        for (var i = 0; i < count; ++i) {
                            names[i] = strings[intArgs[intArgIndex++]];
                        }
                        var jsName = strings[intArgs[intArgIndex++]];
                        sink.emitVariables(names, jsName);
                    } else {
                        intArgIndex += count + 1;
                    }
                    break;

                case RememberingSourceWriter.EMIT_CLASS:
                    if ((filter & FILTER_DEBUG) != 0) {
                        var classIndex = intArgs[intArgIndex];
                        sink.emitClass(classIndex >= 0 ? strings[classIndex] : null);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.EMIT_METHOD:
                    if ((filter & FILTER_DEBUG) != 0) {
                        var methodIndex = intArgs[intArgIndex];
                        sink.emitMethod(methodIndex >= 0 ? methodDescriptors[methodIndex] : null);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.MARK_CLASS_START:
                    if ((filter & FILTER_STATS) != 0) {
                        sink.markClassStart(strings[intArgs[intArgIndex]]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.MARK_CLASS_END:
                    if ((filter & FILTER_STATS) != 0) {
                        sink.markClassEnd();
                    }
                    break;

                case RememberingSourceWriter.MARK_SECTION_START:
                    if ((filter & FILTER_STATS) != 0) {
                        sink.markSectionStart(intArgs[intArgIndex]);
                    }
                    intArgIndex++;
                    break;

                case RememberingSourceWriter.MARK_SECTION_END:
                    if ((filter & FILTER_STATS) != 0) {
                        sink.markSectionEnd();
                    }
                    break;
            }
        }
    }

    @Override
    public void write(SourceWriter writer, int precedence) {
        replay(writer, FILTER_ALL);
    }
}
