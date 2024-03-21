/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.classlib.java.io;

public class TInvalidClassException extends TObjectStreamException {
    public String classname;

    public TInvalidClassException(String reason) {
        super(reason);
    }

    public TInvalidClassException(String classname, String message) {
        super(message);
        this.classname = classname;
    }

    public TInvalidClassException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public TInvalidClassException(String classname, String message, Throwable cause) {
        super(message, cause);
        this.classname = classname;
    }
}
