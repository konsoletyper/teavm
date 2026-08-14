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

public interface GPUPipeline extends GPU.ObjectBase {
    GPUBindGroup.Layout getBindGroupLayout(int index);

    interface Layout extends GPU.ObjectBase {
        // im done with this le stub bs
    }

    interface Compute extends GPUPipeline {
    }

    interface Render extends GPUPipeline {
    }

    final class ShaderStage {
        public static final int VERTEX = 0x1;
        public static final int FRAGMENT = 0x2;
        public static final int COMPUTE = 0x4;

        private ShaderStage() {
        }
    }

    final class ColorWrite {
        public static final int RED = 0x1;
        public static final int GREEN = 0x2;
        public static final int BLUE = 0x4;
        public static final int ALPHA = 0x8;
        public static final int ALL = 0xF;

        private ColorWrite() {
        }
    }
}

