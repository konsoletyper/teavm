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
package org.teavm.vm;

import java.io.OutputStream;
import java.util.List;
import org.teavm.dependency.DependencyChecker;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.vm.spi.TeaVMHostExtension;

public interface TeaVMTarget {
    void setController(TeaVMTargetController controller);

    List<TeaVMHostExtension> getHostExtensions();

    boolean requiresRegisterAllocation();

    void contributeDependencies(DependencyChecker dependencyChecker);

    void emit(ListableClassHolderSource classes, OutputStream output, BuildTarget buildTarget);
}
