package org.teavm.chromerdp;

import java.util.Collections;
import java.util.Map;
import org.teavm.debugging.JavaScriptValue;
import org.teavm.debugging.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class RDPValue implements JavaScriptValue {
    private String representation;

    public RDPValue(String representation) {
        this.representation = representation;
    }

    @Override
    public String getRepresentation() {
        return representation;
    }

    @Override
    public Map<String, JavaScriptVariable> getProperties() {
        return Collections.emptyMap();
    }
}
