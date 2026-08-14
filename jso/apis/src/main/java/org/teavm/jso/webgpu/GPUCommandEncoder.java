package org.teavm.jso.webgpu;

public interface GPUCommandEncoder extends GPU.ObjectBase {
    GPURenderPassEncoder beginRenderPass(GPUDescriptor.RenderPass descriptor);

    GPUComputePassEncoder beginComputePass();

    GPUComputePassEncoder beginComputePass(GPUDescriptor.ComputePass descriptor);

    void copyBufferToBuffer(GPUBuffer source, GPUBuffer destination, double size);

    void copyBufferToBuffer(GPUBuffer source, double sourceOffset, GPUBuffer destination, double destinationOffset, double size);

    void copyBufferToTexture(GPUDescriptor.TexelCopyBufferInfo source, GPUDescriptor.TexelCopyTextureInfo destination, GPUDescriptor.Extent3D copySize);

    void copyTextureToBuffer(GPUDescriptor.TexelCopyTextureInfo source, GPUDescriptor.TexelCopyBufferInfo destination, GPUDescriptor.Extent3D copySize);

    void copyTextureToTexture(GPUDescriptor.TexelCopyTextureInfo source, GPUDescriptor.TexelCopyTextureInfo destination, GPUDescriptor.Extent3D copySize);

    void clearBuffer(GPUBuffer buffer);

    void clearBuffer(GPUBuffer buffer, double offset);

    void clearBuffer(GPUBuffer buffer, double offset, double size);

    void resolveQuerySet(GPUDevice.QuerySet querySet, int firstQuery, int queryCount, GPUBuffer destination, double destinationOffset);

    CommandBuffer finish();

    CommandBuffer finish(GPUDescriptor.CommandBuffer descriptor);

    void pushDebugGroup(String groupLabel);

    void popDebugGroup();

    void insertDebugMarker(String markerLabel);

    interface CommandBuffer extends GPU.ObjectBase {
        // le ✨stub✨
    }
}

