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
package org.teavm.classlib.java.text;

import static org.junit.Assert.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class SimpleDateTimeFormatTest {
    @Test
    public void fieldsFormatted() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertEquals("2014-06-24 17:33:49", format.format(new Date(1403616829504L)));
    }
}
