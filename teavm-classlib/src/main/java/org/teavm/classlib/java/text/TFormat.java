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
package org.teavm.classlib.java.text;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public abstract class TFormat implements TSerializable, TCloneable {
    public TFormat() {
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    String convertPattern(String template, String fromChars, String toChars, boolean check) {
        if (!check && fromChars.equals(toChars)) {
            return template;
        }
        boolean quote = false;
        StringBuilder output = new StringBuilder();
        int length = template.length();
        for (int i = 0; i < length; i++) {
            int index;
            char next = template.charAt(i);
            if (next == '\'') {
                quote = !quote;
            }
            index = fromChars.indexOf(next);
            if (!quote && index != -1) {
                output.append(toChars.charAt(index));
            } else if (check && !quote && ((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z'))) {
                throw new IllegalArgumentException("Invalid pattern char" + next + " in " + template);
            } else {
                output.append(next);
            }
        }
        if (quote) {
            throw new IllegalArgumentException("Unterminated quote");
        }
        return output.toString();
    }

    public final String format(Object object) {
        return format(object, new StringBuffer(), new TFieldPosition(0)).toString();
    }

    public abstract StringBuffer format(Object object, StringBuffer buffer, TFieldPosition field);

    public TAttributedCharacterIterator formatToCharacterIterator(Object object) {
        return new TAttributedString(format(object)).getIterator();
    }

    public Object parseObject(String string) throws TParseException {
        TParsePosition position = new TParsePosition(0);
        Object result = parseObject(string, position);
        if (position.getIndex() == 0) {
            throw new TParseException("Format.parseObject(String) parse failure", position.getErrorIndex());
        }
        return result;
    }

    public abstract Object parseObject(String string, TParsePosition position);

    static boolean upTo(String string, TParsePosition position, StringBuffer buffer, char stop) {
        int index = position.getIndex();
        int length = string.length();
        boolean lastQuote = false;
        boolean quote = false;
        while (index < length) {
            char ch = string.charAt(index++);
            if (ch == '\'') {
                if (lastQuote) {
                    buffer.append('\'');
                }
                quote = !quote;
                lastQuote = true;
            } else if (ch == stop && !quote) {
                position.setIndex(index);
                return true;
            } else {
                lastQuote = false;
                buffer.append(ch);
            }
        }
        position.setIndex(index);
        return false;
    }

    static boolean upToWithQuotes(String string, TParsePosition position, StringBuffer buffer, char stop, char start) {
        int index = position.getIndex();
        int length = string.length();
        int count = 1;
        boolean quote = false;
        while (index < length) {
            char ch = string.charAt(index++);
            if (ch == '\'') {
                quote = !quote;
            }
            if (!quote) {
                if (ch == stop) {
                    count--;
                }
                if (count == 0) {
                    position.setIndex(index);
                    return true;
                }
                if (ch == start) {
                    count++;
                }
            }
            buffer.append(ch);
        }
        throw new IllegalArgumentException("Unmatched braces in the pattern");
    }

    public static class Field extends TAttributedCharacterIterator.Attribute {
        protected Field(String fieldName) {
            super(fieldName);
        }
    }
}
