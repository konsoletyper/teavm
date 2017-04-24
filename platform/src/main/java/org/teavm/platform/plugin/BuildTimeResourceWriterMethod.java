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
package org.teavm.platform.plugin;

import org.teavm.backend.javascript.codegen.SourceWriter;

class BuildTimeResourceWriterMethod implements BuildTimeResourceMethod {
    private String[] propertyNames;

    public BuildTimeResourceWriterMethod(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    @Override
    public Object invoke(BuildTimeResourceProxy proxy, Object[] args) throws Throwable {
        SourceWriter writer = (SourceWriter) args[0];
        writer.append('{');
        for (int i = 0; i < propertyNames.length; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            ResourceWriterHelper.writeIdentifier(writer, propertyNames[i]);
            writer.ws().append(':').ws();
            ResourceWriterHelper.write(writer, proxy.data[i]);
        }
        writer.append('}').tokenBoundary();
        return null;
    }
}
