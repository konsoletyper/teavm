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
package org.teavm.classlib.impl.currency;

import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;

public class CurrencyResourceBuilder extends ObjectResourceBuilder {
    public String code;
    public int numericCode;
    public int fractionDigits;

    @Override
    public Object getValue(int index) {
        switch (index) {
            case 0:
                return code;
            case 1:
                return numericCode;
            case 2:
                return fractionDigits;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public String[] fieldNames() {
        return new String[] { "code", "numericCode", "fractionDigits" };
    }

    @Override
    public Class<? extends Resource> getOutputClass() {
        return CurrencyResource.class;
    }
}
