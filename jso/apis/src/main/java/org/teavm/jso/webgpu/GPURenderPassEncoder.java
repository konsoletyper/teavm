package org.teavm.jso.webgpu;

import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.typedarrays.Uint32Array;

public interface GPURenderPassEncoder extends GPU.ObjectBase {
    void setPipeline(GPUPipeline.Render pipeline);

    void setBindGroup(int index, GPUBindGroup bindGroup);

    void setBindGroup(int index, GPUBindGroup bindGroup, Uint32Array dynamicOffsets);

    void setIndexBuffer(GPUBuffer buffer, String indexFormat);

    void setIndexBuffer(GPUBuffer buffer, String indexFormat, double offset);

    void setIndexBuffer(GPUBuffer buffer, String indexFormat, double offset, double size);

    void setVertexBuffer(int slot, GPUBuffer buffer);

    void setVertexBuffer(int slot, GPUBuffer buffer, double offset);

    void setVertexBuffer(int slot, GPUBuffer buffer, double offset, double size);

    void draw(int vertexCount);

    void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance);

    void drawIndexed(int indexCount);

    void drawIndexed(int indexCount, int instanceCount, int firstIndex, int baseVertex, int firstInstance);

    void drawIndirect(GPUBuffer indirectBuffer, double indirectOffset);

    void drawIndexedIndirect(GPUBuffer indirectBuffer, double indirectOffset);

    void setViewport(double x, double y, double width, double height, double minDepth, double maxDepth);

    void setScissorRect(int x, int y, int width, int height);

    void setBlendConstant(GPUDescriptor.Color color);

    void setStencilReference(int reference);

    void beginOcclusionQuery(int queryIndex);

    void endOcclusionQuery();

    void executeBundles(JSArrayReader<Bundle> bundles);

    void end();

    void pushDebugGroup(String groupLabel);

    void popDebugGroup();

    void insertDebugMarker(String markerLabel);

    interface Bundle extends GPU.ObjectBase {
        // le ✨stub✨
    }

    interface BundleEncoder extends GPU.ObjectBase {
        void setPipeline(GPUPipeline.Render pipeline);

        void setBindGroup(int index, GPUBindGroup bindGroup);

        void setIndexBuffer(GPUBuffer buffer, String indexFormat);

        void setVertexBuffer(int slot, GPUBuffer buffer);

        void draw(int vertexCount);

        void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance);

        void drawIndexed(int indexCount);

        void drawIndexed(int indexCount, int instanceCount, int firstIndex, int baseVertex, int firstInstance);

        Bundle finish();

        Bundle finish(GPUDescriptor.RenderBundle descriptor);
    }
}

