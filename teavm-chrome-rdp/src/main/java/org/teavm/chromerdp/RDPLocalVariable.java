package org.teavm.chromerdp;

import org.teavm.debugging.JavaScriptValue;
import org.teavm.debugging.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class RDPLocalVariable implements JavaScriptVariable {
    private String name;
    private RDPValue value;

    public RDPLocalVariable(String name, RDPValue value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JavaScriptValue getValue() {
        return value;
    }
}
