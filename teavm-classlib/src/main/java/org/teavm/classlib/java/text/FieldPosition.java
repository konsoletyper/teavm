/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.java.text;

public class FieldPosition {
    private int myField, beginIndex, endIndex;
    private Format.Field myAttribute;

    public FieldPosition(int field) {
        myField = field;
    }

    public FieldPosition(Format.Field attribute) {
        myAttribute = attribute;
        myField = -1;
    }

    public FieldPosition(Format.Field attribute, int field) {
        myAttribute = attribute;
        myField = field;
    }

    void clear() {
        beginIndex = endIndex = 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FieldPosition)) {
            return false;
        }
        FieldPosition pos = (FieldPosition) object;
        return myField == pos.myField && myAttribute == pos.myAttribute && beginIndex == pos.beginIndex &&
                endIndex == pos.endIndex;
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

    public Format.Field getFieldAttribute() {
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
        return getClass().getName() + "[attribute=" + myAttribute + ", field=" + myField + ", beginIndex=" +
                beginIndex + ", endIndex=" + endIndex + "]";
    }
}
