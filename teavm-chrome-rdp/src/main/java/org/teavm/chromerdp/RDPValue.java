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
    private String typeName;
    private ChromeRDPDebugger debugger;
    private String objectId;
    private Map<String, ? extends JavaScriptVariable> properties;

    public RDPValue(ChromeRDPDebugger debugger, String representation, String typeName, String objectId) {
        this.representation = representation;
        this.typeName = typeName;
        this.debugger = debugger;
        this.objectId = objectId;
        properties = objectId != null ? new RDPScope(debugger, objectId) :
                Collections.<String, RDPLocalVariable>emptyMap();
    }

    @Override
    public String getRepresentation() {
        return representation;
    }

    @Override
    public String getClassName() {
        if (objectId != null) {
            String className = debugger.getClassName(objectId);
            return className != null ? className : "object";
        } else {
            return typeName;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, JavaScriptVariable> getProperties() {
        return (Map<String, JavaScriptVariable>)properties;
    }
}
