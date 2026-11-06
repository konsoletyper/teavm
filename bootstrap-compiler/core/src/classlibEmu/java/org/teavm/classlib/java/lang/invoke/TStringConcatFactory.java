/*
 *  Copyright 2025 konsoletyper.
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

package org.teavm.classlib.java.lang.invoke;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatException;

public final class TStringConcatFactory {
    private TStringConcatFactory() {
    }

    public static CallSite makeConcat(MethodHandles.Lookup lookup, String name, MethodType concatType)
            throws StringConcatException {
        throw new UnsupportedOperationException();
    }

    public static CallSite makeConcatWithConstants(MethodHandles.Lookup lookup, String name, MethodType concatType,
            String recipe, Object... constants) throws StringConcatException {
        throw new UnsupportedOperationException();
    }
}
