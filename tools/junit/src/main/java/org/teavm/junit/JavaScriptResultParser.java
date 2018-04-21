/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.junit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

final class JavaScriptResultParser {
    private JavaScriptResultParser() {
    }

    static void parseResult(String result, TestRunCallback callback) throws IOException  {
        if (result == null) {
            callback.complete();
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode resultObject = (ObjectNode) mapper.readTree(result);
        String status = resultObject.get("status").asText();
        switch (status) {
            case "ok":
                callback.complete();
                break;
            case "exception": {
                String stack = resultObject.get("stack").asText();
                String exception = resultObject.has("exception") ? resultObject.get("exception").asText() : null;
                callback.error(new AssertionError(exception + "\n" + stack));
                break;
            }
        }
    }
}
