/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.util.json;

import java.util.HashMap;
import java.util.Map;

public class JsonPropertyVisitor extends JsonAllErrorVisitor {
    private Map<String, JsonVisitor> properties = new HashMap<>();
    private boolean skipNonExistentProperties;

    public JsonPropertyVisitor(boolean skipNonExistentProperties) {
        this.skipNonExistentProperties = skipNonExistentProperties;
    }

    public void addProperty(String propertyName, JsonVisitor visitor) {
        properties.put(propertyName, visitor);
    }

    @Override
    public JsonVisitor property(JsonErrorReporter reporter, String name) {
        if (!skipNonExistentProperties && !properties.containsKey(name)) {
            reporter.error("Unexpected property name: " + name);
        }
        return properties.get(name);
    }
}
