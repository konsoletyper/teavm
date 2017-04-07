/*
 *  Copyright 2016 Liraz.
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
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

/**
 * https://html.spec.whatwg.org/#validitystate
 */
public interface ValidityState {

    @JSProperty
    boolean isValueMissing();

    @JSProperty
    boolean isTypeMismatch();

    @JSProperty
    boolean isPatternMismatch();

    @JSProperty
    boolean isTooLong();

    @JSProperty
    boolean isTooShort();

    @JSProperty
    boolean isRangeUnderflow();

    @JSProperty
    boolean isRangeOverflow();

    @JSProperty
    boolean isStepMismatch();

    @JSProperty
    boolean isBadInput();

    @JSProperty
    boolean isCustomError();

    @JSProperty
    boolean isValid();
}
