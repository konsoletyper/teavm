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
package org.teavm.classlib.java.lang;

import java.util.Objects;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.util.TObjects;

/**
 *
 * @author Alexey Andreev
 */
public final class TStackTraceElement extends TObject implements TSerializable {
    private TString declaringClass;
    private TString methodName;
    private TString fileName;
    private int lineNumber;

    public TStackTraceElement(TString declaringClass, TString methodName, TString fileName, int lineNumber) {
        if (declaringClass == null || methodName == null) {
            throw new TNullPointerException();
        }
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public TString getClassName() {
        return declaringClass;
    }

    public TString getMethodName() {
        return methodName;
    }

    public TString getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isNativeMethod() {
        return fileName == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass, methodName, fileName, lineNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TStackTraceElement)) {
            return false;
        }
        TStackTraceElement other = (TStackTraceElement) obj;
        return TObjects.equals(declaringClass, other.declaringClass)
                && TObjects.equals(methodName, other.methodName)
                && TObjects.equals(fileName, other.fileName)
                && lineNumber == other.lineNumber;
    }

    @Override
    public String toString() {
        TStringBuilder sb = new TStringBuilder();
        int index = declaringClass.lastIndexOf('.');
        sb.append(declaringClass.substring(index + 1)).append('.').append(methodName).append('(');
        if (fileName != null) {
            sb.append(fileName).append(':').append(lineNumber);
        } else {
            sb.append(TString.wrap("Unknown Source"));
        }
        return sb.toString();
    }
}
