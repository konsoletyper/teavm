/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.vm.intrinsic;

import java.util.Properties;
import java.util.Set;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.vm.TeaVMTargetController;

public class BaseIntrinsicContributorContext implements IntrinsicContributorContext {
    private TeaVMTargetController controller;
    private ClassHierarchy hierarchy;
    private ListableClassReaderSource classes;
    private Set<MethodReference> asyncSplitMethods;

    public BaseIntrinsicContributorContext(TeaVMTargetController controller, ClassHierarchy hierarchy,
            ListableClassReaderSource classes, Set<MethodReference> asyncSplitMethods) {
        this.controller = controller;
        this.hierarchy = hierarchy;
        this.classes = classes;
        this.asyncSplitMethods = asyncSplitMethods;
    }

    @Override
    public ClassHierarchy hierarchy() {
        return hierarchy;
    }

    @Override
    public ListableClassReaderSource classes() {
        return classes;
    }

    @Override
    public ClassLoader classLoader() {
        return controller.getClassLoader();
    }

    @Override
    public ResourceProvider resources() {
        return controller.getResourceProvider();
    }

    @Override
    public String entryPoint() {
        return controller.getEntryPoint();
    }

    @Override
    public Diagnostics diagnostics() {
        return controller.getDiagnostics();
    }

    @Override
    public ClassInitializerInfo classInitInfo() {
        return controller.getClassInitializerInfo();
    }

    @Override
    public DependencyInfo dependency() {
        return controller.getDependencyInfo();
    }

    @Override
    public boolean isAsyncMethod(MethodReference method) {
        return asyncSplitMethods.contains(method);
    }

    @Override
    public Properties properties() {
        return controller.getProperties();
    }
}
