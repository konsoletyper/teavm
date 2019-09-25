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
package org.teavm.chromerdp.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.teavm.chromerdp.data.LocationDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetBreakpointResponse {
    private String breakpointId;
    private LocationDTO actualLocation;

    public String getBreakpointId() {
        return breakpointId;
    }

    public void setBreakpointId(String breakpointId) {
        this.breakpointId = breakpointId;
    }

    public LocationDTO getActualLocation() {
        return actualLocation;
    }

    public void setActualLocation(LocationDTO actualLocation) {
        this.actualLocation = actualLocation;
    }
}
