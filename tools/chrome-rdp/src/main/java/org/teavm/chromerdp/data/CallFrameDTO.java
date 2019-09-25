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

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFrameDTO {
    private String callFrameId;
    private LocationDTO location;
    private ScopeDTO[] scopeChain;

    public String getCallFrameId() {
        return callFrameId;
    }

    public void setCallFrameId(String callFrameId) {
        this.callFrameId = callFrameId;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }

    public ScopeDTO[] getScopeChain() {
        return scopeChain;
    }

    public void setScopeChain(ScopeDTO[] scopeChain) {
        this.scopeChain = scopeChain;
    }
}
