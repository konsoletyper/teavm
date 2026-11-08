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
package org.teavm.extension;

import java.nio.charset.StandardCharsets;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformerEnvironment;

/**
 * Replaces string constant in class file of {@link TransformedGreeter} with a value of the same length,
 * which allows to patch constant pool in place.
 */
@Autoregistered
public class TestResourceTransformer implements ResourceTransformer {
    private static final String TARGET = "org/teavm/extension/TransformedGreeter.class";
    private static final String ORIGINAL = "teavm-original-value";
    private static final String REPLACEMENT = "teavm-patched--value";

    @Override
    public byte[] transform(ResourceTransformerEnvironment environment, String name, byte[] resource) {
        if (!name.equals(TARGET)) {
            return resource;
        }
        var original = ORIGINAL.getBytes(StandardCharsets.UTF_8);
        var replacement = REPLACEMENT.getBytes(StandardCharsets.UTF_8);
        var result = resource.clone();
        var index = indexOf(result, original);
        if (index < 0) {
            throw new IllegalStateException("Constant not found in " + name);
        }
        System.arraycopy(replacement, 0, result, index, replacement.length);
        return result;
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (var i = 0; i <= data.length - pattern.length; ++i) {
            for (var j = 0; j < pattern.length; ++j) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
