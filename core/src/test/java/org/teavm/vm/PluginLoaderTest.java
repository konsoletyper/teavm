/*
 *  Copyright 2015 Alexey Andreev.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.vm.spi.Before;
import org.teavm.vm.spi.Requires;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 *
 * @author Alexey Andreev
 */
public class PluginLoaderTest {
    @Test
    public void loadsPlugins() {
        List<String> plugins = order(A.class, B.class);
        assertThat(plugins.size(), is(2));
        assertThat(plugins.get(0), is(B.class.getName()));
        assertThat(plugins.get(1), is(A.class.getName()));
    }

    private List<String> order(Class<?>... classes) {
        return TeaVMPluginLoader.orderPlugins(PluginLoaderTest.class.getClassLoader(),
                Arrays.stream(classes).map(Class::getName).collect(Collectors.toSet()));
    }

    static class A implements TeaVMPlugin {
        @Override
        public void install(TeaVMHost host) {
        }
    }

    @Before(A.class)
    @Requires(A.class)
    static class B implements TeaVMPlugin {
        @Override
        public void install(TeaVMHost host) {
        }
    }
}
