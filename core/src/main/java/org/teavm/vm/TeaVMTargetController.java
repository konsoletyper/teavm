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

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import org.teavm.cache.CacheStatus;
import org.teavm.common.ServiceRepository;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassInitializerInfo;

public interface TeaVMTargetController {
    boolean wasCancelled();

    ClassLoader getClassLoader();

    ClassReaderSource getUnprocessedClassSource();

    CacheStatus getCacheStatus();

    DependencyInfo getDependencyInfo();

    Diagnostics getDiagnostics();

    Properties getProperties();

    ServiceRepository getServices();

    TeaVMOptimizationLevel getOptimizationLevel();

    boolean isFriendlyToDebugger();

    Map<? extends String, ? extends TeaVMEntryPoint> getEntryPoints();

    Set<? extends String> getPreservedClasses();

    boolean isVirtual(MethodReference method);

    TeaVMProgressFeedback reportProgress(int progress);

    void addVirtualMethods(Predicate<MethodReference> methods);

    ClassInitializerInfo getClassInitializerInfo();
}
