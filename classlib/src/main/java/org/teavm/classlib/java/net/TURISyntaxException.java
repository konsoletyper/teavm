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
import org.teavm.classlib.java.lang.TString;
import org.teavm.interop.Rename;

/**
 * A {@code URISyntaxException} will be thrown if some information could not be parsed
 * while creating a URI.
 */
public class TURISyntaxException extends TException {

    private static final long serialVersionUID = 2137979680897488891L;

    private TString input;

    private int index;

    /**
     * Constructs a new {@code URISyntaxException} instance containing the
     * string that caused the exception, a description of the problem and the
     * index at which the error occurred.
     *
     * @param input
     *            the string that caused the exception.
     * @param reason
     *            the reason why the exception occurred.
     * @param index
     *            the position where the exception occurred.
     * @throws NullPointerException
     *             if one of the arguments {@code input} or {@code reason} is
     *             {@code null}.
     * @throws IllegalArgumentException
     *             if the value for {@code index} is lesser than {@code -1}.
     */
    public TURISyntaxException(TString input, TString reason, int index) {
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

    /**
     * Constructs a new {@code URISyntaxException} instance containing the
     * string that caused the exception and a description of the problem.
     *
     *@param input
     *            the string that caused the exception.
     * @param reason
     *            the reason why the exception occurred.
     * @throws NullPointerException
     *             if one of the arguments {@code input} or {@code reason} is
     *             {@code null}.
     */
    public TURISyntaxException(TString input, TString reason) {
        super(reason);

        if (input == null || reason == null) {
            throw new TNullPointerException();
        }

        this.input = input;
        index = -1;
    }

    /**
     * Gets the index at which the syntax error was found or {@code -1} if the
     * index is unknown/unavailable.
     *
     * @return the index of the syntax error.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets a description of the syntax error.
     *
     * @return the string describing the syntax error.
     */
    public TString getReason() {
        return TString.wrap(super.getMessage());
    }

    /**
     * Gets the initial string that contains an invalid syntax.
     *
     * @return the string that caused the exception.
     */
    public TString getInput() {
        return input;
    }

    /**
     * Gets a description of the exception, including the reason, the string
     * that caused the syntax error and the position of the syntax error if
     * available.
     *
     * @return a sting containing information about the exception.
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    @Rename("getMessage")
    public TString getMessage0() {
        return TString.wrap("");
    }
}
