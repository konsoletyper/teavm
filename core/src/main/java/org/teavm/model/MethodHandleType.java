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
package org.teavm.model;

public enum MethodHandleType {
    GET_FIELD(1),
    GET_STATIC_FIELD(2),
    PUT_FIELD(3),
    PUT_STATIC_FIELD(4),
    INVOKE_VIRTUAL(5),
    INVOKE_STATIC(6),
    INVOKE_SPECIAL(7),
    INVOKE_CONSTRUCTOR(8),
    INVOKE_INTERFACE(9),
    ;
    private final int referenceKind;

    MethodHandleType(int referenceKind) {
        this.referenceKind = referenceKind;
    }

    public int getReferenceKind() {
        return referenceKind;
    }
}
