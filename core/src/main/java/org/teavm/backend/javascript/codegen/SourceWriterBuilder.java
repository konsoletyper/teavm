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
package org.teavm.backend.javascript.codegen;

public class SourceWriterBuilder {
    private NamingStrategy naming;
    private boolean minified;
    private int lineWidth = 512;

    public SourceWriterBuilder(NamingStrategy naming) {
        this.naming = naming;
    }

    public boolean isMinified() {
        return minified;
    }

    public void setMinified(boolean minified) {
        this.minified = minified;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public SourceWriter build(Appendable innerWriter) {
        SourceWriter writer = new SourceWriter(naming, innerWriter, lineWidth);
        writer.setMinified(minified);
        return writer;
    }
}
