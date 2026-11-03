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
package org.teavm.compiler.bootstrap;

import org.teavm.extension.bridge.BridgeGuestModule;

/**
 * Loads module with extensions, compiled by {@link TeaVMBootstrapCompiler}. Since Wasm modules can only be
 * instantiated asynchronously, loader reports result via callback.
 */
public interface ExtensionModuleLoader {
    void load(byte[] module, Callback callback);

    interface Callback {
        void loaded(BridgeGuestModule module);

        void failed(String message);
    }
}
