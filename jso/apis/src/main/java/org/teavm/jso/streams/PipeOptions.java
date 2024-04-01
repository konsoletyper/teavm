/*
 *  Copyright 2024 ihromant.
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
package org.teavm.jso.streams;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class PipeOptions implements JSObject {
    @JSBody(script = "return {};")
    public static native PipeOptions create();

    @JSProperty
    private native void setPreventClose(boolean value);

    public PipeOptions preventClose(boolean value) {
        setPreventClose(value);
        return this;
    }

    @JSProperty
    private native void setPreventAbort(boolean value);

    public PipeOptions preventAbort(boolean value) {
        setPreventAbort(value);
        return this;
    }

    @JSProperty
    private native void setPreventCancel(boolean value);

    public PipeOptions preventCancel(boolean value) {
        setPreventCancel(value);
        return this;
    }

    @JSProperty
    private native void setSignal(AbortSignal signal);

    public PipeOptions signal(AbortSignal signal) {
        setSignal(signal);
        return this;
    }
}
