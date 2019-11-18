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
package org.teavm.backend.c.generate;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BufferedCodeWriter extends CodeWriter {
    private List<Fragment> fragments = new ArrayList<>();
    private int currentIndent;
    private int lastIndent;
    private StringBuilder buffer = new StringBuilder();
    private boolean lineNumbersEmitted;
    private String lastFileName;
    private int lastLineNumber;
    private boolean locationDirty;

    public BufferedCodeWriter(boolean lineNumbersEmitted) {
        this.lineNumbersEmitted = lineNumbersEmitted;
    }

    public void writeTo(PrintWriter writer, String fileName) {
        WriterWithContext writerWithContext = new WriterWithContext(writer, fileName);
        for (Fragment fragment : fragments) {
            fragment.writeTo(writerWithContext);
        }
    }

    @Override
    public CodeWriter fragment() {
        flush();
        BufferedCodeWriter innerWriter = new BufferedCodeWriter(lineNumbersEmitted);
        innerWriter.lastFileName = lastFileName;
        innerWriter.lastLineNumber = lastLineNumber;
        innerWriter.locationDirty = locationDirty;
        fragments.add(new InnerWriterFragment(innerWriter.fragments));
        locationDirty = true;
        return innerWriter;
    }

    @Override
    protected void newLine() {
        fragments.add(new SimpleFragment(true, lastIndent, buffer.toString()));
        buffer.setLength(0);
        lastIndent = currentIndent;
        currentIndent = 0;
        if (lineNumbersEmitted) {
            lastLineNumber++;
        }
    }

    @Override
    protected void append(String text) {
        buffer.append(text);
    }

    @Override
    protected void indentBy(int amount) {
        if (buffer.length() == 0) {
            lastIndent += amount;
        } else {
            currentIndent += amount;
        }
    }

    @Override
    public void source(String fileName, int lineNumber) {
        if (!lineNumbersEmitted) {
            return;
        }

        if (!Objects.equals(lastFileName, fileName) || lastLineNumber != lineNumber || locationDirty) {
            flush();
            fragments.add(new SourceFragment(fileName, lineNumber));
            lastFileName = fileName;
            lastLineNumber = lineNumber;
            locationDirty = false;
        }
    }

    @Override
    public void nosource() {
        source(null, 0);
    }

    @Override
    public void flush() {
        if (buffer.length() > 0 || lastIndent != 0 || currentIndent != 0) {
            fragments.add(new SimpleFragment(false, lastIndent, buffer.toString()));
            lastIndent = currentIndent;
            currentIndent = 0;
            buffer.setLength(0);
        }
    }

    static class WriterWithContext {
        PrintWriter writer;
        boolean isNewLine = true;
        int indentLevel;
        String initialFileName;
        String fileName;
        int lineNumber;
        int absLineNumber = 1;
        String pendingFileName;
        int pendingLineNumber = -1;

        WriterWithContext(PrintWriter writer, String fileName) {
            this.writer = writer;
            this.fileName = fileName;
            initialFileName = fileName;
            lineNumber = 1;
        }

        void append(String text) {
            if (text.isEmpty()) {
                return;
            }
            if (isNewLine) {
                if (pendingFileName != null && pendingLineNumber >= 0) {
                    printLineDirective(pendingFileName, pendingLineNumber);
                    pendingLineNumber = -1;
                    pendingFileName = null;
                }
                printIndent();
                isNewLine = false;
            }
            writer.print(text);
        }

        private void printLineDirective(String fileName, int lineNumber) {
            if (Objects.equals(this.fileName, fileName) && lineNumber == this.lineNumber) {
                return;
            }

            printIndent();
            writer.print("#line ");
            if (Objects.equals(fileName, initialFileName)) {
                lineNumber = absLineNumber + 1;
            }
            writer.print(lineNumber);
            if (!Objects.equals(fileName, this.fileName)) {
                writer.print(" \"");
                escape(writer, fileName);
                writer.print("\"");
            }
            writer.println();
            absLineNumber++;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }

        void newLine() {
            lineNumber++;
            absLineNumber++;
            writer.println();
            isNewLine = true;
        }

        private void printIndent() {
            for (int i = 0; i < indentLevel; ++i) {
                writer.print("    ");
            }
        }

        void source(String fileName, int lineNumber) {
            if (fileName == null) {
                fileName = initialFileName;
                lineNumber = absLineNumber;
            }
            if (isNewLine) {
                pendingFileName = fileName;
                pendingLineNumber = lineNumber;
            } else if (!Objects.equals(this.fileName, fileName) || this.lineNumber != lineNumber) {
                this.lineNumber++;
                absLineNumber++;
                writer.println();
                printLineDirective(fileName, lineNumber);
                isNewLine = true;
            }
        }
    }

    static abstract class Fragment {
        abstract void writeTo(WriterWithContext writer);
    }

    static class SimpleFragment extends Fragment {
        boolean newLine;
        int indentLevel;
        String text;

        SimpleFragment(boolean newLine, int indentLevel, String text) {
            this.newLine = newLine;
            this.indentLevel = indentLevel;
            this.text = text;
        }

        @Override
        void writeTo(WriterWithContext writer) {
            writer.indentLevel += indentLevel;
            writer.append(text);
            if (newLine) {
                writer.newLine();
            }
        }
    }

    static class SourceFragment extends Fragment {
        private String fileName;
        private int lineNumber;

        SourceFragment(String fileName, int lineNumber) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }

        @Override
        void writeTo(WriterWithContext writer) {
            writer.source(fileName, lineNumber);
        }
    }

    private static void escape(PrintWriter writer, String string) {
        int chunkSize = 256;
        for (int i = 0; i < string.length(); i += chunkSize) {
            int last = Math.min(i + chunkSize, string.length());

            for (int j = i; j < last; ++j) {
                char c = string.charAt(j);
                switch (c) {
                    case '\\':
                        writer.print("\\\\");
                        break;
                    case '"':
                        writer.print("\\\"");
                        break;
                    case '\r':
                        writer.print("\\r");
                        break;
                    case '\n':
                        writer.print("\\n");
                        break;
                    case '\t':
                        writer.print("\\t");
                        break;
                    default:
                        if (c < 32) {
                            writer.print("\\0" + Character.forDigit(c >> 3, 8) + Character.forDigit(c & 0x7, 8));
                        } else if (c > 127) {
                            writer.print("\\u"
                                    + Character.forDigit(c >> 12, 16)
                                    + Character.forDigit((c >> 8) & 15, 16)
                                    + Character.forDigit((c >> 4) & 15, 16)
                                    + Character.forDigit(c & 15, 16));
                        } else {
                            writer.print(c);
                        }
                        break;
                }
            }
        }
    }

    static class InnerWriterFragment extends Fragment {
        List<Fragment> fragments;

        InnerWriterFragment(List<Fragment> fragments) {
            this.fragments = fragments;
        }

        @Override
        void writeTo(WriterWithContext writer) {
            for (Fragment fragment : fragments) {
                fragment.writeTo(writer);
            }
        }
    }
}
