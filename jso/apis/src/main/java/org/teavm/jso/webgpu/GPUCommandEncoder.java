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

public interface GPUCommandEncoder extends GPU.ObjectBase {
    GPURenderPassEncoder beginRenderPass(GPUDescriptor.RenderPass descriptor);

    GPUComputePassEncoder beginComputePass();

    GPUComputePassEncoder beginComputePass(GPUDescriptor.ComputePass descriptor);

    void copyBufferToBuffer(GPUBuffer source, GPUBuffer destination, double size);

    void copyBufferToBuffer(GPUBuffer source, double sourceOffset, GPUBuffer destination, double destinationOffset,
            double size);

    void copyBufferToTexture(GPUDescriptor.TexelCopyBufferInfo source, GPUDescriptor.TexelCopyTextureInfo destination,
            GPUDescriptor.Extent3D copySize);

    void copyTextureToBuffer(GPUDescriptor.TexelCopyTextureInfo source, GPUDescriptor.TexelCopyBufferInfo destination,
            GPUDescriptor.Extent3D copySize);

    void copyTextureToTexture(GPUDescriptor.TexelCopyTextureInfo source, GPUDescriptor.TexelCopyTextureInfo destination,
            GPUDescriptor.Extent3D copySize);

    void clearBuffer(GPUBuffer buffer);

    void clearBuffer(GPUBuffer buffer, double offset);

    void clearBuffer(GPUBuffer buffer, double offset, double size);

    void resolveQuerySet(GPUDevice.QuerySet querySet, int firstQuery, int queryCount, GPUBuffer destination,
            double destinationOffset);

    CommandBuffer finish();

    CommandBuffer finish(GPUDescriptor.CommandBuffer descriptor);

    void pushDebugGroup(String groupLabel);

    void popDebugGroup();

    void insertDebugMarker(String markerLabel);

    interface CommandBuffer extends GPU.ObjectBase {
    }
}

