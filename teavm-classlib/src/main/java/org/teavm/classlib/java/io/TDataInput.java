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
package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TString;

/**
 *
 * @author Alexey Andreev
 */
public interface TDataInput {
    void readFully(byte[] b) throws TIOException;

    void readFully(byte[] b, int off, int len) throws TIOException;

    int skipBytes(int n) throws TIOException;

    boolean readBoolean() throws TIOException;

    byte readByte() throws TIOException;

    int readUnsignedByte() throws TIOException;

    short readShort() throws TIOException;

    int readUnsignedShort() throws TIOException;

    char readChar() throws TIOException;

    int readInt() throws TIOException;

    long readLong() throws TIOException;

    float readFloat() throws TIOException;

    double readDouble() throws TIOException;

    TString readLine() throws TIOException;

    TString readUTF() throws TIOException;
}
