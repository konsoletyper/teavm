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
import org.teavm.classlib.java.lang.*;

public class TDataOutputStream extends TFilterOutputStream implements TDataOutput {
    /**
     * The number of bytes written out so far.
     */
    protected int written;
    byte[] buff;

    public TDataOutputStream(TOutputStream out) {
        super(out);
        buff = new byte[8];
    }

    @Override
    public void flush() throws TIOException {
        super.flush();
    }

    public final int size() {
        if (written < 0) {
            written = TInteger.MAX_VALUE;
        }
        return written;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws TIOException {
        if (buffer == null) {
            throw new TNullPointerException();
        }
        out.write(buffer, offset, count);
        written += count;
    }

    @Override
    public void write(int oneByte) throws TIOException {
        out.write(oneByte);
        written++;
    }

    @Override
    public final void writeBoolean(boolean val) throws TIOException {
        out.write(val ? 1 : 0);
        written++;
    }

    @Override
    public final void writeByte(int val) throws TIOException {
        out.write(val);
        written++;
    }

    @Override
    public final void writeBytes(TString str) throws TIOException {
        if (str.length() == 0) {
            return;
        }
        byte[] bytes = new byte[str.length()];
        for (int index = 0; index < str.length(); index++) {
            bytes[index] = (byte) str.charAt(index);
        }
        out.write(bytes);
        written += bytes.length;
    }

    @Override
    public final void writeChar(int val) throws TIOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    @Override
    public final void writeChars(TString str) throws TIOException {
        byte[] newBytes = new byte[str.length() * 2];
        for (int index = 0; index < str.length(); index++) {
            int newIndex = index == 0 ? index : index * 2;
            newBytes[newIndex] = (byte) (str.charAt(index) >> 8);
            newBytes[newIndex + 1] = (byte) str.charAt(index);
        }
        out.write(newBytes);
        written += newBytes.length;
    }

    @Override
    public final void writeDouble(double val) throws TIOException {
        writeLong(TDouble.doubleToLongBits(val));
    }

    @Override
    public final void writeFloat(float val) throws TIOException {
        writeInt(TFloat.floatToIntBits(val));
    }

    @Override
    public final void writeInt(int val) throws TIOException {
        buff[0] = (byte) (val >> 24);
        buff[1] = (byte) (val >> 16);
        buff[2] = (byte) (val >> 8);
        buff[3] = (byte) val;
        out.write(buff, 0, 4);
        written += 4;
    }

    @Override
    public final void writeLong(long val) throws TIOException {
        buff[0] = (byte) (val >> 56);
        buff[1] = (byte) (val >> 48);
        buff[2] = (byte) (val >> 40);
        buff[3] = (byte) (val >> 32);
        buff[4] = (byte) (val >> 24);
        buff[5] = (byte) (val >> 16);
        buff[6] = (byte) (val >> 8);
        buff[7] = (byte) val;
        out.write(buff, 0, 8);
        written += 8;
    }

    int writeLongToBuffer(long val, byte[] buffer, int offset) throws TIOException {
        buffer[offset++] = (byte) (val >> 56);
        buffer[offset++] = (byte) (val >> 48);
        buffer[offset++] = (byte) (val >> 40);
        buffer[offset++] = (byte) (val >> 32);
        buffer[offset++] = (byte) (val >> 24);
        buffer[offset++] = (byte) (val >> 16);
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    @Override
    public final void writeShort(int val) throws TIOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    int writeShortToBuffer(int val, byte[] buffer, int offset) throws TIOException {
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    @Override
    public final void writeUTF(TString str) throws TIOException {
        long utfCount = countUTFBytes(str);
        if (utfCount > 65535) {
            throw new TIOException(TString.wrap("UTF Error"));
        }
        byte[] buffer = new byte[(int) utfCount + 2];
        int offset = 0;
        offset = writeShortToBuffer((int) utfCount, buffer, offset);
        offset = writeUTFBytesToBuffer(str, buffer, offset);
        write(buffer, 0, offset);
    }

    long countUTFBytes(TString str) {
        int utfCount = 0;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            int charValue = str.charAt(i);
            if (charValue > 0 && charValue <= 127) {
                utfCount++;
            } else if (charValue <= 2047) {
                utfCount += 2;
            } else {
                utfCount += 3;
            }
        }
        return utfCount;
    }

    int writeUTFBytesToBuffer(TString str, byte[] buffer, int offset) throws TIOException {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            int charValue = str.charAt(i);
            if (charValue > 0 && charValue <= 127) {
                buffer[offset++] = (byte) charValue;
            } else if (charValue <= 2047) {
                buffer[offset++] = (byte) (0xc0 | (0x1f & (charValue >> 6)));
                buffer[offset++] = (byte) (0x80 | (0x3f & charValue));
            } else {
                buffer[offset++] = (byte) (0xe0 | (0x0f & (charValue >> 12)));
                buffer[offset++] = (byte) (0x80 | (0x3f & (charValue >> 6)));
                buffer[offset++] = (byte) (0x80 | (0x3f & charValue));
             }
        }
        return offset;
    }
}
