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
package org.teavm.backend.javascript.intrinsics.eventqueue;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplate;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.model.MethodReference;
import org.teavm.runtime.EventQueue;

public class EventQueueGenerator implements Generator {
    public static final MethodReference OFFER = new MethodReference(EventQueue.class, "offerJS",
            EventQueue.Event.class, double.class, int.class);
    public static final MethodReference KILL = new MethodReference(EventQueue.class, "killJS", int.class, void.class);
    static final MethodReference RUN = new MethodReference(EventQueue.class, "run", EventQueue.Event.class,
            void.class);

    private JavaScriptTemplate template;

    public EventQueueGenerator(JavaScriptTemplateFactory templateFactory) {
        template = templateFactory.createFromResource("org/teavm/backend/javascript/intrinsics/eventQueue.js");
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "offerJS":
                template.builder("offer").withContext(context).build().write(writer, 0);
                break;
            case "killJS":
                template.builder("kill").withContext(context).build().write(writer, 0);
                break;
        }
    }
}
