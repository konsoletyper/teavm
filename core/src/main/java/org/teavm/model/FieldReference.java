/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model;

import java.io.Serializable;

public class FieldReference implements Serializable {
    private String className;
    private String fieldName;

    public FieldReference(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

    public String getClassName() {
        return className;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        return className.hashCode() ^ fieldName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldReference)) {
            return false;
        }
        FieldReference other = (FieldReference) obj;
        return className.equals(other.className) && fieldName.equals(other.fieldName);
    }

    @Override
    public String toString() {
        return className + "." + fieldName;
    }
}
