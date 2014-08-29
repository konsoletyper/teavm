package org.teavm.chromerdp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class RDPValue implements JavaScriptValue {
    private AtomicReference<String> representation = new AtomicReference<>();
    private AtomicReference<String> className = new AtomicReference<>();
    private String typeName;
    private ChromeRDPDebugger debugger;
    private String objectId;
    private Map<String, ? extends JavaScriptVariable> properties;

    public RDPValue(ChromeRDPDebugger debugger, String representation, String typeName, String objectId) {
        this.representation.set(representation == null && objectId == null ? "" : representation);
        this.typeName = typeName;
        this.debugger = debugger;
        this.objectId = objectId;
        properties = objectId != null ? new RDPScope(debugger, objectId) :
                Collections.<String, RDPLocalVariable>emptyMap();
    }

    @Override
    public String getRepresentation() {
        if (representation.get() == null) {
            representation.compareAndSet(null, debugger.getRepresentation(objectId));
        }
        return representation.get();
    }

    @Override
    public String getClassName() {
        if (className.get() == null) {
            if (objectId != null) {
                String computedClassName = debugger.getClassName(objectId);
                className.compareAndSet(null, computedClassName != null ? computedClassName : "@Object");
            } else {
                className.compareAndSet(null, "@" + typeName);
            }
        }
        return className.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, JavaScriptVariable> getProperties() {
        return (Map<String, JavaScriptVariable>)properties;
    }
}
