/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package org.teavm.classlib.java.io;
import org.teavm.classlib.java.lang.*;
/**
 * The DataOutput interface provides for converting data from any of the Java primitive types to a series of bytes and writing these bytes to a binary stream. There is also a facility for converting a String into Java modified UTF-8 format and writing the resulting series of bytes.
 * For all the methods in this interface that write bytes, it is generally true that if a byte cannot be written for any reason, an IOException is thrown.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInput, DataOutputStream
 */
public interface TDataOutput{
    /**
     * Writes to the output stream all the bytes in array b. If b is null, a NullPointerException is thrown. If b.length is zero, then no bytes are written. Otherwise, the byte b[0] is written first, then b[1], and so on; the last byte written is b[b.length-1].
     */
    public abstract void write(byte[] b) throws TIOException;

    /**
     * Writes len bytes from array b, in order, to the output stream. If b is null, a NullPointerException is thrown. If off is negative, or len is negative, or off+len is greater than the length of the array b, then an IndexOutOfBoundsException is thrown. If len is zero, then no bytes are written. Otherwise, the byte b[off] is written first, then b[off+1], and so on; the last byte written is b[off+len-1].
     */
    public abstract void write(byte[] b, int off, int len) throws TIOException;

    /**
     * Writes to the output stream the eight low-order bits of the argument b. The 24 high-order bits of b are ignored.
     */
    public abstract void write(int b) throws TIOException;

    /**
     * Writes a boolean value to this output stream. If the argument v is true, the value (byte)1 is written; if v is false, the value (byte)0 is written. The byte written by this method may be read by the readBoolean method of interface DataInput, which will then return a boolean equal to v.
     */
    public abstract void writeBoolean(boolean v) throws TIOException;

    /**
     * Writes to the output stream the eight low- order bits of the argument v. The 24 high-order bits of v are ignored. (This means that writeByte does exactly the same thing as write for an integer argument.) The byte written by this method may be read by the readByte method of interface DataInput, which will then return a byte equal to (byte)v.
     */
    public abstract void writeByte(int v) throws TIOException;

    /**
     * Writes a char value, which is comprised of two bytes, to the output stream. The byte values to be written, in the order shown, are:
     * (byte)(0xff (v 8)) (byte)(0xff v)
     * The bytes written by this method may be read by the readChar method of interface DataInput, which will then return a char equal to (char)v.
     */
    public abstract void writeChar(int v) throws TIOException;

    /**
     * Writes every character in the string s, to the output stream, in order, two bytes per character. If s is null, a NullPointerException is thrown. If s.length is zero, then no characters are written. Otherwise, the character s[0] is written first, then s[1], and so on; the last character written is s[s.length-1]. For each character, two bytes are actually written, high-order byte first, in exactly the manner of the writeChar method.
     */
    public abstract void writeChars(TString s) throws java.io.IOException;

    /**
     * Writes a double value, which is comprised of eight bytes, to the output stream. It does this as if it first converts this double value to a long in exactly the manner of the Double.doubleToLongBits method and then writes the long value in exactly the manner of the writeLong method. The bytes written by this method may be read by the readDouble method of interface DataInput, which will then return a double equal to v.
     */
    public abstract void writeDouble(double v) throws TIOException;

    /**
     * Writes a float value, which is comprised of four bytes, to the output stream. It does this as if it first converts this float value to an int in exactly the manner of the Float.floatToIntBits method and then writes the int value in exactly the manner of the writeInt method. The bytes written by this method may be read by the readFloat method of interface DataInput, which will then return a float equal to v.
     */
    public abstract void writeFloat(float v) throws TIOException;

    /**
     * Writes an int value, which is comprised of four bytes, to the output stream. The byte values to be written, in the order shown, are:
     * (byte)(0xff (v 24)) (byte)(0xff (v 16)) (byte)(0xff (v 8)) (byte)(0xff v)
     * The bytes written by this method may be read by the readInt method of interface DataInput, which will then return an int equal to v.
     */
    public abstract void writeInt(int v) throws TIOException;

    /**
     * Writes an long value, which is comprised of four bytes, to the output stream. The byte values to be written, in the order shown, are:
     * (byte)(0xff (v 56)) (byte)(0xff (v 48)) (byte)(0xff (v 40)) (byte)(0xff (v 32)) (byte)(0xff (v 24)) (byte)(0xff (v 16)) (byte)(0xff (v 8)) (byte)(0xff v)
     * The bytes written by this method may be read by the readLong method of interface DataInput, which will then return a long equal to v.
     */
    public abstract void writeLong(long v) throws TIOException;

    /**
     * Writes two bytes to the output stream to represent the value of the argument. The byte values to be written, in the order shown, are:
     * (byte)(0xff (v 8)) (byte)(0xff v)
     * The bytes written by this method may be read by the readShort method of interface DataInput, which will then return a short equal to (short)v.
     */
    public abstract void writeShort(int v) throws TIOException;

    /**
     * Writes two bytes of length information to the output stream, followed by the Java modified UTF representation of every character in the string s. If s is null, a NullPointerException is thrown. Each character in the string s is converted to a group of one, two, or three bytes, depending on the value of the character.
     * If a character c is in the range u0001 through u007f, it is represented by one byte:
     * (byte)c
     * If a character c is u0000 or is in the range u0080 through u07ff, then it is represented by two bytes, to be written in the order shown:
     * (byte)(0xc0 | (0x1f (c 6))) (byte)(0x80 | (0x3f c))
     * If a character c is in the range u0800 through uffff, then it is represented by three bytes, to be written in the order shown:
     * (byte)(0xe0 | (0x0f (c 12))) (byte)(0x80 | (0x3f (c 6))) (byte)(0x80 | (0x3f c))
     * First, the total number of bytes needed to represent all the characters of s is calculated. If this number is larger than 65535, then a UTFDataFormatError is thrown. Otherwise, this length is written to the output stream in exactly the manner of the writeShort method; after this, the one-, two-, or three-byte representation of each character in the string s is written.
     * The bytes written by this method may be read by the readUTF method of interface DataInput, which will then return a String equal to s.
     */
    public abstract void writeUTF(TString s) throws TIOException;

}
