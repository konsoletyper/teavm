/*
 *  Copyright 2013 Alexey Andreev.
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

class Transition {
    private DependencyNode source;
    private DependencyNode destination;
    private byte destSubsetOfSrc;
    boolean fresh;

    Transition(DependencyNode source, DependencyNode destination) {
        this.source = source;
        this.destination = destination;
    }

    DependencyNode getSource() {
        return source;
    }

    DependencyNode getDestination() {
        return destination;
    }

    boolean isDestSubsetOfSrc() {
        if (destSubsetOfSrc == 0) {
            if (destination.typeFilter == null) {
                destSubsetOfSrc = 2;
            } else if (source.typeFilter == null) {
                destSubsetOfSrc = 1;
            } else {
                destSubsetOfSrc = source.dependencyAnalyzer.getClassHierarchy().isSuperType(
                        source.typeFilter, destination.typeFilter, false) ? (byte) 1 : 2;
            }
        }
        return destSubsetOfSrc == 1;
    }
}
