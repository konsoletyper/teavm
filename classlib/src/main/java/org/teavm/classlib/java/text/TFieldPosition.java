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

public class TFieldPosition {
    private int myField;
    private int beginIndex;
    private int endIndex;
    private TFormat.Field myAttribute;

    public TFieldPosition(int field) {
        myField = field;
    }

    public TFieldPosition(TFormat.Field attribute) {
        myAttribute = attribute;
        myField = -1;
    }

    public TFieldPosition(TFormat.Field attribute, int field) {
        myAttribute = attribute;
        myField = field;
    }

    void clear() {
        beginIndex = 0;
        endIndex = 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TFieldPosition)) {
            return false;
        }
        TFieldPosition pos = (TFieldPosition) object;
        return myField == pos.myField && myAttribute == pos.myAttribute && beginIndex == pos.beginIndex
                && endIndex == pos.endIndex;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getField() {
        return myField;
    }

    public TFormat.Field getFieldAttribute() {
        return myAttribute;
    }

    @Override
    public int hashCode() {
        int attributeHash = (myAttribute == null) ? 0 : myAttribute.hashCode();
        return attributeHash + myField * 10 + beginIndex * 100 + endIndex;
    }

    public void setBeginIndex(int index) {
        beginIndex = index;
    }

    public void setEndIndex(int index) {
        endIndex = index;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[attribute=" + myAttribute + ", field=" + myField + ", beginIndex="
                + beginIndex + ", endIndex=" + endIndex + "]";
    }
}
