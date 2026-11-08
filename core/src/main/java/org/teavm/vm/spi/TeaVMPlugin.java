/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.vm.spi;

/**
 * <p><strong>Warning!</strong> It's an INTERNAL API. It's intended mainly for TeaVM internal needs.
 * You are not supposed to implement this class. Use on your own risk. TeaVM internals are the subject of change.
 * Any changes to this interface are not considered as breaking changes and never
 * mentioned in release notes.</p>
 *
 * <p>Instead, you should consider official stable SPI (see {@code teavm-extension-spi} artifact)</p>
 */
public interface TeaVMPlugin {
    void install(TeaVMHost host);
}
