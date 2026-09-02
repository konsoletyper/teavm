/*
 *  Copyright 2026 thesupersupersigma.
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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DefaultAliasProviderTest {
    @Test
    public void reservedWordClassNamesAreEscaped() {
        var provider = new DefaultAliasProvider(10000);
        assertEquals("in1", provider.getClassAlias("in").name);
        assertEquals("with1", provider.getClassAlias("with").name);
        assertEquals("do1", provider.getClassAlias("do").name);
    }

    @Test
    public void ordinaryClassNamesAreUntouched() {
        var provider = new DefaultAliasProvider(10000);
        assertEquals("Main", provider.getClassAlias("Main").name);
        assertEquals("jl_String", provider.getClassAlias("java.lang.String").name);
    }
}
