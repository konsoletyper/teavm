/*
 *  Copyright 2017 Alexey Andreev.
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

import java.util.Objects;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;

public class CallSiteLocation {
    private String fileName;
    private String className;
    private String methodName;
    private int lineNumber;

    public CallSiteLocation(String fileName, String className, String methodName, int lineNumber) {
        this.fileName = fileName;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CallSiteLocation)) {
            return false;
        }
        CallSiteLocation other = (CallSiteLocation) obj;
        return Objects.equals(fileName, other.fileName) && Objects.equals(className, other.className)
                && Objects.equals(methodName, other.methodName) && lineNumber == other.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, className, methodName, lineNumber);
    }

    public AnnotationReader save() {
        AnnotationHolder annotation = new AnnotationHolder(CallSiteLocationAnnot.class.getName());
        annotation.getValues().put("fileName", CallSiteDescriptor.saveNullableString(fileName));
        annotation.getValues().put("className", CallSiteDescriptor.saveNullableString(className));
        annotation.getValues().put("methodName", CallSiteDescriptor.saveNullableString(methodName));
        annotation.getValues().put("lineNumber", new AnnotationValue(lineNumber));
        return annotation;
    }

    public static CallSiteLocation load(AnnotationReader reader) {
        return new CallSiteLocation(
                CallSiteDescriptor.loadNullableString(reader.getValue("fileName")),
                CallSiteDescriptor.loadNullableString(reader.getValue("className")),
                CallSiteDescriptor.loadNullableString(reader.getValue("methodName")),
                reader.getValue("lineNumber").getInt());
    }
}
