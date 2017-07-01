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
package org.teavm.classlib.impl.unicode;

public class CLDRDecimalData {
    int groupingSeparator;
    int decimalSeparator;
    int listSeparator;
    int perMille;
    int percent;
    String nan;
    String infinity;
    int minusSign;
    int monetaryDecimalSeparator;
    String exponentSeparator;

    public int getGroupingSeparator() {
        return groupingSeparator;
    }

    public int getDecimalSeparator() {
        return decimalSeparator;
    }

    public int getListSeparator() {
        return listSeparator;
    }

    public int getPerMille() {
        return perMille;
    }

    public int getPercent() {
        return percent;
    }

    public String getNaN() {
        return nan;
    }

    public String getInfinity() {
        return infinity;
    }

    public int getMinusSign() {
        return minusSign;
    }

    public int getMonetaryDecimalSeparator() {
        return monetaryDecimalSeparator;
    }

    public String getExponentSeparator() {
        return exponentSeparator;
    }
}
