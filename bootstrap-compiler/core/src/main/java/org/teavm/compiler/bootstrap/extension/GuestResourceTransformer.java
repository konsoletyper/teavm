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
package org.teavm.compiler.bootstrap.extension;

import org.teavm.extension.bridge.BridgeGuestModule;
import org.teavm.extension.bridge.BridgeResource;
import org.teavm.extension.bridge.BridgeResourceHost;
import org.teavm.extension.bridge.BridgeResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformerEnvironment;

/**
 * Delegates to {@link ResourceTransformer}s, that run in guest module. Guest instantiates transformers once
 * per instance of this class, so a new instance should be used for every compilation.
 */
public class GuestResourceTransformer implements ResourceTransformer {
    private BridgeResourceTransformer remote;
    private ResourceTransformerEnvironment currentEnv;

    private GuestResourceTransformer() {
    }

    /**
     * Instantiates transformers in guest module.
     *
     * @return transformer or {@code null} if guest module does not contain any resource transformers.
     */
    public static GuestResourceTransformer create(BridgeGuestModule module) {
        var result = new GuestResourceTransformer();
        result.remote = module.createResourceTransformer(result.new Host());
        return result.remote != null ? result : null;
    }

    @Override
    public byte[] transform(ResourceTransformerEnvironment environment, String name, byte[] resource) {
        var previousEnv = currentEnv;
        currentEnv = environment;
        try {
            var result = remote.transform(name, resource);
            return result != null ? result : resource;
        } finally {
            currentEnv = previousEnv;
        }
    }

    private class Host implements BridgeResourceHost {
        @Override
        public String property(String name) {
            return currentEnv.property(name);
        }

        @Override
        public BridgeResource[] resources(String name) {
            return HostResource.toArray(currentEnv.resources(name));
        }
    }
}
