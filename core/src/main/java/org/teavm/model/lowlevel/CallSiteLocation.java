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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.model.InliningInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;

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

    public static CallSiteLocation[] fromTextLocation(TextLocation location, MethodReference callingMethod) {
        if (location == null) {
            return new CallSiteLocation[] {
                    new CallSiteLocation("", callingMethod.getClassName(), callingMethod.getName(), -1)
            };
        } else if (location.getInlining() == null) {
            return new CallSiteLocation[] {
                    new CallSiteLocation(
                            convertFileName(location.getFileName()),
                            callingMethod.getClassName(),
                            callingMethod.getName(),
                            location.getLine())
            };
        } else {
            List<CallSiteLocation> result = new ArrayList<>();
            InliningInfo inlining = location.getInlining();
            result.add(new CallSiteLocation(
                    convertFileName(location.getFileName()),
                    inlining.getMethod().getClassName(),
                    inlining.getMethod().getName(),
                    location.getLine()));
            while (inlining != null) {
                MethodReference method = inlining.getParent() != null
                        ? inlining.getParent().getMethod()
                        : callingMethod;
                result.add(new CallSiteLocation(
                        convertFileName(inlining.getFileName()),
                        method.getClassName(),
                        method.getName(),
                        inlining.getLine()));
                inlining = inlining.getParent();
            }
            return result.toArray(new CallSiteLocation[0]);
        }
    }

    private static String convertFileName(String fileName) {
        if (fileName != null) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        return fileName;
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
}
