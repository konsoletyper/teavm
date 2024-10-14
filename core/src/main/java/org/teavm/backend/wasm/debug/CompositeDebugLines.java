/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import org.teavm.backend.wasm.debug.sourcemap.SourceMapBuilder;
import org.teavm.model.MethodReference;

public class CompositeDebugLines implements DebugLines {
    private DebugLines debugLinesBuilder;
    private SourceMapBuilder sourceMapBuilder;

    public CompositeDebugLines(DebugLines debugLinesBuilder, SourceMapBuilder sourceMapBuilder) {
        this.debugLinesBuilder = debugLinesBuilder;
        this.sourceMapBuilder = sourceMapBuilder;
    }

    @Override
    public void advance(int ptr) {
        debugLinesBuilder.advance(ptr);
        sourceMapBuilder.advance(ptr);
    }

    @Override
    public void location(String file, int line) {
        debugLinesBuilder.location(file, line);
        sourceMapBuilder.location(file, line);
    }

    @Override
    public void emptyLocation() {
        debugLinesBuilder.emptyLocation();
        sourceMapBuilder.emptyLocation();
    }

    @Override
    public void start(MethodReference methodReference) {
        debugLinesBuilder.start(methodReference);
        sourceMapBuilder.start(methodReference);
    }

    @Override
    public void end() {
        debugLinesBuilder.end();
        sourceMapBuilder.end();
    }
}
