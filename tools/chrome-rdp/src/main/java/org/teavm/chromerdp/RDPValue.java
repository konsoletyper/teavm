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

import java.util.HashMap;
import java.util.Map;
import org.teavm.chromerdp.data.RemoteObjectDTO;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

class RDPValue implements JavaScriptValue {
    private ChromeRDPDebugger debugger;
    private String objectId;
    private Promise<Map<String, ? extends JavaScriptVariable>> properties;
    private boolean innerStructure;
    private Promise<String> className;
    private Promise<String> representation;
    private final String defaultRepresentation;
    private final String typeName;
    RemoteObjectDTO getter;

    RDPValue(ChromeRDPDebugger debugger, String representation, String typeName, String objectId,
            boolean innerStructure) {
        this.debugger = debugger;
        this.objectId = objectId;
        this.innerStructure = innerStructure;
        this.typeName = typeName;
        defaultRepresentation = representation;
    }

    @Override
    public Promise<String> getRepresentation() {
        if (representation == null) {
            if (objectId != null) {
                representation = defaultRepresentation != null
                        ? Promise.of(defaultRepresentation)
                        : debugger.getRepresentation(objectId);
            } else {
                representation = Promise.of(defaultRepresentation != null ? defaultRepresentation : "");
            }
        }
        return representation;
    }

    @Override
    public Promise<String> getClassName() {
        if (className == null) {
            if (objectId == null) {
                className = Promise.of("@" + typeName);
            } else {
                className = debugger.getClassName(objectId).then(c -> c != null ? c : "@Object");
            }
        }
        return className;
    }

    @Override
    public Promise<Map<String, ? extends JavaScriptVariable>> getProperties() {
        if (properties == null) {
            if (getter == null) {
                properties = debugger.createScope(objectId);
            } else {
                properties = debugger.invokeGetter(getter.getObjectId(), objectId).then(value -> {
                    if (value == null) {
                        value = new RDPValue(debugger, "null", "null", null, false);
                    }
                    Map<String, RDPLocalVariable> map = new HashMap<>();
                    map.put("<value>", new RDPLocalVariable("<value>", value));
                    map.put("<function>", new RDPLocalVariable("<function>", debugger.mapValue(getter)));
                    return map;
                });
            }
        }
        return properties;
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
