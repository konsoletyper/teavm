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
package org.teavm.debugging.information;

import org.teavm.backend.javascript.codegen.LocationProvider;
import org.teavm.model.MethodDescriptor;

public interface DebugInformationEmitter {
    void setLocationProvider(LocationProvider locationProvider);

    void emitLocation(String fileName, int line);

    void emitStatementStart();

    void emitMethod(MethodDescriptor method);

    void emitClass(String className);

    void emitVariable(String[] sourceNames, String generatedName);

    DeferredCallSite emitCallSite();

    void addClass(String jsName, String className, String parentName);

    void addField(String fieldName, String jsName);

    void addSuccessors(SourceLocation location, SourceLocation[] successors);
}