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
