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
package org.teavm.model.lowlevel;

import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;

public class ExceptionHandlerDescriptor {
    private int id;
    private String className;

    public ExceptionHandlerDescriptor(int id, String className) {
        this.id = id;
        this.className = className;
    }

    public int getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public AnnotationReader save() {
        AnnotationHolder annot = new AnnotationHolder(ExceptionHandlerDescriptorAnnot.class.getName());
        annot.getValues().put("id", new AnnotationValue(id));
        annot.getValues().put("className", CallSiteDescriptor.saveNullableString(className));
        return annot;
    }

    public static ExceptionHandlerDescriptor load(AnnotationReader annot) {
        return new ExceptionHandlerDescriptor(
                annot.getValue("id").getInt(),
                CallSiteDescriptor.loadNullableString(annot.getValue("className")));
    }
}
