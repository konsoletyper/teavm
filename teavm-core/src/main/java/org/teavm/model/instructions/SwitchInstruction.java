package org.teavm.model.instructions;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class SwitchInstruction extends Instruction {
    private Variable condition;
    private List<SwitchTableEntry> entries = new ArrayList<>();
    private BasicBlock defaultTarget;

    public Variable getCondition() {
        return condition;
    }

    public void setCondition(Variable condition) {
        this.condition = condition;
    }

    private List<SwitchTableEntry> safeEntries = new AbstractList<SwitchTableEntry>() {
        @Override
        public SwitchTableEntry set(int index, SwitchTableEntry element) {
            SwitchTableEntry oldElement = entries.get(index);
            oldElement.setInstruction(null);
            entries.set(index, element);
            element.setInstruction(SwitchInstruction.this);
            return oldElement;
        }

        @Override
        public void add(int index, SwitchTableEntry element) {
            entries.add(index, element);
            element.setInstruction(SwitchInstruction.this);
        }

        @Override
        public SwitchTableEntry remove(int index) {
            SwitchTableEntry element = entries.remove(index);
            element.setInstruction(null);
            return element;
        }

        @Override
        public void clear() {
            for (SwitchTableEntry element : entries) {
                element.setInstruction(null);
            }
            entries.clear();
        }

        @Override
        public SwitchTableEntry get(int index) {
            return entries.get(index);
        }

        @Override
        public int size() {
            return entries.size();
        }
    };

    public List<SwitchTableEntry> getEntries() {
        return safeEntries;
    }

    public BasicBlock getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(BasicBlock defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
