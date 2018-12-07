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
import org.teavm.chromerdp.data.CallArgumentDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFunctionCommand {
    private String objectId;
    private String functionDeclaration;
    private CallArgumentDTO[] arguments;
    private boolean returnByValue;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public void setFunctionDeclaration(String functionDeclaration) {
        this.functionDeclaration = functionDeclaration;
    }

    public CallArgumentDTO[] getArguments() {
        return arguments;
    }

    public void setArguments(CallArgumentDTO[] arguments) {
        this.arguments = arguments;
    }

    public boolean isReturnByValue() {
        return returnByValue;
    }

    public void setReturnByValue(boolean returnByValue) {
        this.returnByValue = returnByValue;
    }
}
