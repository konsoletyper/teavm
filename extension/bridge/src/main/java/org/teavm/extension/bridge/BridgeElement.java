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
 * Base interface for introspected elements, passed from host to guest.
 */
public interface BridgeElement extends JSObject {
    /**
     * One of {@link BridgeKinds} element constants, i.e. {@link BridgeKinds#CLASS}, {@link BridgeKinds#FIELD}, etc.
     * Guest relies on it to determine which interface element implements.
     */
    int kind();

    /**
     * Text representation of element, as returned by {@code toString()} of host's element.
     */
    String describe();
}
