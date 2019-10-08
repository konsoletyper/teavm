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
package org.teavm.model;

import java.io.Serializable;
import java.util.Objects;

public class InliningInfo implements Serializable {
    private MethodReference method;
    private String fileName;
    private int line;
    private InliningInfo parent;
    private transient int hash;

    public InliningInfo(MethodReference method, String fileName, int line, InliningInfo parent) {
        this.method = method;
        this.fileName = fileName;
        this.line = line;
        this.parent = parent;
    }

    public MethodReference getMethod() {
        return method;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public InliningInfo getParent() {
        return parent;
    }

    public boolean isEmpty() {
        return fileName == null && line < 0;
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            final int prime = 31;
            result = 1;
            result = prime * result + method.hashCode();
            result = prime * result + (fileName == null ? 0 : fileName.hashCode());
            result = prime * result + line;
            result = prime * result + (parent != null ? parent.hashCode() : 0);
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InliningInfo)) {
            return false;
        }
        InliningInfo that = (InliningInfo) obj;
        return Objects.equals(method, that.method)
                && Objects.equals(fileName, that.fileName)
                && line == that.line
                && Objects.equals(parent, that.parent);
    }
}
