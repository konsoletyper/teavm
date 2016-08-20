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

import org.teavm.model.TextLocation;

/**
 * <p>Call site that represents exact place in the code that calls a method.</p>.
 * @author Alexey Andreev
 */
public interface CallSite {
    /**
     * <p>Gets location of the call site</p>.
     *
     * @return location of the call site or <code>null</code> if no debug information found for this call site.
     */
    TextLocation getLocation();

    /**
     * <p>Gets a method that this call site invokes.</p>
     *
     * @return a node that represent methods being called
     */
    CallGraphNode getCallee();

    /**
     * <p>Gets a method that contains this call site.</p>
     *
     * @return a node that represents methods's caller
     */
    CallGraphNode getCaller();
}
