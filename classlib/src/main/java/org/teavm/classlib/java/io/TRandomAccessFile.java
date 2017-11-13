/*
 *  Copyright 2017 Alexey Andreev.
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

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.Objects;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileAccessor;
import org.teavm.classlib.java.lang.TIndexOutOfBoundsException;
import org.teavm.classlib.java.lang.TNullPointerException;

public class TRandomAccessFile implements DataInput, DataOutput, Closeable {
    private boolean readOnly;
    private VirtualFileAccessor accessor;
    private int pos;
    private byte[] buff;

    public TRandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(new TFile(name), mode);
    }

    public TRandomAccessFile(TFile file, String mode) throws FileNotFoundException {
        switch (mode) {
            case "r":
                readOnly = true;
                break;
            case "rw":
            case "rwd":
            case "rws":
                break;
            default:
                throw new IllegalArgumentException("Invalid mode: " + mode);
        }

        VirtualFile virtualFile = file.findVirtualFile();
        if (virtualFile == null || virtualFile.isDirectory()) {
            throw new FileNotFoundException();
        }

        accessor = virtualFile.createAccessor();
        if (accessor == null) {
            throw new FileNotFoundException();
        }
    }

    @Override
    public void close() throws IOException {
        accessor = null;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b);
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= accessor.size()) {
            return -1;
        }
        ensureOpened();
        int result = accessor.read(pos, b, off, len);
        pos += result;
        return result;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read() throws IOException {
        ensureOpened();
        byte[] buffer = new byte[1];
        int read = accessor.read(pos, buffer, 0, 1);
        pos += read;
        return read > 0 ? buffer[0] : -1;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new TIndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }

        if (b == null) {
            throw new TNullPointerException();
        }
        if (off < 0 || off > b.length - len) {
            throw new TIndexOutOfBoundsException();
        }

        ensureOpened();

        while (len > 0) {
            int result = read(b, off, len);
            if (result < 0) {
                throw new EOFException();
            }
            off += result;
            len -= result;
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        ensureOpened();

        int newPos = Math.max(pos, Math.min(accessor.size(), pos));
        int result = newPos - pos;
        pos = newPos;
        return result;
    }

    public long getFilePointer() throws IOException {
        ensureOpened();
        return pos;
    }

    public void seek(long pos) throws IOException {
        ensureOpened();
        this.pos = (int) pos;
    }

    public long length() throws IOException {
        ensureOpened();
        return accessor.size();
    }

    public void setLength(long newLength) throws IOException {
        ensureOpened();
        accessor.resize((int) newLength);
    }

    @Override
    public boolean readBoolean() throws IOException {
        int temp = read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp != 0;
    }

    @Override
    public byte readByte() throws IOException {
        int temp = read();
        if (temp < 0) {
            throw new EOFException();
        }
        return (byte) temp;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        int temp = read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        if (readToBuff(2) < 0) {
            throw new EOFException();
        }
        return (short) ((((buff[0] & 0xff) << 24) >> 16) | (buff[1] & 0xff));
    }

    @Override
    public int readUnsignedShort() throws IOException {
        if (readToBuff(2) < 0) {
            throw new EOFException();
        }
        return (char) (((buff[0] & 0xff) << 8) | (buff[1] & 0xff));
    }

    @Override
    public char readChar() throws IOException {
        if (readToBuff(2) < 0) {
            throw new EOFException();
        }
        return (char) (((buff[0] & 0xff) << 8) | (buff[1] & 0xff));
    }

    @Override
    public int readInt() throws IOException {
        if (readToBuff(4) < 0) {
            throw new TEOFException();
        }
        return ((buff[0] & 0xff) << 24) | ((buff[1] & 0xff) << 16) | ((buff[2] & 0xff) << 8) | (buff[3] & 0xff);
    }

    @Override
    public long readLong() throws IOException {
        if (readToBuff(8) < 0) {
            throw new TEOFException();
        }
        int i1 = ((buff[0] & 0xff) << 24) | ((buff[1] & 0xff) << 16) | ((buff[2] & 0xff) << 8) | (buff[3] & 0xff);
        int i2 = ((buff[4] & 0xff) << 24) | ((buff[5] & 0xff) << 16) | ((buff[6] & 0xff) << 8) | (buff[7] & 0xff);
        return ((i1 & 0xffffffffL) << 32) | (i2 & 0xffffffffL);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder(80);
        boolean foundTerminator = false;
        while (true) {
            int nextByte = read();
            switch (nextByte) {
                case -1:
                    if (line.length() == 0 && !foundTerminator) {
                        return null;
                    }
                    return line.toString();
                case (byte) '\r':
                    if (foundTerminator) {
                        seek(getFilePointer() - 1);
                        return line.toString();
                    }
                    foundTerminator = true;
                    break;
                case (byte) '\n':
                    return line.toString();
                default:
                    if (foundTerminator) {
                        seek(getFilePointer() - 1);
                        return line.toString();
                    }
                    line.append((char) nextByte);
            }
        }
    }

    @Override
    public String readUTF() throws IOException {
        return decodeUTF(readUnsignedShort());
    }

    String decodeUTF(int utfSize) throws IOException {
        byte[] buf = new byte[utfSize];
        char[] out = new char[utfSize];
        readFully(buf, 0, utfSize);

        return convertUTF8WithBuf(buf, out, 0, utfSize);
    }

    @Override
    public void write(int b) throws IOException {
        if (readOnly) {
            throw new IOException("This instance is read-only");
        }
        ensureOpened();
        byte[] buffer = { (byte) b };
        accessor.write(pos, buffer, 0, 1);
        pos++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (readOnly) {
            throw new IOException("This instance is read-only");
        }
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b);
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpened();
        accessor.write(pos, b, off, len);
        pos += len;
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        buff[0] = (byte) (v >> 8);
        buff[1] = (byte) v;
        write(buff, 0, 2);
    }

    @Override
    public void writeChar(int v) throws IOException {
        buff[0] = (byte) (v >> 8);
        buff[1] = (byte) v;
        write(buff, 0, 2);
    }

    @Override
    public void writeInt(int v) throws IOException {
        buff[0] = (byte) (v >> 24);
        buff[1] = (byte) (v >> 16);
        buff[2] = (byte) (v >> 8);
        buff[3] = (byte) v;
        write(buff, 0, 4);
    }

    @Override
    public void writeLong(long v) throws IOException {
        buff[0] = (byte) (v >> 56);
        buff[1] = (byte) (v >> 48);
        buff[2] = (byte) (v >> 40);
        buff[3] = (byte) (v >> 32);
        buff[4] = (byte) (v >> 24);
        buff[5] = (byte) (v >> 16);
        buff[6] = (byte) (v >> 8);
        buff[7] = (byte) v;
        write(buff, 0, 8);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        if (s.length() == 0) {
            return;
        }
        byte[] bytes = new byte[s.length()];
        for (int index = 0; index < s.length(); index++) {
            bytes[index] = (byte) s.charAt(index);
        }
        write(bytes);
    }

    @Override
    public void writeChars(String s) throws IOException {
        byte[] newBytes = new byte[s.length() * 2];
        for (int index = 0; index < s.length(); index++) {
            int newIndex = index == 0 ? index : index * 2;
            newBytes[newIndex] = (byte) (s.charAt(index) >> 8);
            newBytes[newIndex + 1] = (byte) s.charAt(index);
        }
        write(newBytes);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        long utfCount = countUTFBytes(s);
        if (utfCount > 65535) {
            throw new IOException("UTF Error");
        }
        byte[] buffer = new byte[(int) utfCount + 2];
        int offset = 0;
        offset = writeShortToBuffer((int) utfCount, buffer, offset);
        offset = writeUTFBytesToBuffer(s, buffer, offset);
        write(buffer, 0, offset);
    }

    private void ensureOpened() throws IOException {
        if (accessor == null) {
            throw new IOException("This stream is already closed");
        }
    }

    private int readToBuff(int count) throws IOException {
        int offset = 0;
        while (offset < count) {
            int bytesRead = read(buff, offset, count - offset);
            if (bytesRead == -1) {
                return bytesRead;
            }
            offset += bytesRead;
        }
        return offset;
    }

    private static String convertUTF8WithBuf(byte[] buf, char[] out, int offset, int utfSize)
            throws UTFDataFormatException {
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
                    throw new UTFDataFormatException("End of stream reached");
                }
                int b = buf[offset + count++];
                if ((b & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("Malformed UTF-8 sequence");
                }
                out[s++] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
            } else if ((a & 0xf0) == 0xe0) {
                if (count + 1 >= utfSize) {
                    throw new UTFDataFormatException("Malformed UTF-8 sequence");
                }
                int b = buf[offset + count++];
                int c = buf[offset + count++];
                if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
                    throw new UTFDataFormatException("Malformed UTF-8 sequence");
                }
                out[s++] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
            } else {
                throw new UTFDataFormatException("Malformed UTF-8 sequence");
            }
        }
        return new String(out, 0, s);
    }

    static int writeShortToBuffer(int val, byte[] buffer, int offset) throws TIOException {
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    static long countUTFBytes(String str) {
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

    static int writeUTFBytesToBuffer(String str, byte[] buffer, int offset) throws IOException {
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
