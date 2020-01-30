/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.migration;

import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link LineMigrator} that composes multiple {@link LineMigrator} sequentially.
 */
public class ComposedLineMigrator implements LineMigrator {

    private final Collection<LineMigrator> children;

    /**
     * The constructor.
     *
     * @param children the {@link LineMigrator}s to compose.
     */
    public ComposedLineMigrator(Collection<LineMigrator> children) {

        super();
        this.children = children;
    }

    @Override
    public String migrate(String line) {

        String result = line;
        for (LineMigrator child : this.children) {
            if (result == null) {
                return null;
            }
            result = child.migrate(result);
        }
        return result;
    }

    @Override
    public void collect(Collection<LineMigrator> migrators) {

        for (LineMigrator child : this.children) {
            child.collect(migrators);
        }
    }

    @Override
    public LineMigrator append(LineMigrator... migrators) {

        Collection<LineMigrator> all = new ArrayList<>();
        for (LineMigrator child : this.children) {
            child.collect(all);
        }
        for (LineMigrator child : migrators) {
            if (child != null) {
                child.collect(all);
            }
        }
        return new ComposedLineMigrator(all);
    }

}
