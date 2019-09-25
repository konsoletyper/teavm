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

public class JsonAllErrorVisitor extends JsonVisitor {
    @Override
    public JsonVisitor object(JsonErrorReporter reporter) {
        reporter.error("Unexpected object");
        return null;
    }

    @Override
    public JsonVisitor array(JsonErrorReporter reporter) {
        reporter.error("Unexpected array");
        return null;
    }

    @Override
    public JsonVisitor property(JsonErrorReporter reporter, String name) {
        reporter.error("Unexpected property");
        return null;
    }

    @Override
    public void stringValue(JsonErrorReporter reporter, String value) {
        reporter.error("Unexpected string");
    }

    @Override
    public void intValue(JsonErrorReporter reporter, long value) {
        reporter.error("Unexpected number");
    }

    @Override
    public void floatValue(JsonErrorReporter reporter, double value) {
        reporter.error("Unexpected number");
    }

    @Override
    public void nullValue(JsonErrorReporter reporter) {
        reporter.error("Unexpected null");
    }

    @Override
    public void booleanValue(JsonErrorReporter reporter, boolean value) {
        reporter.error("Unexpected boolean");
    }
}
