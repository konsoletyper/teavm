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
import java.util.stream.Collectors;
import org.teavm.model.*;

public class ListingBuilder {
    public String buildListing(ProgramReader program, String prefix) {
        StringBuilder sb = new StringBuilder();
        StringBuilder insnSb = new StringBuilder();
        InstructionStringifier stringifier = new InstructionStringifier(insnSb);
        for (int i = 0; i < program.variableCount(); ++i) {
            sb.append(prefix).append("var @").append(i);
            VariableReader var = program.variableAt(i);
            if (var != null && var.getDebugName() != null) {
                sb.append(" as ").append(var.getDebugName());
            }
            sb.append('\n');
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            sb.append(prefix).append("$").append(i).append(":\n");
            if (block == null) {
                continue;
            }

            if (block.getExceptionVariable() != null) {
                sb.append("    @").append(block.getExceptionVariable().getIndex()).append(" = exception\n");
            }

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

            TextLocation location = null;
            for (int j = 0; j < block.instructionCount(); ++j) {
                insnSb.setLength(0);
                block.readInstruction(j, stringifier);
                if (!Objects.equals(location, stringifier.getLocation())) {
                    location = stringifier.getLocation();
                    sb.append(prefix).append("  at ").append(location != null ? location.toString()
                            : "unknown location").append('\n');
                }
                sb.append(prefix).append("    ").append(insnSb).append("\n");
            }
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                sb.append(prefix).append("    catch ").append(tryCatch.getExceptionType())
                        .append(" -> $").append(tryCatch.getHandler().getIndex());
                sb.append("\n");
                for (TryCatchJointReader joint : tryCatch.readJoints()) {
                    sb.append("      @").append(joint.getReceiver().getIndex()).append(" := e-phi(");
                    sb.append(joint.readSourceVariables().stream().map(sourceVar -> "@" + sourceVar.getIndex())
                            .collect(Collectors.joining(", ")));
                    sb.append(")\n");
                }
            }
        }
        return sb.toString();
    }
}
