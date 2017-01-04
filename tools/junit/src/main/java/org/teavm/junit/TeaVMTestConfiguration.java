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

import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMOptimizationLevel;

interface TeaVMTestConfiguration {
    String getSuffix();

    void apply(TeaVM vm);

    void apply(JavaScriptTarget target);

    TeaVMTestConfiguration DEFAULT = new TeaVMTestConfiguration() {
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

    TeaVMTestConfiguration OPTIMIZED = new TeaVMTestConfiguration() {
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

    TeaVMTestConfiguration MINIFIED = new TeaVMTestConfiguration() {
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
}
