/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.newir.interpreter;

import java.io.PrintWriter;
import java.util.List;

public class InstructionPrinter {
    private boolean resultWritten;
    private boolean preparing;
    private StringBuilder sb = new StringBuilder();
    private Line[] lines;
    private Line line;

    InstructionPrinter() {
    }

    void write(PrintWriter writer, List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return;
        }

        int lineNumberSize = numberSize(instructions.size() - 1);
        int resultSize = 0;
        int opcodeSize = 0;

        lines = new Line[instructions.size()];
        for (int i = 0; i < lines.length; ++i) {
            lines[i] = new Line();
        }

        preparing = true;
        for (Instruction instruction : instructions) {
            instruction.print(this);
        }

        int labelCount = 0;
        for (int i = 0; i < lines.length; ++i) {
            if (lines[i].label >= 0) {
                lines[i].label = labelCount++;
            }
        }
        int labelSize = numberSize(labelCount - 1) + 1;

        preparing = false;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            resultWritten = false;
            line = lines[i];
            instruction.print(this);
            line.arguments = sb.toString();
            lines[i] = line;
            if (line.opcode == null) {
                line.opcode = "";
            }
            if (line.result == null) {
                line.result = "";
            }
            sb.setLength(0);
            resultSize = Math.max(resultSize, line.result.length());
            opcodeSize = Math.max(opcodeSize, line.opcode.length());
        }

        for (int i = 0; i < lines.length; ++i) {
            String number = String.valueOf(i);
            for (int j = number.length(); j < lineNumberSize; ++j) {
                writer.append(' ');
            }
            writer.append(number).append(":  ");

            Line line = lines[i];

            if (line.label < 0) {
                for (int j = 0; j < labelSize; ++j) {
                    writer.append(' ');
                }
                writer.append("   ");
            } else {
                String label = String.valueOf(line.label);
                for (int j = label.length() + 1; j < labelSize; ++j) {
                    writer.append(' ');
                }
                writer.append("L").append(label).append(":  ");
            }

            for (int j = line.result.length(); j < resultSize; ++j) {
                writer.append(' ');
            }
            writer.append(line.result).append(line.result.isEmpty() ? "   " : " = ");

            writer.append(line.opcode);
            for (int j = line.opcode.length(); j < opcodeSize; ++j) {
                writer.append(' ');
            }

            writer.append("  ").append(line.arguments);
            writer.println();
        }

        lines = null;
    }

    private int numberSize(int n) {
        int result = 0;
        while (n > 0) {
            n /= 10;
            result++;
        }
        return result;
    }

    public InstructionPrinter intVar(int slot) {
        if (!preparing) {
            sb.append("i").append(slot);
        }
        return this;
    }

    public InstructionPrinter longVar(int slot) {
        if (!preparing) {
            sb.append("l").append(slot);
        }
        return this;
    }

    public InstructionPrinter objVar(int slot) {
        if (!preparing) {
            sb.append("l").append(slot);
        }
        return this;
    }

    public InstructionPrinter floatVar(int slot) {
        if (!preparing) {
            sb.append("f").append(slot);
        }
        return this;
    }

    public InstructionPrinter doubleVar(int slot) {
        if (!preparing) {
            sb.append("d").append(slot);
        }
        return this;
    }

    public InstructionPrinter label(int label) {
        if (!preparing) {
            sb.append("L").append(lines[label].label);
        } else {
            lines[label].label = 0;
        }
        return this;
    }

    public InstructionPrinter opcode(String opcode) {
        if (!preparing) {
            flushResult();
            line.opcode = opcode;
        }
        return this;
    }

    public InstructionPrinter arg() {
        if (!preparing) {
            flushResult();
            if (sb.length() > 0) {
                sb.append(", ");
            }
        }
        return this;
    }

    public InstructionPrinter text(String string) {
        if (!preparing) {
            sb.append(string);
        }
        return this;
    }

    private void flushResult() {
        if (!resultWritten) {
            line.result = sb.toString();
            sb.setLength(0);
            resultWritten = true;
        }
    }

    static class Line {
        int label = -1;
        String result;
        String opcode;
        String arguments;
    }
}
