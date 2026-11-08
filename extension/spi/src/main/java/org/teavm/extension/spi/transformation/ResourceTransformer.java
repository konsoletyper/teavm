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
package org.teavm.extension.spi.transformation;

/**
 * <p>Allows transformation of resources (including class files).
 * The PRIMARY goal of this extension point is class file transformation.
 * Usual scenario: your app contains some code that is not supported by TeaVM.
 * In this case you can patch problematic parts.</p>
 *
 * <p>It does not provide any routine to parse classes - use your favourite library for that (i.e. ASM).</p>
 *
 * <p>You also MAY use it to transform other resources specifically for TeaVM.</p>
 */
public interface ResourceTransformer {
    byte[] transform(ResourceTransformerEnvironment environment, String name, byte[] resource);
}
