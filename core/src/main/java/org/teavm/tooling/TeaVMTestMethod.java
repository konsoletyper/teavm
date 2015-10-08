/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.tooling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class TeaVMTestMethod {
    private MethodReference method;
    private String fileName;
    private List<String> expectedExceptions = new ArrayList<>();

    public TeaVMTestMethod(MethodReference method, String fileName, List<String> expectedExceptions) {
        this.method = method;
        this.fileName = fileName;
        this.expectedExceptions = Collections.unmodifiableList(new ArrayList<>(expectedExceptions));
    }

    public MethodReference getMethod() {
        return method;
    }

    public String getFileName() {
        return fileName;
    }

    public List<String> getExpectedExceptions() {
        return expectedExceptions;
    }
}
