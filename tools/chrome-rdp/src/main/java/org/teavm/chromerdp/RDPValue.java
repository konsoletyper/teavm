/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev
 */
public class RDPValue implements JavaScriptValue {
    private AtomicReference<String> representation = new AtomicReference<>();
    private AtomicReference<String> className = new AtomicReference<>();
    private String typeName;
    private ChromeRDPDebugger debugger;
    private String objectId;
    private Map<String, ? extends JavaScriptVariable> properties;
    private boolean innerStructure;

    public RDPValue(ChromeRDPDebugger debugger, String representation, String typeName, String objectId,
            boolean innerStructure) {
        this.representation.set(representation == null && objectId == null ? "" : representation);
        this.typeName = typeName;
        this.debugger = debugger;
        this.objectId = objectId;
        this.innerStructure = innerStructure;
        properties = objectId != null ? new RDPScope(debugger, objectId)
                : Collections.<String, RDPLocalVariable>emptyMap();
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
        return (Map<String, JavaScriptVariable>) properties;
    }

    @Override
    public boolean hasInnerStructure() {
        return innerStructure;
    }

    @Override
    public String getInstanceId() {
        return objectId;
    }
}
