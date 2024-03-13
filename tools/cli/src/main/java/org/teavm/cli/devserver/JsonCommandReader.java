/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.cli.devserver;

import java.util.function.Consumer;
import org.teavm.common.json.JsonValue;
import org.teavm.devserver.DevServer;

public class JsonCommandReader implements Consumer<JsonValue> {
    private DevServer devServer;

    public JsonCommandReader(DevServer devServer) {
        this.devServer = devServer;
    }

    @Override
    public void accept(JsonValue jsonValue) {
        var obj = jsonValue.asObject();
        var type = obj.get("type").asString();
        switch (type) {
            case "build":
                devServer.buildProject();
                break;
            case "cancel":
                devServer.cancelBuild();
                break;
            case "stop":
                System.exit(0);
                break;
        }
    }
}
