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

public class BufferedCodeWriter extends CodeWriter {
    private List<Fragment> fragments = new ArrayList<>();
    private int currentIndent;
    private int lastIndent;
    private StringBuilder buffer = new StringBuilder();

    public BufferedCodeWriter() {
    }

    public void writeTo(PrintWriter writer) {
        WriterWithContext writerWithContext = new WriterWithContext(writer);
        for (Fragment fragment : fragments) {
            fragment.writeTo(writerWithContext);
        }
    }

    @Override
    public CodeWriter fragment() {
        flush();
        BufferedCodeWriter innerWriter = new BufferedCodeWriter();
        fragments.add(new InnerWriterFragment(innerWriter.fragments));
        return innerWriter;
    }

    @Override
    protected void newLine() {
        fragments.add(new SimpleFragment(true, lastIndent, buffer.toString()));
        buffer.setLength(0);
        lastIndent = currentIndent;
        currentIndent = 0;
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
    public void flush() {
        fragments.add(new SimpleFragment(false, lastIndent, buffer.toString()));
        lastIndent = currentIndent;
        currentIndent = 0;
        buffer.setLength(0);
    }

    static class WriterWithContext {
        PrintWriter writer;
        boolean isNewLine = true;
        int indentLevel;

        WriterWithContext(PrintWriter writer) {
            this.writer = writer;
        }

        void append(String text) {
            if (isNewLine) {
                for (int i = 0; i < indentLevel; ++i) {
                    writer.print("    ");
                }
                isNewLine = false;
            }
            writer.print(text);
        }

        void newLine() {
            writer.println();
            isNewLine = true;
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
