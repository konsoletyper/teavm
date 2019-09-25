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
package org.teavm.chromerdp.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyDescriptorDTO {
    private String name;
    private RemoteObjectDTO value;
    private RemoteObjectDTO getter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RemoteObjectDTO getValue() {
        return value;
    }

    public void setValue(RemoteObjectDTO value) {
        this.value = value;
    }

    @JsonProperty("get")
    public RemoteObjectDTO getGetter() {
        return getter;
    }

    @JsonProperty("get")
    public void setGetter(RemoteObjectDTO getter) {
        this.getter = getter;
    }
}
