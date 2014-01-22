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
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class ListingBuilder {
    public String buildListing(Program program, String prefix) {
        StringBuilder sb = new StringBuilder();
        InstructionStringifier stringifier = new InstructionStringifier(sb);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            sb.append(prefix).append("$").append(i).append(":\n");
            for (Phi phi : block.getPhis()) {
                sb.append(prefix).append("    ");
                sb.append("@").append(phi.getReceiver().getIndex()).append(" := ");
                List<Incoming> incomings = phi.getIncomings();
                for (int j = 0; j < incomings.size(); ++j) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    Incoming incoming = incomings.get(j);
                    sb.append("@").append(incoming.getValue().getIndex()).append(" from ")
                            .append("$").append(incoming.getSource().getIndex());
                }
                sb.append("\n");
            }
            for (Instruction insn : block.getInstructions()) {
                sb.append(prefix).append("    ");
                insn.acceptVisitor(stringifier);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
