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
package org.teavm.extension.bridge.guest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.teavm.extension.bridge.BridgeResourceHost;
import org.teavm.extension.bridge.BridgeResourceTransformer;
import org.teavm.extension.resource.Resource;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformerEnvironment;

class GuestResourceTransformer implements BridgeResourceTransformer, ResourceTransformerEnvironment {
    private final BridgeResourceHost host;
    private final List<ResourceTransformer> transformers;

    GuestResourceTransformer(BridgeResourceHost host, List<ResourceTransformer> transformers) {
        this.host = host;
        this.transformers = transformers;
    }

    @Override
    public byte[] transform(String name, byte[] bytes) {
        var changed = false;
        for (var transformer : transformers) {
            var newBytes = transformer.transform(this, name, bytes);
            if (newBytes != null && newBytes != bytes && !Arrays.equals(newBytes, bytes)) {
                bytes = newBytes;
                changed = true;
            }
        }
        return changed ? bytes : null;
    }

    @Override
    public Iterator<Resource> resources(String name) {
        var remoteResources = host.resources(name);
        var result = new ArrayList<Resource>(remoteResources.length);
        for (var remoteResource : remoteResources) {
            result.add(() -> new ByteArrayInputStream(remoteResource.read()));
        }
        return result.iterator();
    }

    @Override
    public ClassLoader classLoader() {
        throw new UnsupportedOperationException("Class loader is not available when extensions run "
                + "in a separate module");
    }

    @Override
    public String property(String name) {
        return host.property(name);
    }
}
