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

import java.util.ArrayDeque;
import java.util.Deque;

public class JsonVisitingConsumer extends JsonConsumer {
    private Deque<JsonVisitor> visitorStack = new ArrayDeque<>();
    private int noVisitorLevel;

    public JsonVisitingConsumer(JsonVisitor visitor) {
        visitorStack.push(visitor);
    }

    @Override
    public void enterObject(JsonErrorReporter reporter) {
        if (noVisitorLevel == 0) {
            JsonVisitor next = visitorStack.peek().object(reporter);
            if (next == null) {
                noVisitorLevel = 1;
            } else {
                visitorStack.push(next);
            }
        } else {
            noVisitorLevel++;
        }
    }

    @Override
    public void exitObject(JsonErrorReporter reporter) {
        exit(reporter);
    }

    @Override
    public void enterArray(JsonErrorReporter reporter) {
        if (noVisitorLevel == 0) {
            JsonVisitor next = visitorStack.peek().array(reporter);
            if (next == null) {
                noVisitorLevel = 1;
            } else {
                visitorStack.push(next);
            }
        } else {
            noVisitorLevel++;
        }
    }

    @Override
    public void exitArray(JsonErrorReporter reporter) {
        exit(reporter);
    }

    @Override
    public void enterProperty(JsonErrorReporter reporter, String name) {
        if (noVisitorLevel == 0) {
            JsonVisitor next = visitorStack.peek().property(reporter, name);
            if (next == null) {
                noVisitorLevel = 1;
            } else {
                visitorStack.push(next);
            }
        } else {
            noVisitorLevel++;
        }
    }

    @Override
    public void exitProperty(JsonErrorReporter reporter, String name) {
        exit(reporter);
    }

    private void exit(JsonErrorReporter reporter) {
        if (noVisitorLevel > 0) {
            noVisitorLevel--;
        } else {
            visitorStack.pop();
        }
        if (noVisitorLevel == 0 && !visitorStack.isEmpty()) {
            visitorStack.peek().end(reporter);
        }
    }

    @Override
    public void stringValue(JsonErrorReporter reporter, String value) {
        if (noVisitorLevel == 0) {
            visitorStack.peek().stringValue(reporter, value);
        }
    }

    @Override
    public void intValue(JsonErrorReporter reporter, long value) {
        if (noVisitorLevel == 0) {
            visitorStack.peek().intValue(reporter, value);
        }
    }

    @Override
    public void floatValue(JsonErrorReporter reporter, double value) {
        if (noVisitorLevel == 0) {
            visitorStack.peek().floatValue(reporter, value);
        }
    }

    @Override
    public void nullValue(JsonErrorReporter reporter) {
        if (noVisitorLevel == 0) {
            visitorStack.peek().nullValue(reporter);
        }
    }

    @Override
    public void booleanValue(JsonErrorReporter reporter, boolean value) {
        if (noVisitorLevel == 0) {
            visitorStack.peek().booleanValue(reporter, value);
        }
    }
}
