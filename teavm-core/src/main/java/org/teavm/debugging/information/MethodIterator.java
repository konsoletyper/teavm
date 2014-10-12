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
package org.teavm.debugging.information;

import org.teavm.model.MethodDescriptor;

/**
 *
 * @author Alexey Andreev
 */
public class MethodIterator {
    private DebugInformation debugInformation;
    private int index;

    MethodIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
    }

    public boolean isEndReached() {
        return index < debugInformation.methodMapping.size();
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return DebugInformation.key(debugInformation.methodMapping.get(index));
    }

    public int getMethodId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return debugInformation.methodMapping.get(index).get(2);
    }

    public MethodDescriptor getMethod() {
        int methodId = getMethodId();
        return methodId >= 0 ? debugInformation.getMethod(methodId) : null;
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        ++index;
    }
}
