/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.jso.webgpu;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;

public interface GPU extends JSObject {
    JSPromise<GPUAdapter> requestAdapter();

    JSPromise<GPUAdapter> requestAdapter(GPUDescriptor.RequestAdapterOptions options);

    String getPreferredCanvasFormat();

    @JSProperty
    StringSet getWgslLanguageFeatures();

    interface StringSet extends JSObject {
        @JSProperty
        int getSize();

        boolean has(String value);
    }

    interface ObjectBase extends JSObject {
        @JSProperty
        String getLabel();

        @JSProperty
        void setLabel(String label);
    }
}

