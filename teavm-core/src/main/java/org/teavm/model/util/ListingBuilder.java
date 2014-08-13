/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.List;
import java.util.Objects;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class ListingBuilder {
    public String buildListing(ProgramReader program, String prefix) {
        StringBuilder sb = new StringBuilder();
        StringBuilder insnSb = new StringBuilder();
        InstructionStringifier stringifier = new InstructionStringifier(insnSb);
        for (int i = 0; i < program.variableCount(); ++i) {
            sb.append(prefix).append("var @").append(i);
            VariableReader var = program.variableAt(i);
            if (!var.readDebugNames().isEmpty()) {
                sb.append(" as ");
                boolean first = true;
                for (String debugName : var.readDebugNames()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(debugName);
                }
            }
            sb.append('\n');
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            sb.append(prefix).append("$").append(i).append(":\n");
            for (PhiReader phi : block.readPhis()) {
                sb.append(prefix).append("    ");
                sb.append("@").append(phi.getReceiver().getIndex()).append(" := ");
                List<? extends IncomingReader> incomings = phi.readIncomings();
                for (int j = 0; j < incomings.size(); ++j) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    IncomingReader incoming = incomings.get(j);
                    sb.append("@").append(incoming.getValue().getIndex()).append(" from ")
                            .append("$").append(incoming.getSource().getIndex());
                }
                sb.append("\n");
            }
            InstructionLocation location = null;
            for (int j = 0; j < block.instructionCount(); ++j) {
                insnSb.setLength(0);
                block.readInstruction(j, stringifier);
                if (!Objects.equals(location, stringifier.getLocation())) {
                    location = stringifier.getLocation();
                    sb.append(prefix).append("  at ").append(location != null ? location.toString() :
                            "unknown location").append('\n');
                }
                sb.append(prefix).append("    ").append(insnSb).append("\n");
            }
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                sb.append(prefix).append("    catch ").append(tryCatch.getExceptionType()).append(" @")
                        .append(tryCatch.getExceptionVariable().getIndex())
                        .append(" -> $").append(tryCatch.getHandler().getIndex()).append("\n");
            }
        }
        return sb.toString();
    }
}
