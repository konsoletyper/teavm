/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.EventObject;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class EventObjectTest {
    // EventObject(Object source) Javadoc: "Throws: IllegalArgumentException - if source is null"
    @Test
    public void nullSourceRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EventObject(null));
        assertThrows(IllegalArgumentException.class, () -> new SubEvent(null));
    }

    // getSource() Javadoc: "Returns: the object on which the Event initially occurred"
    @Test
    public void getSourceReturnsConstructorArgument() {
        Object[] sources = { new Object(), "", "src", 0, new int[0], new EventObject("inner") };
        for (Object source : sources) {
            assertSame(source, new EventObject(source).getSource());
            assertSame(source, new SubEvent(source).getSource());
        }
    }

    // Field summary: "protected Object source - The object on which the Event initially occurred."
    // The field is protected and not final, so subclasses can both read and assign it.
    @Test
    public void subclassReadsSourceField() {
        var source = new Object();
        var event = new SubEvent(source);
        assertSame(source, event.sourceField());
    }

    @Test
    public void subclassAssignsSourceField() {
        var event = new SubEvent("first");
        var replacement = new Object();
        event.setSourceField(replacement);
        assertSame(replacement, event.getSource());
        event.setSourceField(null);
        assertNull(event.getSource());
    }

    // toString() Javadoc: "Returns a String representation of this EventObject." The format is
    // unspecified, so only check that one is returned.
    @Test
    public void toStringReturnsString() {
        assertNotNull(new EventObject("src").toString());
    }

    static class SubEvent extends EventObject {
        SubEvent(Object source) {
            super(source);
        }

        Object sourceField() {
            return source;
        }

        void setSourceField(Object source) {
            this.source = source;
        }
    }
}
