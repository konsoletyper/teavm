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

import java.io.IOException;
import java.util.List;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.vm.spi.TeaVMHostExtension;

public interface TeaVMTarget {
    List<ClassHolderTransformer> getTransformers();

    List<DependencyListener> getDependencyListeners();

    void setController(TeaVMTargetController controller);

    List<TeaVMHostExtension> getHostExtensions();

    boolean requiresRegisterAllocation();

    void contributeDependencies(DependencyAnalyzer dependencyAnalyzer);

    void beforeOptimizations(Program program, MethodReader method, ListableClassReaderSource classSource);

    void afterOptimizations(Program program, MethodReader method, ListableClassReaderSource classSource);

    void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName) throws IOException;

    String[] getPlatformTags();

    boolean isAsyncSupported();
}
