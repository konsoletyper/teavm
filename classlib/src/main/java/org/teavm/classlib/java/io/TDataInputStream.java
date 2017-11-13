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

import org.teavm.classlib.java.lang.*;

public class TDataInputStream extends TFilterInputStream implements TDataInput {
    byte[] buff;

    public TDataInputStream(TInputStream in) {
        super(in);
        buff = new byte[8];
    }

    @Override
    public final int read(byte[] buffer) throws TIOException {
        return in.read(buffer, 0, buffer.length);
    }

    @Override
    public final int read(byte[] buffer, int offset, int length) throws TIOException {
        return in.read(buffer, offset, length);
    }

    @Override
    public final boolean readBoolean() throws TIOException {
        int temp = in.read();
        if (temp < 0) {
            throw new TEOFException();
        }
        return temp != 0;
    }

    @Override
    public final byte readByte() throws TIOException {
        int temp = in.read();
        if (temp < 0) {
            throw new TEOFException();
        }
        return (byte) temp;
    }

    private int readToBuff(int count) throws TIOException {
        int offset = 0;
        while (offset < count) {
            int bytesRead = in.read(buff, offset, count - offset);
            if (bytesRead == -1) {
                return bytesRead;
            }
            offset += bytesRead;
        }
        return offset;
    }

    @Override
    public final char readChar() throws TIOException {
        if (readToBuff(2) < 0) {
            throw new TEOFException();
        }
        return (char) (((buff[0] & 0xff) << 8) | (buff[1] & 0xff));
    }

    @Override
    public final double readDouble() throws TIOException {
        return TDouble.longBitsToDouble(readLong());
    }

    @Override
    public final float readFloat() throws TIOException {
        return TFloat.intBitsToFloat(readInt());
    }

    @Override
    public final void readFully(byte[] buffer) throws TIOException {
        readFully(buffer, 0, buffer.length);
    }

    @Override
    public final void readFully(byte[] buffer, int offset, int length) throws TIOException {
        if (length < 0) {
            throw new TIndexOutOfBoundsException();
        }
        if (length == 0) {
            return;
        }
        if (in == null) {
            throw new TNullPointerException();
        }
        if (buffer == null) {
            throw new TNullPointerException();
        }
        if (offset < 0 || offset > buffer.length - length) {
            throw new TIndexOutOfBoundsException();
        }
        while (length > 0) {
            int result = in.read(buffer, offset, length);
            if (result < 0) {
                throw new TEOFException();
            }
            offset += result;
            length -= result;
        }
    }

    @Override
    public final int readInt() throws TIOException {
        if (readToBuff(4) < 0) {
            throw new TEOFException();
        }
        return ((buff[0] & 0xff) << 24) | ((buff[1] & 0xff) << 16) | ((buff[2] & 0xff) << 8) | (buff[3] & 0xff);
    }

    @Override
    @Deprecated
    public final TString readLine() throws TIOException {
        TStringBuilder line = new TStringBuilder(80);
        boolean foundTerminator = false;
        while (true) {
            int nextByte = in.read();
            switch (nextByte) {
                case -1:
                    if (line.length() == 0 && !foundTerminator) {
                        return null;
                    }
                    return TString.wrap(line.toString());
                case (byte) '\r':
                    if (foundTerminator) {
                        ((TPushbackInputStream) in).unread(nextByte);
                        return TString.wrap(line.toString());
                    }
                    foundTerminator = true;
                    /* Have to be able to peek ahead one byte */
                    if (!(in.getClass() == TPushbackInputStream.class)) {
                        in = new TPushbackInputStream(in);
                    }
                    break;
                case (byte) '\n':
                    return TString.wrap(line.toString());
                default:
                    if (foundTerminator) {
                        ((TPushbackInputStream) in).unread(nextByte);
                        return TString.wrap(line.toString());
                    }
                    line.append((char) nextByte);
            }
        }
    }

    @Override
    public final long readLong() throws TIOException {
        if (readToBuff(8) < 0) {
            throw new TEOFException();
        }
        int i1 = ((buff[0] & 0xff) << 24) | ((buff[1] & 0xff) << 16)
                | ((buff[2] & 0xff) << 8) | (buff[3] & 0xff);
        int i2 = ((buff[4] & 0xff) << 24) | ((buff[5] & 0xff) << 16)
                | ((buff[6] & 0xff) << 8) | (buff[7] & 0xff);
        return ((i1 & 0xffffffffL) << 32) | (i2 & 0xffffffffL);
    }

    @Override
    public final short readShort() throws TIOException {
        if (readToBuff(2) < 0) {
            throw new TEOFException();
        }
        return (short) ((((buff[0] & 0xff) << 24) >> 16) | (buff[1] & 0xff));
    }

    @Override
    public final int readUnsignedByte() throws TIOException {
        int temp = in.read();
        if (temp < 0) {
            throw new TEOFException();
        }
        return temp & 0xFF;
    }

    @Override
    public final int readUnsignedShort() throws TIOException {
        if (readToBuff(2) < 0) {
            throw new TEOFException();
        }
        return (char) (((buff[0] & 0xff) << 8) | (buff[1] & 0xff));
    }

    @Override
    public final TString readUTF() throws TIOException {
        return decodeUTF(readUnsignedShort());
    }

    TString decodeUTF(int utfSize) throws TIOException {
        return decodeUTF(utfSize, this);
    }

    private static TString decodeUTF(int utfSize, TDataInput in) throws TIOException {
        byte[] buf = new byte[utfSize];
        char[] out = new char[utfSize];
        in.readFully(buf, 0, utfSize);

        return convertUTF8WithBuf(buf, out, 0, utfSize);
    }

    public static final TString readUTF(TDataInput in) throws TIOException {
        return decodeUTF(in.readUnsignedShort(), in);
    }

    @Override
    public final int skipBytes(int count) throws TIOException {
        int skipped = 0;

        while (skipped < count) {
            long skip = in.skip(count - skipped);
            if (skip == 0) {
                break;
            }
            skipped += skip;
        }
        if (skipped < 0) {
            throw new TEOFException();
        }
        return skipped;
    }

    private static TString convertUTF8WithBuf(byte[] buf, char[] out, int offset, int utfSize)
            throws TUTFDataFormatException {
        int count = 0;
        int s = 0;
        int a;
        while (count < utfSize) {
            char ch = (char) buf[offset + count++];
            out[s] = ch;
            a = out[s];
            if (ch < '\u0080') {
                s++;
            } else if ((a & 0xe0) == 0xc0) {
                if (count >= utfSize) {
                    throw new TUTFDataFormatException(TString.wrap("End of stream reached"));
                }
                int b = buf[offset + count++];
                if ((b & 0xC0) != 0x80) {
                    throw new TUTFDataFormatException(TString.wrap("Malformed UTF-8 sequence"));
                }
                out[s++] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
            } else if ((a & 0xf0) == 0xe0) {
                if (count + 1 >= utfSize) {
                    throw new TUTFDataFormatException(TString.wrap("Malformed UTF-8 sequence"));
                }
                int b = buf[offset + count++];
                int c = buf[offset + count++];
                if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
                    throw new TUTFDataFormatException(TString.wrap("Malformed UTF-8 sequence"));
                }
                out[s++] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
            } else {
                throw new TUTFDataFormatException(TString.wrap("Malformed UTF-8 sequence"));
            }
        }
        return new TString(out, 0, s);
    }
}
