package org.teavm.jso.webgpu;

import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

public interface GPUQueue extends GPU.ObjectBase {
    void submit(JSArrayReader<GPUCommandEncoder.CommandBuffer> commandBuffers);

    JSPromise<JSUndefined> onSubmittedWorkDone();

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBuffer data);

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBuffer data, double dataOffset);

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBuffer data, double dataOffset, double size);

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBufferView data);

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBufferView data, double dataOffset);

    void writeBuffer(GPUBuffer buffer, double bufferOffset, ArrayBufferView data, double dataOffset, double size);

    void writeTexture(GPUDescriptor.TexelCopyTextureInfo destination, ArrayBuffer data, GPUDescriptor.TexelCopyBufferLayout dataLayout, GPUDescriptor.Extent3D size);

    void writeTexture(GPUDescriptor.TexelCopyTextureInfo destination, ArrayBufferView data, GPUDescriptor.TexelCopyBufferLayout dataLayout, GPUDescriptor.Extent3D size);

    void copyExternalImageToTexture(GPUDescriptor.CopyExternalImageSourceInfo source, GPUDescriptor.CopyExternalImageDestInfo destination, GPUDescriptor.Extent3D copySize);
}
