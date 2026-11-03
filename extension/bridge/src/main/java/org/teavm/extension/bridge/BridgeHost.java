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
 * Exposes compiler's {@link org.teavm.extension.ExtensionEnvironment} to guest.
 */
public interface BridgeHost extends JSObject {
    BridgeClass findClass(String name);

    BridgeClass findClassByDesc(String desc);

    String property(String name);

    BridgeResource[] resources(String name);

    /**
     * Reports diagnostic message.
     *
     * @param method method to report problem at, nullable.
     * @param paramElements for each parameter either an introspected element or null. In the latter case
     *                      parameter is taken from {@code paramStrings}.
     */
    void report(boolean error, BridgeMethod method, String fileName, int lineNumber, String message,
            BridgeElement[] paramElements, String[] paramStrings);
}
