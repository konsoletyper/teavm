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
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 * <p>Root object for traversing through call graph. Graph does not represent polymorphic calls.
 * Instead, it generated multiple call sites for one location.</p>
 *
 * @author Alexey Andreev
 */
public interface CallGraph {
    /**
     * <p>Get node corresponding to the specific method.</p>
     *
     * @param method a method to lookup node for.
     * @return the node that corresponds to the specified method or <code>null</code>, if this method is never
     * called.
     */
    CallGraphNode getNode(MethodReference method);

    Collection<? extends FieldAccessSite> getFieldAccess(FieldReference reference);
}
