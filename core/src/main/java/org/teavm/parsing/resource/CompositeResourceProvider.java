/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.parsing.resource;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompositeResourceProvider implements ResourceProvider {
    private ResourceProvider[] providers;

    public CompositeResourceProvider(ResourceProvider... providers) {
        this.providers = providers;
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var iterators = Stream.of(providers)
                .map(provider -> provider.getResources(name))
                .collect(Collectors.toList());
        return new Iterator<>() {
            Iterator<Resource> current;
            int currentIndex;

            @Override
            public boolean hasNext() {
                if (current == null) {
                    return following();
                }
                return current.hasNext();
            }

            @Override
            public Resource next() {
                if (current == null || !current.hasNext()) {
                    if (!following()) {
                        throw new NoSuchElementException();
                    }
                }
                var result = current.next();
                if (!current.hasNext()) {
                    current = null;
                }
                return result;
            }

            private boolean following() {
                while (true) {
                    if (currentIndex >= iterators.size()) {
                        return false;
                    }
                    current = iterators.get(currentIndex++);
                    if (current.hasNext()) {
                        return true;
                    }
                }
            }
        };
    }

    @Override
    public void close() {
        for (var provider : providers) {
            provider.close();
        }
    }
}
