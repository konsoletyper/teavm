package org.teavm.parsing;

import org.teavm.model.Instruction;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface VariableDebugInformation {
    String getDefinitionDebugName(Instruction insn);

    String getParameterDebugName(int index);
}
