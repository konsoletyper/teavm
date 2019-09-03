/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.generators;

import java.lang.ref.ReferenceQueue;
import org.teavm.model.MethodReference;

public class ReferenceQueueGenerator implements Generator {
    private static final MethodReference OBJECT_INIT = new MethodReference(
            Object.class, "<init>", void.class);

    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(ReferenceQueue.class.getName());
    }

    @Override
    public void generate(GeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "<init>":
                context.importMethod(OBJECT_INIT, false);
                context.writer().print(context.names().forMethod(OBJECT_INIT)).print("(")
                        .print(context.parameterName(0))
                        .println(");");
                break;

            case "poll":
                context.writer().print("return teavm_reference_poll(");
                context.writer().print("(TeaVM_ReferenceQueue*) ").print(context.parameterName(0));
                context.writer().println(");");
                break;
        }
    }
}
