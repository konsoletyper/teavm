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
package org.teavm.classlib.java.net;

import org.teavm.classlib.java.lang.TException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.interop.Rename;

public class TURISyntaxException extends TException {
    private String input;
    private int index;

    public TURISyntaxException(String input, String reason, int index) {
        super(reason);

        if (input == null || reason == null) {
            throw new TNullPointerException();
        }

        if (index < -1) {
            throw new TIllegalArgumentException();
        }

        this.input = input;
        this.index = index;
    }

    public TURISyntaxException(String input, String reason) {
        super(reason);

        if (input == null || reason == null) {
            throw new TNullPointerException();
        }

        this.input = input;
        index = -1;
    }

    public int getIndex() {
        return index;
    }

    public String getReason() {
        return super.getMessage();
    }

    public String getInput() {
        return input;
    }

    @Override
    @Rename("getMessage")
    public String getMessage0() {
        return "";
    }
}
