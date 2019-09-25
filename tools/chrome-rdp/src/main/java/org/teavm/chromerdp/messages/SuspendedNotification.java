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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.teavm.chromerdp.data.CallFrameDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SuspendedNotification {
    private CallFrameDTO[] callFrames;
    private String reason;
    private JsonNode data;
    private List<String> hitBreakpoints;

    public CallFrameDTO[] getCallFrames() {
        return callFrames;
    }

    public void setCallFrames(CallFrameDTO[] callFrames) {
        this.callFrames = callFrames;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public List<String> getHitBreakpoints() {
        return hitBreakpoints;
    }

    public void setHitBreakpoints(List<String> hitBreakpoints) {
        this.hitBreakpoints = hitBreakpoints;
    }
}
