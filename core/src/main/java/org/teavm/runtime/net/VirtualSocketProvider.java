/*
 *  Copyright 2025 Maksim Tiushev.
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
package org.teavm.runtime.net;

import org.teavm.interop.Platforms;
import org.teavm.interop.SupportedOn;

public final class VirtualSocketProvider {
    private static VirtualSocket instance;

    private VirtualSocketProvider() {
    }

    public static VirtualSocket getInstance() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    @SupportedOn(Platforms.WEBASSEMBLY_WASI)
    private static VirtualSocket create() {
        throw new UnsupportedOperationException();
    }

    public static void setInstance(VirtualSocket instance) {
        VirtualSocketProvider.instance = instance;
    }
}
