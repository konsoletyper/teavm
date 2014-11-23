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
package org.teavm.callgraph;

import org.teavm.model.InstructionLocation;

/**
 *
 * @author Alexey Andreev
 */
public class DefaultClassAccessSite implements ClassAccessSite {
    private InstructionLocation location;
    private CallGraphNode callee;
    private String className;

    DefaultClassAccessSite(InstructionLocation location, CallGraphNode callee, String className) {
        this.location = location;
        this.callee = callee;
        this.className = className;
    }

    @Override
    public InstructionLocation getLocation() {
        return location;
    }

    @Override
    public CallGraphNode getCallee() {
        return callee;
    }

    @Override
    public String getClassName() {
        return className;
    }
}
