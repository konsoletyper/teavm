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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.platform.PlatformObject;
import org.teavm.platform.PlatformQueue;

public class PlatformQueueGenerator implements Injector, DependencyPlugin {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        MethodDependency addMethod = agent.linkMethod(new MethodReference(PlatformQueue.class, "wrap",
                Object.class, PlatformObject.class), null);
        addMethod.getVariable(1).connect(method.getResult());
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        context.writeExpr(context.getArgument(0));
    }
}
