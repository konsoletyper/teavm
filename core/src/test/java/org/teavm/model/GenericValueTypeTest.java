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
package org.teavm.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GenericValueTypeTest {
    @Test
    public void innerClassErasureUsesFullName() {
        var position = new GenericValueType.ParsePosition();
        var type = GenericValueType.parse("Ljava/lang/ref/ReferenceQueue<TT;>.RemoveCallback;", position);
        var objectType = (GenericValueType.Object) type;
        assertEquals("java.lang.ref.ReferenceQueue$RemoveCallback", objectType.getFullClassName());
        assertEquals(ValueType.object("java.lang.ref.ReferenceQueue$RemoveCallback"), objectType.asValueType());
    }

    @Test
    public void topLevelErasureUnchanged() {
        var position = new GenericValueType.ParsePosition();
        var type = GenericValueType.parse("Ljava/lang/String;", position);
        assertEquals(ValueType.object("java.lang.String"), ((GenericValueType.Object) type).asValueType());
    }
}
