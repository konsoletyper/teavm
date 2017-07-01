/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.MethodReference;

class JSBodyRepository {
    public final Map<MethodReference, JSBodyEmitter> emitters = new HashMap<>();
    public final Map<MethodReference, MethodReference> methodMap = new HashMap<>();
    public final Set<MethodReference> processedMethods = new HashSet<>();
    public final Set<MethodReference> inlineMethods = new HashSet<>();
    public final Map<MethodReference, MethodReference> callbackCallees = new HashMap<>();
    public final Map<MethodReference, Set<MethodReference>> callbackMethods = new HashMap<>();
    public final Map<MethodReference, Set<MethodReference>> callbackMethodsDeps = new HashMap<>();
}
