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
package org.teavm.extension.bridge;

import org.teavm.jso.JSObject;

/**
 * Exports of guest module, see {@link org.teavm.extension.bridge.guest.ExtensionGuest}.
 */
public interface BridgeGuestModule extends JSObject {
    /**
     * Instantiates extensions. Should be called once per compilation, since extensions may have state.
     */
    BridgeGuest create(BridgeHost host);

    /**
     * Collects rules from all {@link org.teavm.extension.spi.substitution.SubstitutionPolicy}s. Unlike
     * {@link #create(BridgeHost)}, does not require compilation to be started, since substitution is applied
     * while parsing classes.
     */
    BridgeSubstitutionRule[] substitutionRules();

    /**
     * Instantiates {@link org.teavm.extension.spi.transformation.ResourceTransformer}s. Should be called once
     * per compilation.
     *
     * @return combined transformer or {@code null} if there are no transformers.
     */
    BridgeResourceTransformer createResourceTransformer(BridgeResourceHost host);
}
