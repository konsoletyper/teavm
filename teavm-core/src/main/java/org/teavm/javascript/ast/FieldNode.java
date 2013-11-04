package org.teavm.javascript.ast;

import java.util.EnumSet;
import java.util.Set;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class FieldNode {
    private String name;
    private ValueType type;
    private Set<NodeModifier> modifiers = EnumSet.noneOf(NodeModifier.class);
    private Object initialValue;

    public FieldNode(String name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Set<NodeModifier> getModifiers() {
        return modifiers;
    }

    public ValueType getType() {
        return type;
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
    }
}
