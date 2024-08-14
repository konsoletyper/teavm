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
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

@JSClass(transparent = true)
public class FilePropertyBag extends BlobPropertyBag {
    FilePropertyBag() {
    }

    public static FilePropertyBag create() {
        return JSObjects.create();
    }

    @Override
    public FilePropertyBag type(String type) {
        return (FilePropertyBag) super.type(type);
    }

    @Override
    public FilePropertyBag endingType(String endingType) {
        return (FilePropertyBag) super.endingType(endingType);
    }

    public FilePropertyBag lastModified(long lastModified) {
        setLastModified(lastModified);
        return this;
    }

    @JSProperty
    private native void setLastModified(double lastModified);
}
