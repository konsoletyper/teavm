/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.io;
import org.teavm.classlib.java.lang.*;

/**
 * A data output stream lets an application write primitive Java data types to an output stream in a portable way. An application can then use a data input stream to read the data back in.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInputStream
 */
public class TDataOutputStream extends TFilterOutputStream implements TDataOutput{
    /**
     * The number of bytes written out so far.
     */
    protected int written;
    byte buff[];

    /**
     * Constructs a new {@code DataOutputStream} on the {@code OutputStream}
     * {@code out}. Note that data written by this stream is not in a human
     * readable form but can be reconstructed by using a {@link DataInputStream}
     * on the resulting output.
     *
     * @param out
     *            the target stream for writing.
     */
    public TDataOutputStream(TOutputStream out) {
        super(out);
        buff = new byte[8];
    }

    /**
     * Flushes this stream to ensure all pending data is sent out to the target
     * stream. This implementation then also flushes the target stream.
     *
     * @throws IOException
     *             if an error occurs attempting to flush this stream.
     */
    @Override
    public void flush() throws TIOException {
        super.flush();
    }

    /**
     * Returns the total number of bytes written to the target stream so far.
     *
     * @return the number of bytes written to the target stream.
     */
    public final int size() {
        if (written < 0) {
            written = TInteger.MAX_VALUE;
        }
        return written;
    }

    /**
     * Writes {@code count} bytes from the byte array {@code buffer} starting at
     * {@code offset} to the target stream.
     *
     * @param buffer
     *            the buffer to write to the target stream.
     * @param offset
     *            the index of the first byte in {@code buffer} to write.
     * @param count
     *            the number of bytes from the {@code buffer} to write.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @throws NullPointerException
     *             if {@code buffer} is {@code null}.
     * @see DataInputStream#readFully(byte[])
     * @see DataInputStream#readFully(byte[], int, int)
     */
    @Override
    public void write(byte buffer[], int offset, int count) throws TIOException {
        if (buffer == null) {
            throw new TNullPointerException();
        }
        out.write(buffer, offset, count);
        written += count;
    }

    /**
     * Writes a byte to the target stream. Only the least significant byte of
     * the integer {@code oneByte} is written.
     *
     * @param oneByte
     *            the byte to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     */
    @Override
    public void write(int oneByte) throws TIOException {
        out.write(oneByte);
        written++;
    }

    /**
     * Writes a boolean to the target stream.
     *
     * @param val
     *            the boolean value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readBoolean()
     */
    @Override
    public final void writeBoolean(boolean val) throws TIOException {
        out.write(val ? 1 : 0);
        written++;
    }

    /**
     * Writes an 8-bit byte to the target stream. Only the least significant
     * byte of the integer {@code val} is written.
     *
     * @param val
     *            the byte value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     * @see DataInputStream#readUnsignedByte()
     */
    @Override
    public final void writeByte(int val) throws TIOException {
        out.write(val);
        written++;
    }

    /**
     * Writes the low order bytes from a string to the target stream.
     *
     * @param str
     *            the string containing the bytes to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readFully(byte[])
     * @see DataInputStream#readFully(byte[],int,int)
     */
    public final void writeBytes(TString str) throws TIOException {
        if (str.length() == 0) {
            return;
        }
        byte bytes[] = new byte[str.length()];
        for (int index = 0; index < str.length(); index++) {
            bytes[index] = (byte) str.charAt(index);
        }
        out.write(bytes);
        written += bytes.length;
    }

    /**
     * Writes a 16-bit character to the target stream. Only the two lower bytes
     * of the integer {@code val} are written, with the higher one written
     * first. This corresponds to the Unicode value of {@code val}.
     *
     * @param val
     *            the character to write to the target stream
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readChar()
     */
    @Override
    public final void writeChar(int val) throws TIOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    /**
     * Writes the 16-bit characters contained in {@code str} to the target
     * stream.
     *
     * @param str
     *            the string that contains the characters to write to this
     *            stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readChar()
     */
    @Override
    public final void writeChars(TString str) throws TIOException {
        byte newBytes[] = new byte[str.length() * 2];
        for (int index = 0; index < str.length(); index++) {
            int newIndex = index == 0 ? index : index * 2;
            newBytes[newIndex] = (byte) (str.charAt(index) >> 8);
            newBytes[newIndex + 1] = (byte) str.charAt(index);
        }
        out.write(newBytes);
        written += newBytes.length;
    }

    /**
     * Writes a 64-bit double to the target stream. The resulting output is the
     * eight bytes resulting from calling Double.doubleToLongBits().
     *
     * @param val
     *            the double to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readDouble()
     */
    @Override
    public final void writeDouble(double val) throws TIOException {
        writeLong(TDouble.doubleToLongBits(val));
    }

    /**
     * Writes a 32-bit float to the target stream. The resulting output is the
     * four bytes resulting from calling Float.floatToIntBits().
     *
     * @param val
     *            the float to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readFloat()
     */
    @Override
    public final void writeFloat(float val) throws TIOException {
        writeInt(TFloat.floatToIntBits(val));
    }

    /**
     * Writes a 32-bit int to the target stream. The resulting output is the
     * four bytes, highest order first, of {@code val}.
     *
     * @param val
     *            the int to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readInt()
     */
    @Override
    public final void writeInt(int val) throws TIOException {
        buff[0] = (byte) (val >> 24);
        buff[1] = (byte) (val >> 16);
        buff[2] = (byte) (val >> 8);
        buff[3] = (byte) val;
        out.write(buff, 0, 4);
        written += 4;
    }

    /**
     * Writes a 64-bit long to the target stream. The resulting output is the
     * eight bytes, highest order first, of {@code val}.
     *
     * @param val
     *            the long to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readLong()
     */
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

    int writeLongToBuffer(long val,
                          byte[] buffer, int offset) throws TIOException {
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

    /**
     * Writes the specified 16-bit short to the target stream. Only the lower
     * two bytes of the integer {@code val} are written, with the higher one
     * written first.
     *
     * @param val
     *            the short to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readShort()
     * @see DataInputStream#readUnsignedShort()
     */
    @Override
    public final void writeShort(int val) throws TIOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    int writeShortToBuffer(int val,
                           byte[] buffer, int offset) throws TIOException {
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    /**
     * Writes the specified encoded in {@link DataInput modified UTF-8} to this
     * stream.
     *
     * @param str
     *            the string to write to the target stream encoded in
     *            {@link DataInput modified UTF-8}.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @throws UTFDataFormatException
     *             if the encoded string is longer than 65535 bytes.
     * @see DataInputStream#readUTF()
     */
    @Override
    public final void writeUTF(TString str) throws TIOException {
        long utfCount = countUTFBytes(str);
        if (utfCount > 65535) {
            throw new TIOException(TString.wrap("UTF Error"));
        }
        byte[] buffer = new byte[(int)utfCount + 2];
        int offset = 0;
        offset = writeShortToBuffer((int) utfCount, buffer, offset);
        offset = writeUTFBytesToBuffer(str, buffer, offset);
        write(buffer, 0, offset);
    }

    long countUTFBytes(TString str) {
        int utfCount = 0, length = str.length();
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
