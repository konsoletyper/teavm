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
package org.teavm.classlib.impl.regex;

/**
 * Receives a sequence of calls to static methods of {@code java.util.regex.PatternFactory} that reconstruct
 * a pattern compiled in build time. Arguments are either {@link Integer}, {@link Boolean}, {@link Character},
 * {@link String}, {@code null} or objects that previously were passed as {@code node} to
 * {@link #create(Object, String, Object[])}.
 */
public interface PatternConstructionListener {
    void create(Object node, String factoryMethod, Object[] arguments);

    void call(String factoryMethod, Object[] arguments);
}
