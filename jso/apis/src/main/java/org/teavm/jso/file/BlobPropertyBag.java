/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.jso.file;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

@JSClass(transparent = true)
public abstract class BlobPropertyBag implements JSObject {
    public static final String ENDING_TYPE_TRANSPARENT = "transparent";
    public static final String ENDING_TYPE_NATIVE = "native";

    BlobPropertyBag() {
    }

    public static BlobPropertyBag create() {
        return JSObjects.create();
    }

    public BlobPropertyBag type(String type) {
        setType(type);
        return this;
    }

    // TODO: update signature when union of strings (lighweight enums) are supported in TeaVM
    public BlobPropertyBag endingType(String endingType) {
        setEndingType(endingType);
        return this;
    }

    @JSProperty
    private native void setType(String type);

    @JSProperty
    private native void setEndingType(String endingType);
}
