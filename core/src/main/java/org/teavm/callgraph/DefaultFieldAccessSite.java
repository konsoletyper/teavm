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
import org.teavm.model.FieldReference;
import org.teavm.model.TextLocation;

public class DefaultFieldAccessSite implements FieldAccessSite, Serializable {
    private TextLocation location;
    private DefaultCallGraphNode callee;
    private FieldReference field;

    DefaultFieldAccessSite(TextLocation location, DefaultCallGraphNode callee, FieldReference field) {
        this.location = location;
        this.callee = callee;
        this.field = field;
    }

    @Override
    public TextLocation getLocation() {
        return null;
    }

    @Override
    public DefaultCallGraphNode getCallee() {
        return callee;
    }

    @Override
    public FieldReference getField() {
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, callee, field);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultFieldAccessSite)) {
            return false;
        }
        DefaultFieldAccessSite other = (DefaultFieldAccessSite) obj;
        return Objects.equals(location, other.location) && Objects.equals(callee, other.callee)
                && Objects.equals(field, other.field);
    }
}
