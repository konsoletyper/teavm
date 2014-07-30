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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.teavm.chromerdp.data.CallFrameDTO;

/**
 *
 * @author Alexey Andreev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuspendedNotification {
    private CallFrameDTO[] callFrames;
    private String reason;
    private JsonNode data;

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
}
