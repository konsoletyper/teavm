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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.io.TBufferedInputStream;
import org.teavm.classlib.java.io.TIOException;
import org.teavm.classlib.java.io.TInputStream;
import org.teavm.classlib.java.io.TOutputStream;
import org.teavm.classlib.java.io.TOutputStreamWriter;
import org.teavm.classlib.java.io.TPrintStream;
import org.teavm.classlib.java.io.TWriter;
import org.teavm.classlib.java.lang.TString;

public class TProperties extends THashtable<Object, Object> {
    /**
     * The default values for keys not found in this {@code Properties}
     * instance.
     */
    protected TProperties defaults;

    private static final int NONE = 0;
    private static final int SLASH = 1;
    private static final int UNICODE = 2;
    private static final int CONTINUE = 3;
    private static final int KEY_DONE = 4;
    private static final int IGNORE = 5;

    public TProperties() {
        super();
    }

    public TProperties(TProperties properties) {
        defaults = properties;
    }

    private void dumpString(StringBuilder buffer, String string, boolean isKey) {
        int index = 0;
        int length = string.length();
        if (!isKey && index < length && string.charAt(index) == ' ') {
            buffer.append("\\ "); //$NON-NLS-1$
            index++;
        }

        for (; index < length; index++) {
            char ch = string.charAt(index);
            switch (ch) {
            case '\t':
                buffer.append("\\t");
                break;
            case '\n':
                buffer.append("\\n");
                break;
            case '\f':
                buffer.append("\\f");
                break;
            case '\r':
                buffer.append("\\r");
                break;
            default:
                if ("\\#!=:".indexOf(ch) >= 0 || (isKey && ch == ' ')) {
                    buffer.append('\\');
                }
                if (ch >= ' ' && ch <= '~') {
                    buffer.append(ch);
                } else {
                    buffer.append(toHexaDecimal(ch));
                }
            }
        }
    }

    private char[] toHexaDecimal(final int ch) {
        char[] hexChars = { '\\', 'u', '0', '0', '0', '0' };
        int hexChar;
        int index = hexChars.length;
        int copyOfCh = ch;
        do {
            hexChar = copyOfCh & 15;
            if (hexChar > 9) {
                hexChar = hexChar - 10 + 'A';
            } else {
                hexChar += '0';
            }
            hexChars[--index] = (char) hexChar;
            copyOfCh >>>= 4;
        } while (copyOfCh != 0);
        return hexChars;
    }

