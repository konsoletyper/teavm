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

import java.io.Serializable;
import java.util.Objects;
import org.teavm.model.TextLocation;

public class DefaultCallSite implements CallSite, Serializable {
    private TextLocation location;
    private DefaultCallGraphNode callee;
    private DefaultCallGraphNode caller;

    DefaultCallSite(TextLocation location, DefaultCallGraphNode callee, DefaultCallGraphNode caller) {
        this.location = location;
        this.callee = callee;
        this.caller = caller;
    }

    @Override
    public TextLocation getLocation() {
        return location;
    }

    @Override
    public DefaultCallGraphNode getCallee() {
        return callee;
    }

    @Override
    public DefaultCallGraphNode getCaller() {
        return caller;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DefaultCallSite)) {
            return false;
        }
        DefaultCallSite other = (DefaultCallSite) obj;
        return Objects.equals(callee.getMethod(), other.callee.getMethod())
                && Objects.equals(location, other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callee.getMethod(), location);
    }
}
