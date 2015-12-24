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
package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TString;


public interface TDataOutput {
    void write(int b) throws TIOException;

    void write(byte[] b) throws TIOException;

    void write(byte[] b, int off, int len) throws TIOException;

    void writeBoolean(boolean v) throws TIOException;

    void writeByte(int v) throws TIOException;

    void writeShort(int v) throws TIOException;

    void writeChar(int v) throws TIOException;

    void writeInt(int v) throws TIOException;

    void writeLong(long v) throws TIOException;

    void writeFloat(float v) throws TIOException;

    void writeDouble(double v) throws TIOException;

    void writeBytes(TString s) throws TIOException;

    void writeChars(TString s) throws TIOException;

    void writeUTF(TString s) throws TIOException;
}
