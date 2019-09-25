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
package org.teavm.model.lowlevel;

import java.util.Objects;

public class ExportedMethodKey {
    public final String functionClassName;
    public final String className;
    public final String methodName;

    public ExportedMethodKey(String functionClassName, String className, String methodName) {
        this.functionClassName = functionClassName;
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExportedMethodKey that = (ExportedMethodKey) o;
        return Objects.equals(functionClassName, that.functionClassName)
                && Objects.equals(className, that.className)
                && Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionClassName, className, methodName);
    }
}
