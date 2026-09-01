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

