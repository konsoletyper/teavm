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

import java.util.Collection;
import org.teavm.model.MethodReference;

/**
 * Represents a method with information about what methods does it call and what method do call the method.
 * @author Alexey Andreev
 */
public interface CallGraphNode {
    /**
     * Returns reference to entire call graph.
     *
     * @return graph
     */
    CallGraph getGraph();

    /**
     * Returns the method that this node represents.
     *
     * @return method
     */
    MethodReference getMethod();

    /**
     * Returns immutable collection of all call sites that are in the method.
     *
     * @return call site
     */
    Collection<? extends CallSite> getCallSites();

    /**
     * Returns immutable collection of all call sites that call this method.
     *
     * @return call sites
     */
    Collection<? extends CallSite> getCallerCallSites();

    Collection<? extends FieldAccessSite> getFieldAccessSites();

    Collection<? extends ClassAccessSite> getClassAccessSites();
}
