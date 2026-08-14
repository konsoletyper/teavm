package org.teavm.jso.webgpu;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public interface GPUCanvasContext extends JSObject {
    @JSProperty
    HTMLCanvasElement getCanvas();

    void configure(GPUDescriptor.CanvasConfiguration configuration);

    void unconfigure();

    GPUDescriptor.CanvasConfiguration getConfiguration();

    GPUTexture getCurrentTexture();
}

