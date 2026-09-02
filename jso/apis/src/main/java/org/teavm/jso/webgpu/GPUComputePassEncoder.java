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

import org.teavm.jso.typedarrays.Uint32Array;

public interface GPUComputePassEncoder extends GPU.ObjectBase {
    void setPipeline(GPUPipeline.Compute pipeline);

    void setBindGroup(int index, GPUBindGroup bindGroup);

    void setBindGroup(int index, GPUBindGroup bindGroup, Uint32Array dynamicOffsets);

    void setBindGroup(int index, GPUBindGroup bindGroup, Uint32Array dynamicOffsets, double dynamicOffsetsDataStart,
            int dynamicOffsetsDataLength);

    void dispatchWorkgroups(int workgroupCountX);

    void dispatchWorkgroups(int workgroupCountX, int workgroupCountY);

    void dispatchWorkgroups(int workgroupCountX, int workgroupCountY, int workgroupCountZ);

    void dispatchWorkgroupsIndirect(GPUBuffer indirectBuffer, double indirectOffset);

    void end();

    void pushDebugGroup(String groupLabel);

    void popDebugGroup();

    void insertDebugMarker(String markerLabel);
}

