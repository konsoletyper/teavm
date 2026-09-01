package org.teavm.jso.webgpu;

import org.teavm.jso.typedarrays.Uint32Array;

public interface GPUComputePassEncoder extends GPU.ObjectBase {
    void setPipeline(GPUPipeline.Compute pipeline);

    void setBindGroup(int index, GPUBindGroup bindGroup);

    void setBindGroup(int index, GPUBindGroup bindGroup, Uint32Array dynamicOffsets);

    void setBindGroup(int index, GPUBindGroup bindGroup, Uint32Array dynamicOffsets, double dynamicOffsetsDataStart, int dynamicOffsetsDataLength);

    void dispatchWorkgroups(int workgroupCountX);

    void dispatchWorkgroups(int workgroupCountX, int workgroupCountY);

    void dispatchWorkgroups(int workgroupCountX, int workgroupCountY, int workgroupCountZ);

    void dispatchWorkgroupsIndirect(GPUBuffer indirectBuffer, double indirectOffset);

    void end();

    void pushDebugGroup(String groupLabel);

    void popDebugGroup();

    void insertDebugMarker(String markerLabel);
}

