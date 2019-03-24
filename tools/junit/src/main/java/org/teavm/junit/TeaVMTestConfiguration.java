/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import org.teavm.backend.c.CTarget;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMTarget;

interface TeaVMTestConfiguration<T extends TeaVMTarget> {
    String getSuffix();

    void apply(TeaVM vm);

    void apply(T target);

    TeaVMTestConfiguration<JavaScriptTarget> JS_DEFAULT = new TeaVMTestConfiguration<JavaScriptTarget>() {
        @Override
        public String getSuffix() {
            return "";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        }

        @Override
        public void apply(JavaScriptTarget target) {
            target.setMinifying(false);
        }
    };

    TeaVMTestConfiguration<JavaScriptTarget> JS_OPTIMIZED = new TeaVMTestConfiguration<JavaScriptTarget>() {
        @Override
        public String getSuffix() {
            return "optimized";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
        }

        @Override
        public void apply(JavaScriptTarget target) {
            target.setMinifying(false);
        }
    };

    TeaVMTestConfiguration<JavaScriptTarget> JS_MINIFIED = new TeaVMTestConfiguration<JavaScriptTarget>() {
        @Override
        public String getSuffix() {
            return "min";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        }

        @Override
        public void apply(JavaScriptTarget target) {
            target.setMinifying(true);
        }
    };

    TeaVMTestConfiguration<WasmTarget> WASM_DEFAULT = new TeaVMTestConfiguration<WasmTarget>() {
        @Override
        public String getSuffix() {
            return "";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        }

        @Override
        public void apply(WasmTarget target) {
            target.setMinHeapSize(32 * 1024 * 1024);
            target.setWastEmitted(true);
            target.setCEmitted(true);
            target.setDebugging(true);
        }
    };

    TeaVMTestConfiguration<WasmTarget> WASM_OPTIMIZED = new TeaVMTestConfiguration<WasmTarget>() {
        @Override
        public String getSuffix() {
            return "optimized";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
        }

        @Override
        public void apply(WasmTarget target) {
        }
    };

    TeaVMTestConfiguration<CTarget> C_DEFAULT = new TeaVMTestConfiguration<CTarget>() {
        @Override
        public String getSuffix() {
            return "";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        }

        @Override
        public void apply(CTarget target) {
        }
    };

    TeaVMTestConfiguration<CTarget> C_OPTIMIZED = new TeaVMTestConfiguration<CTarget>() {
        @Override
        public String getSuffix() {
            return "optimized";
        }

        @Override
        public void apply(TeaVM vm) {
            vm.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
        }

        @Override
        public void apply(CTarget target) {
        }
    };
}
