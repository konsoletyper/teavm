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
package org.teavm.model;

import java.io.Serializable;
import java.util.Objects;

public class TextLocation implements Serializable {
    public static final TextLocation EMPTY = new TextLocation(null, -1);
    private static final InliningInfo[] EMPTY_ARRAY = new InliningInfo[0];

    private String fileName;
    private int line;
    private InliningInfo inlining;
    private transient int hash;

    public TextLocation(String fileName, int line) {
        this(fileName, line, null);
    }

    public TextLocation(String fileName, int line, InliningInfo inlining) {
        this.fileName = fileName;
        this.line = line;
        this.inlining = inlining;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public InliningInfo getInlining() {
        return inlining;
    }

    public InliningInfo[] getInliningPath() {
        if (inlining == null) {
            return EMPTY_ARRAY;
        }

        InliningInfo inlining = this.inlining;
        int sz = 0;
        while (inlining != null) {
            sz++;
            inlining = inlining.getParent();
        }

        InliningInfo[] result = new InliningInfo[sz];
        inlining = this.inlining;
        while (inlining != null) {
            result[--sz] = inlining;
            inlining = inlining.getParent();
        }

        return result;
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
            result = prime * result + (fileName == null ? 0 : fileName.hashCode());
            result = prime * result + line;
            result = prime * result + (inlining != null ? inlining.hashCode() : 0);
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextLocation)) {
            return false;
        }
        TextLocation other = (TextLocation) obj;
        return Objects.equals(fileName, other.fileName) && line == other.line
                && Objects.equals(inlining, other.inlining);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fileName).append(':').append(line);
        InliningInfo inlining = this.inlining;
        if (inlining != null) {
            sb.append('[');
            boolean first = true;
            while (inlining != null) {
                if (!first) {
                    sb.append("->");
                }
                first = false;
                sb.append(inlining.getMethod()).append("@")
                        .append(inlining.getFileName()).append(':').append(inlining.getLine());
                inlining = inlining.getParent();
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
