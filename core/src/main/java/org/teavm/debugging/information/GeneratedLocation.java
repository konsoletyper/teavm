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

public class GeneratedLocation implements Comparable<GeneratedLocation> {
    private int line;
    private int column;

    public GeneratedLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public int compareTo(GeneratedLocation o) {
        int r = Integer.compare(line, o.line);
        if (r == 0) {
            r = Integer.compare(column, o.column);
        }
        return r;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + column;
        result = prime * result + line;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeneratedLocation)) {
            return false;
        }
        GeneratedLocation other = (GeneratedLocation) obj;
        return line == other.line && column == other.column;
    }

    @Override
    public String toString() {
        return "line: " + line + ", column: " + column;
    }
}