    public String getProperty(String name) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        return property;
    }

    public String getProperty(String name, String defaultValue) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    public void list(TPrintStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        StringBuilder buffer = new StringBuilder(80);
        TEnumeration<?> keys = propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            buffer.append(key);
            buffer.append('=');
            String property = (String) super.get(key);
            TProperties def = defaults;
            while (property == null) {
                property = (String) def.get(key);
                def = def.defaults;
            }
            if (property.length() > 40) {
                buffer.append(property.substring(0, 37));
                buffer.append("..."); //$NON-NLS-1$
            } else {
                buffer.append(property);
            }
            out.println(TString.wrap(buffer.toString()));
            buffer.setLength(0);
        }
    }

    @SuppressWarnings("fallthrough")
    public synchronized void load(TInputStream in) throws TIOException {
        if (in == null) {
            throw new NullPointerException();
        }
        int mode = NONE;
        int unicode = 0;
        int count = 0;
        char nextChar;
        char[] buf = new char[40];
        int offset = 0;
        int keyLength = -1;
        int intVal;
        boolean firstChar = true;
        TBufferedInputStream bis = new TBufferedInputStream(in);

        while (true) {
            intVal = bis.read();
            if (intVal == -1) {
                // if mode is UNICODE but has less than 4 hex digits, should
                // throw an IllegalArgumentException
                if (mode == UNICODE && count < 4) {
                    throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
                }
                // if mode is SLASH and no data is read, should append '\u0000'
                // to buf
                if (mode == SLASH) {
                    buf[offset++] = '\u0000';
                }
                break;
            }
            nextChar = (char) (intVal & 0xff);

            if (offset == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, offset);
                buf = newBuf;
            }
            if (mode == UNICODE) {
                int digit = Character.digit(nextChar, 16);
                if (digit >= 0) {
                    unicode = (unicode << 4) + digit;
                    if (++count < 4) {
                        continue;
                    }
                } else if (count <= 4) {
                    throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
                }
                mode = NONE;
                buf[offset++] = (char) unicode;
                if (nextChar != '\n') {
                    continue;
                }
            }
            if (mode == SLASH) {
                mode = NONE;
                switch (nextChar) {
                case '\r':
                    mode = CONTINUE; // Look for a following \n
                    continue;
                case '\n':
                    mode = IGNORE; // Ignore whitespace on the next line
                    continue;
                case 'b':
                    nextChar = '\b';
                    break;
                case 'f':
                    nextChar = '\f';
                    break;
                case 'n':
                    nextChar = '\n';
                    break;
                case 'r':
                    nextChar = '\r';
                    break;
                case 't':
                    nextChar = '\t';
                    break;
                case 'u':
                    mode = UNICODE;
                    unicode = 0;
                    count = 0;
                    continue;
                }
            } else {
                switch (nextChar) {
                case '#':
                case '!':
                    if (firstChar) {
                        while (true) {
                            intVal = bis.read();
                            if (intVal == -1) {
                                break;
                            }
                            // & 0xff not required
                            nextChar = (char) intVal;
                            if (nextChar == '\r' || nextChar == '\n') {
                                break;
                            }
                        }
                        continue;
                    }
                    break;
                case '\n':
                    if (mode == CONTINUE) { // Part of a \r\n sequence
                        mode = IGNORE; // Ignore whitespace on the next line
                        continue;
                    }
                    // fall into the next case
                    break;
                case '\r':
                    if (mode == CONTINUE) { // Part of a \r\n sequence
                        mode = IGNORE; // Ignore whitespace on the next line
                        continue;
                    }

                    mode = NONE;
                    firstChar = true;
                    if (offset > 0 || (offset == 0 && keyLength == 0)) {
                        if (keyLength == -1) {
                            keyLength = offset;
                        }
                        String temp = new String(buf, 0, offset);
                        put(temp.substring(0, keyLength), temp
                                .substring(keyLength));
                    }
                    keyLength = -1;
                    offset = 0;
                    continue;
                case '\\':
                    if (mode == KEY_DONE) {
                        keyLength = offset;
                    }
                    mode = SLASH;
                    continue;
                case ':':
                case '=':
                    if (keyLength == -1) { // if parsing the key
                        mode = NONE;
                        keyLength = offset;
                        continue;
                    }
                    break;
                }
                if (nextChar < 256 && Character.isWhitespace(nextChar)) {
                    if (mode == CONTINUE) {
                        mode = IGNORE;
                    }
                    // if key length == 0 or value length == 0
                    if (offset == 0 || offset == keyLength || mode == IGNORE) {
                        continue;
                    }
                    if (keyLength == -1) { // if parsing the key
                        mode = KEY_DONE;
                        continue;
                    }
                }
                if (mode == IGNORE || mode == CONTINUE) {
                    mode = NONE;
                }
            }
            firstChar = false;
            if (mode == KEY_DONE) {
                keyLength = offset;
                mode = NONE;
            }
            buf[offset++] = nextChar;
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            put(temp.substring(0, keyLength), temp.substring(keyLength));
        }
    }

    public TEnumeration<?> propertyNames() {
        THashtable<Object, Object> selected = new THashtable<>();
        selectProperties(selected);
        return selected.keys();
    }

    private void selectProperties(TMap<Object, Object> selected) {
        if (defaults != null) {
            defaults.selectProperties(selected);
        }
        selected.putAll(this);
    }

    @Deprecated
    public void save(TOutputStream out, String comment) {
        try {
            store(out, comment);
        } catch (TIOException e) {
            // do nothing
        }
    }

    public Object setProperty(String name, String value) {
        return put(name, value);
    }

    public synchronized void store(TOutputStream out, String comments) throws TIOException {
        TOutputStreamWriter writer = new TOutputStreamWriter(out, "ISO8859_1");
        if (comments != null) {
            writeComments(writer, comments);
        }
        writer.write('#');
        writer.write(new TDate().toString());
        writer.write("\n");

        StringBuilder buffer = new StringBuilder(200);
        for (TIterator<TMap.Entry<Object, Object>> iter = entrySet().iterator(); iter.hasNext();) {
            TMap.Entry<Object, Object> entry = iter.next();
            String key = (String) entry.getKey();
            dumpString(buffer, key, true);
            buffer.append('=');
            dumpString(buffer, (String) entry.getValue(), false);
            buffer.append("\n");
            writer.write(buffer.toString());
            buffer.setLength(0);
        }
        writer.flush();
    }

    private void writeComments(TWriter writer, String comments) throws TIOException {
        writer.write('#');
        char[] chars = comments.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            if (chars[index] == '\r' || chars[index] == '\n') {
                int indexPlusOne = index + 1;
                if (chars[index] == '\r' && indexPlusOne < chars.length
                        && chars[indexPlusOne] == '\n') {
                    // "\r\n"
                    continue;
                }
                writer.write("\n");
                if (indexPlusOne < chars.length
                        && (chars[indexPlusOne] == '#' || chars[indexPlusOne] == '!')) {
                    // return char with either '#' or '!' afterward
                    continue;
                }
                writer.write('#');
            } else {
                writer.write(chars[index]);
            }
        }
        writer.write("\n");
    }

    public TSet<String> stringPropertyNames() {
        TSet<String> selected = new THashSet<>();
        selectPropertyNames(selected);
        return selected;
    }

    private void selectPropertyNames(TSet<String> selected) {
        if (defaults != null) {
            defaults.selectPropertyNames(selected);
        }

        TEnumeration<Object> e = keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            if (key instanceof String) {
                selected.add((String) key);
            }
        }
    }
}
