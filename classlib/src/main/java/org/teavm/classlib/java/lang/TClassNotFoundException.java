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
package org.teavm.classlib.java.lang;

public class TClassNotFoundException extends TReflectiveOperationException {
    private static final long serialVersionUID = -1162632869775788325L;

    public TClassNotFoundException() {
        super();
    }

    public TClassNotFoundException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TClassNotFoundException(TString message) {
        super(message);
    }

    public TThrowable getException() {
        return getCause();
    }
}
