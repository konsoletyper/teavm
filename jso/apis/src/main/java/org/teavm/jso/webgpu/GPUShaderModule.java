package org.teavm.jso.webgpu;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSPromise;

public interface GPUShaderModule extends GPU.ObjectBase {
    JSPromise<CompilationInfo> getCompilationInfo();

    interface CompilationInfo extends JSObject {
        @JSProperty
        JSArrayReader<CompilationMessage> getMessages();
    }

    interface CompilationMessage extends JSObject {
        @JSProperty
        String getMessage();

        @JSProperty
        String getType();

        @JSProperty
        double getLineNum();

        @JSProperty
        double getLinePos();

        @JSProperty
        double getOffset();

        @JSProperty
        double getLength();
    }
}

