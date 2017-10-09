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
package org.teavm.jso.dom.xml;

import org.teavm.jso.JSProperty;

public interface CharacterData extends Node {
    @JSProperty
    String getData();

    @JSProperty
    void setData(String data);

    @JSProperty
    int getLength();

    String substringData(int offset, int count);

    void appendData(String arg);

    void insertData(int offset, String arg);

    void deleteData(int offset, int count);

    void replaceData(int offset, int count, String arg);
}
