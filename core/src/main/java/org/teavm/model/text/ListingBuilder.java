/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.text;

import java.util.List;
import java.util.Objects;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.IncomingReader;
import org.teavm.model.InstructionIterator;
import org.teavm.model.PhiReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.VariableReader;

public class ListingBuilder {
    public String buildListing(ProgramReader program, String prefix) {
        StringBuilder sb = new StringBuilder();
        StringBuilder insnSb = new StringBuilder();
        InstructionStringifier stringifier = new InstructionStringifier(insnSb, program);
        for (int i = 0; i < program.variableCount(); ++i) {
            VariableReader var = program.variableAt(i);
            if (var == null || var.getDebugName() == null) {
                continue;
            }
            sb.append(prefix).append("var @").append(stringifier.getVariableLabel(i));
            sb.append(" as ").append(var.getDebugName());
            sb.append('\n');
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            sb.append(prefix).append("$").append(i).append("\n");
            if (block == null) {
                continue;
            }

            if (block.getExceptionVariable() != null) {
                sb.append("    @").append(stringifier.getVariableLabel(block.getExceptionVariable().getIndex()))
                        .append(" := exception\n");
            }

            for (PhiReader phi : block.readPhis()) {
                sb.append(prefix).append("    ");
                sb.append("@").append(stringifier.getVariableLabel(phi.getReceiver().getIndex())).append(" := phi ");
                List<? extends IncomingReader> incomings = phi.readIncomings();
                for (int j = 0; j < incomings.size(); ++j) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    IncomingReader incoming = incomings.get(j);
                    sb.append("@").append(stringifier.getVariableLabel(incoming.getValue().getIndex()))
                            .append(" from ").append("$").append(incoming.getSource().getIndex());
                }
                sb.append("\n");
            }

            TextLocation location = null;
            for (InstructionIterator iterator = block.iterateInstructions(); iterator.hasNext();) {
                iterator.next();
                insnSb.setLength(0);
                iterator.read(stringifier);
                if (!Objects.equals(location, stringifier.getLocation())) {
                    location = stringifier.getLocation();
                    sb.append(prefix).append("  at ");
                    if (location == null) {
                        sb.append("unknown location");
                    } else {
                        sb.append("'");
                        InstructionStringifier.escapeStringLiteral(location.getFileName(), sb);
                        sb.append("' " + location.getLine());
                    }
                    sb.append('\n');
                }
                sb.append(prefix).append("    ").append(insnSb).append("\n");
            }
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                sb.append(prefix).append("    catch ");
                if (tryCatch.getExceptionType() != null) {
                    InstructionStringifier.escapeStringLiteral(tryCatch.getExceptionType(), sb);
                }
                sb.append(" goto $").append(tryCatch.getHandler().getIndex());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
