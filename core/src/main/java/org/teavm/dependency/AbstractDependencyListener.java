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
package org.teavm.dependency;

import org.teavm.model.CallLocation;

public abstract class AbstractDependencyListener implements DependencyListener {
    @Override
    public void started(DependencyAgent agent) {
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
    }

    @Override
    public void fieldReached(DependencyAgent agent, FieldDependency field, CallLocation location) {
    }

    @Override
    public void completing(DependencyAgent agent) {
    }
}
