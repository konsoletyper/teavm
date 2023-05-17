/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.common.json;

import java.util.function.Consumer;

public abstract class JsonValueParserVisitor extends JsonAllErrorVisitor {
    public abstract void consume(JsonValue value);

    public static JsonValueParserVisitor create(Consumer<JsonValue> consumer) {
        return new JsonValueParserVisitor() {
            @Override
            public void consume(JsonValue value) {
                consumer.accept(value);
            }
        };
    }

    @Override
    public JsonVisitor object(JsonErrorReporter reporter) {
        var jsonObject = new JsonObjectValue();
        consume(jsonObject);
        return new JsonAllErrorVisitor() {
            @Override
            public JsonVisitor property(JsonErrorReporter reporter, String name) {
                return new JsonValueParserVisitor() {
                    @Override
                    public void consume(JsonValue value) {
                        jsonObject.put(name, value);
                    }
                };
            }
        };
    }

    @Override
    public JsonVisitor array(JsonErrorReporter reporter) {
        var jsonArray = new JsonArrayValue();
        consume(jsonArray);
        return new JsonAllErrorVisitor() {
            @Override
            public JsonVisitor array(JsonErrorReporter reporter) {
                return new JsonValueParserVisitor() {
                    @Override
                    public void consume(JsonValue value) {
                        jsonArray.add(value);
                    }
                };
            }
        };
    }

    @Override
    public void stringValue(JsonErrorReporter reporter, String value) {
        consume(new JsonStringValue(value));
    }

    @Override
    public void intValue(JsonErrorReporter reporter, long value) {
        consume(new JsonIntValue(value));
    }

    @Override
    public void floatValue(JsonErrorReporter reporter, double value) {
        consume(new JsonFloatValue(value));
    }

    @Override
    public void nullValue(JsonErrorReporter reporter) {
        consume(new JsonNullValue());
    }

    @Override
    public void booleanValue(JsonErrorReporter reporter, boolean value) {
        consume(new JsonBooleanValue(value));
    }
}
