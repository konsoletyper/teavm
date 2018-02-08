/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.vm.spi;

import java.util.Properties;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

/**
 * <p>A host of plugins for TeaVM. Plugins are provided with this interface
 * in order to give them ability to extend TeaVM.</p>
 *
 * @author Alexey Andreev
 */
public interface TeaVMHost {
    void add(DependencyListener dependencyListener);

    void add(ClassHolderTransformer classTransformer);

    void add(MethodReference methodRef, BootstrapMethodSubstitutor substitutor);

    void add(MethodReference methodRef, DependencyPlugin dependencyPlugin);

    <T extends TeaVMHostExtension> T getExtension(Class<T> extensionType);

    <T> void registerService(Class<T> type, T instance);

    /**
     * Gets class loaded that is used by TeaVM. This class loader is usually specified by
     * {@link TeaVMBuilder#setClassLoader(ClassLoader)}
     * @return class loader that can be used by plugins.
     */
    ClassLoader getClassLoader();

    /**
     * Gets configuration properties. These properties are usually specified by
     * {@link TeaVM#setProperties(Properties)}.
     *
     * @return a copy of all of the VM properties. Any further changes to returned objects will not be
     * visible to VM.
     */
    Properties getProperties();

    String[] getPlatformTags();
}
