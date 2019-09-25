/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.util.json;

public abstract class JsonConsumer {
    public void enterObject(JsonErrorReporter reporter) {
    }

    public void exitObject(JsonErrorReporter reporter) {
    }

    public void enterArray(JsonErrorReporter reporter) {
    }

    public void exitArray(JsonErrorReporter reporter) {
    }

    public void enterProperty(JsonErrorReporter reporter, String name) {
    }

    public void exitProperty(JsonErrorReporter reporter, String name) {
    }

    public void stringValue(JsonErrorReporter reporter, String value) {
    }

    public void intValue(JsonErrorReporter reporter, long value) {
    }

    public void floatValue(JsonErrorReporter reporter, double value) {
    }

    public void nullValue(JsonErrorReporter reporter) {
    }

    public void booleanValue(JsonErrorReporter reporter, boolean value) {
    }
}
