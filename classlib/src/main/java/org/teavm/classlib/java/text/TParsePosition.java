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
package org.teavm.classlib.java.text;

public class TParsePosition {
    private int currentPosition;
    private int errorIndex = -1;

    public TParsePosition(int index) {
        currentPosition = index;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TParsePosition)) {
            return false;
        }
        TParsePosition pos = (TParsePosition) object;
        return currentPosition == pos.currentPosition && errorIndex == pos.errorIndex;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public int getIndex() {
        return currentPosition;
    }

    @Override
    public int hashCode() {
        return currentPosition + errorIndex;
    }

    public void setErrorIndex(int index) {
        errorIndex = index;
    }

    public void setIndex(int index) {
        currentPosition = index;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[index=" + currentPosition + ", errorIndex=" + errorIndex + "]";
    }
}
