/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.unicode;

import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;

public class DecimalDataBuilder extends ObjectResourceBuilder {
    public int groupingSeparator;
    public int decimalSeparator;
    public int listSeparator;
    public int perMille;
    public int percent;
    public String nan;
    public String infinity;
    public int minusSign;
    public String exponentSeparator;

    @Override
    public Object getValue(int index) {
        switch (index) {
            case 0:
                return groupingSeparator;
            case 1:
                return decimalSeparator;
            case 2:
                return listSeparator;
            case 3:
                return perMille;
            case 4:
                return percent;
            case 5:
                return nan;
            case 6:
                return infinity;
            case 7:
                return minusSign;
            case 8:
                return exponentSeparator;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public String[] fieldNames() {
        return new String[] {
                "groupingSeparator",
                "decimalSeparator",
                "listSeparator",
                "perMille",
                "percent",
                "naN",
                "infinity",
                "minusSign",
                "exponentSeparator"
        };
    }

    @Override
    public Class<? extends Resource> getOutputClass() {
        return DecimalData.class;
    }
}
