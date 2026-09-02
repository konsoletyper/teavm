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

    void writeTexture(GPUDescriptor.TexelCopyTextureInfo destination, ArrayBuffer data,
            GPUDescriptor.TexelCopyBufferLayout dataLayout, GPUDescriptor.Extent3D size);

    void writeTexture(GPUDescriptor.TexelCopyTextureInfo destination, ArrayBufferView data,
            GPUDescriptor.TexelCopyBufferLayout dataLayout, GPUDescriptor.Extent3D size);

    void copyExternalImageToTexture(GPUDescriptor.CopyExternalImageSourceInfo source,
            GPUDescriptor.CopyExternalImageDestInfo destination, GPUDescriptor.Extent3D copySize);
}
