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
package org.teavm.jso.file;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.blob.Blob;
import org.teavm.jso.browser.DOMError;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class FileReader implements JSObject, FileReaderEventTarget {

    public static final int EMPTY = 0;

    public static final int LOADING = 1;

    public static final int DONE = 2;

    @JSProperty
    public abstract DOMError getError();

    @JSProperty
    public abstract int getReadyState();

    @JSProperty
    public abstract <T extends JSObject> T getResult();

    @JSProperty("result")
    public abstract JSString getResultAsString();

    @JSProperty("result")
    public abstract ArrayBuffer getResultAsArrayBuffer();

    public abstract void abort();

    public abstract <T extends Blob> void readAsArrayBuffer(T blob);

    public abstract <T extends Blob> void readAsBinaryString(T blob);

    public abstract <T extends Blob> void readAsDataURL(T blob);

    public abstract <T extends Blob> void readAsText(T blob);

    public abstract <T extends Blob> void readAsText(T blob, String encoding);

    @JSBody(params = {}, script = "return new FileReader();")
    public static native FileReader create();

}
