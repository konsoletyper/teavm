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
import org.teavm.chromerdp.data.RemoteObjectDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFunctionResponse {
    private RemoteObjectDTO result;
    private boolean wasThrown;

    public RemoteObjectDTO getResult() {
        return result;
    }

    public void setResult(RemoteObjectDTO result) {
        this.result = result;
    }

    public boolean isWasThrown() {
        return wasThrown;
    }

    public void setWasThrown(boolean wasThrown) {
        this.wasThrown = wasThrown;
    }
}
