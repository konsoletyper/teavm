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
package org.teavm.classlib.java.text;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TLocale;

public class TMessageFormat extends TFormat {
    private TLocale locale = TLocale.getDefault();
    transient private String[] strings;
    private int[] argumentNumbers;
    private TFormat[] formats;
    private int maxOffset;
    transient private int maxArgumentIndex;

    public TMessageFormat(String template, TLocale locale) {
        this.locale = locale;
        applyPattern(template);
    }

    public TMessageFormat(String template) {
        applyPattern(template);
    }

    public void applyPattern(String template) {
        int length = template.length();
        StringBuffer buffer = new StringBuffer();
        TParsePosition position = new TParsePosition(0);
        List<String> localStrings = new ArrayList<>();
        int argCount = 0;
        int[] args = new int[10];
        int maxArg = -1;
        List<TFormat> localFormats = new ArrayList<>();
        while (position.getIndex() < length) {
            if (TFormat.upTo(template, position, buffer, '{')) {
                int arg = 0;
                int offset = position.getIndex();
                if (offset >= length) {
                    throw new IllegalArgumentException("Invalid argument number");
                }
                // Get argument number
                while (true) {
                    char ch = template.charAt(offset++);
                    if (ch == '}' || ch == ',') {
                        break;
                    }
                    if (ch < '0' && ch > '9') {
                        throw new IllegalArgumentException("Invalid argument number");
                    }
                    arg = arg * 10 + (ch - '0');
                    if (arg < 0 || offset >= length) {
                        throw new IllegalArgumentException("Invalid argument number");
                    }
                }
                offset--;
                position.setIndex(offset);
                localFormats.add(parseVariable(template, position));
                if (argCount >= args.length) {
                    int[] newArgs = new int[args.length * 2];
                    System.arraycopy(args, 0, newArgs, 0, args.length);
                    args = newArgs;
                }
                args[argCount++] = arg;
                if (arg > maxArg) {
                    maxArg = arg;
                }
            }
            localStrings.add(buffer.toString());
            buffer.setLength(0);
        }
        this.strings = new String[localStrings.size()];
        for (int i = 0; i < localStrings.size(); i++) {
            this.strings[i] = localStrings.get(i);
        }
        argumentNumbers = args;
        this.formats = new TFormat[argCount];
        for (int i = 0; i < argCount; i++) {
            this.formats[i] = localFormats.get(i);
        }
        maxOffset = argCount - 1;
        maxArgumentIndex = maxArg;
    }

    @Override
    public Object clone() {
        TMessageFormat clone = (TMessageFormat) super.clone();
        TFormat[] array = new TFormat[formats.length];
        for (int i = formats.length; --i >= 0;) {
            if (formats[i] != null) {
                array[i] = (TFormat) formats[i].clone();
            }
        }
        clone.formats = array;
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TMessageFormat)) {
            return false;
        }
        TMessageFormat format = (TMessageFormat) object;
        if (maxOffset != format.maxOffset) {
            return false;
        }
        // Must use a loop since the lengths may be different due
        // to serialization cross-loading
        for (int i = 0; i <= maxOffset; i++) {
            if (argumentNumbers[i] != format.argumentNumbers[i]) {
                return false;
            }
        }
        return locale.equals(format.locale)
                && Arrays.equals(strings, format.strings)
                && Arrays.equals(formats, format.formats);
    }

    @Override
    public TAttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }

        StringBuffer buffer = new StringBuffer();
        List<FieldContainer> fields = new ArrayList<>();

        // format the message, and find fields
        formatImpl((Object[]) object, buffer, new TFieldPosition(0), fields);

        // create an AttributedString with the formatted buffer
        TAttributedString as = new TAttributedString(buffer.toString());

        // add TMessageFormat field attributes and values to the AttributedString
        for (FieldContainer fc : fields) {
            as.addAttribute(fc.attribute, fc.value, fc.start, fc.end);
        }

        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    public final StringBuffer format(Object[] objects, StringBuffer buffer, TFieldPosition field) {
        return formatImpl(objects, buffer, field, null);
    }

    private StringBuffer formatImpl(Object[] objects, StringBuffer buffer, TFieldPosition position,
            List<FieldContainer> fields) {
        TFieldPosition passedField = new TFieldPosition(0);
        for (int i = 0; i <= maxOffset; i++) {
            buffer.append(strings[i]);
            int begin = buffer.length();
            Object arg;
            if (objects != null && argumentNumbers[i] < objects.length) {
                arg = objects[argumentNumbers[i]];
            } else {
                buffer.append('{');
                buffer.append(argumentNumbers[i]);
                buffer.append('}');
                handleArgumentField(begin, buffer.length(), argumentNumbers[i], position, fields);
                continue;
            }
            TFormat format = formats[i];
            if (format == null || arg == null) {
                if (arg instanceof Number) {
                    format = TNumberFormat.getInstance();
                } else if (arg instanceof Date) {
                    format = TDateFormat.getInstance();
                } else {
                    buffer.append(arg);
                    handleArgumentField(begin, buffer.length(), argumentNumbers[i], position, fields);
                    continue;
                }
            }
            if (format instanceof TChoiceFormat) {
                String result = format.format(arg);
                TMessageFormat mf = new TMessageFormat(result);
                mf.setLocale(locale);
                mf.format(objects, buffer, passedField);
                handleArgumentField(begin, buffer.length(), argumentNumbers[i], position, fields);
                handleformat(format, arg, begin, fields);
            } else {
                format.format(arg, buffer, passedField);
                handleArgumentField(begin, buffer.length(), argumentNumbers[i], position, fields);
                handleformat(format, arg, begin, fields);
            }
        }
        if (maxOffset + 1 < strings.length) {
            buffer.append(strings[maxOffset + 1]);
        }
        return buffer;
    }

    private void handleArgumentField(int begin, int end, int argnumber, TFieldPosition position,
            List<FieldContainer> fields) {
        if (fields != null) {
            fields.add(new FieldContainer(begin, end, Field.ARGUMENT, argnumber));
        } else {
            if (position != null
                    && position.getFieldAttribute() == Field.ARGUMENT
                    && position.getEndIndex() == 0) {
                position.setBeginIndex(begin);
                position.setEndIndex(end);
            }
        }
    }

    /**
     * An inner class to store attributes, values, start and end indices.
     * Instances of this inner class are used as elements for the fields vector
     */
    static class FieldContainer {
        int start;
        int end;
        TAttributedCharacterIterator.Attribute attribute;
        Object value;

        FieldContainer(int start, int end, TAttributedCharacterIterator.Attribute attribute, Object value) {
            this.start = start;
            this.end = end;
            this.attribute = attribute;
            this.value = value;
        }
    }

    private void handleformat(TFormat format, Object arg, int begin, List<FieldContainer> fields) {
        if (fields != null) {
            TAttributedCharacterIterator iterator = format.formatToCharacterIterator(arg);
            while (iterator.getIndex() != iterator.getEndIndex()) {
                int start = iterator.getRunStart();
                int end = iterator.getRunLimit();

                TIterator<TAttributedCharacterIterator.Attribute> iter = iterator.getAttributes().keySet().iterator();
                while (iter.hasNext()) {
                    TAttributedCharacterIterator.Attribute attribute = iter.next();
                    Object value = iterator.getAttribute(attribute);
                    fields.add(new FieldContainer(begin + start, begin + end, attribute, value));
                }
                iterator.setIndex(end);
            }
        }
    }

    @Override
    public final StringBuffer format(Object object, StringBuffer buffer, TFieldPosition field) {
        return format((Object[]) object, buffer, field);
    }

    public static String format(String template, Object... objects) {
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] == null) {
                    objects[i] = "null";
                }
            }
        }
        return new TMessageFormat(template).format(objects, new StringBuffer(), new TFieldPosition(0)).toString();
    }

    public TFormat[] getFormats() {
        return formats.clone();
    }

    public TFormat[] getFormatsByArgumentIndex() {
        TFormat[] answer = new TFormat[maxArgumentIndex + 1];
        for (int i = 0; i < maxOffset + 1; i++) {
            answer[argumentNumbers[i]] = formats[i];
        }
        return answer;
    }

    public void setFormatByArgumentIndex(int argIndex, TFormat format) {
        for (int i = 0; i < maxOffset + 1; i++) {
            if (argumentNumbers[i] == argIndex) {
                formats[i] = format;
            }
        }
    }

    public void setFormatsByArgumentIndex(TFormat[] formats) {
        for (int j = 0; j < formats.length; j++) {
            for (int i = 0; i < maxOffset + 1; i++) {
                if (argumentNumbers[i] == j) {
                    this.formats[i] = formats[j];
                }
            }
        }
    }

    public TLocale getLocale() {
        return locale;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i <= maxOffset; i++) {
            hashCode += argumentNumbers[i] + strings[i].hashCode();
            if (formats[i] != null) {
                hashCode += formats[i].hashCode();
            }
        }
        if (maxOffset + 1 < strings.length) {
            hashCode += strings[maxOffset + 1].hashCode();
        }
        if (locale != null) {
            return hashCode + locale.hashCode();
        }
        return hashCode;
    }

    public Object[] parse(String string) throws ParseException {
        TParsePosition position = new TParsePosition(0);
        Object[] result = parse(string, position);
        if (position.getIndex() == 0) {
            throw new ParseException("MessageFormat.parseObject(String) parse failure", position.getErrorIndex());
        }
        return result;
    }

    public Object[] parse(String string, TParsePosition position) {
        if (string == null) {
            return new Object[0];
        }
        TParsePosition internalPos = new TParsePosition(0);
        int offset = position.getIndex();
        Object[] result = new Object[maxArgumentIndex + 1];
        for (int i = 0; i <= maxOffset; i++) {
            String sub = strings[i];
            if (!string.startsWith(sub, offset)) {
                position.setErrorIndex(offset);
                return null;
            }
            offset += sub.length();
            Object parse;
            TFormat format = formats[i];
            if (format == null) {
                if (i + 1 < strings.length) {
                    int next = string.indexOf(strings[i + 1], offset);
                    if (next == -1) {
                        position.setErrorIndex(offset);
                        return null;
                    }
                    parse = string.substring(offset, next);
                    offset = next;
                } else {
                    parse = string.substring(offset);
                    offset = string.length();
                }
            } else {
                internalPos.setIndex(offset);
                parse = format.parseObject(string, internalPos);
                if (internalPos.getErrorIndex() != -1) {
                    position.setErrorIndex(offset);
                    return null;
                }
                offset = internalPos.getIndex();
            }
            result[argumentNumbers[i]] = parse;
        }
        if (maxOffset + 1 < strings.length) {
            String sub = strings[maxOffset + 1];
            if (!string.startsWith(sub, offset)) {
                position.setErrorIndex(offset);
                return null;
            }
            offset += sub.length();
        }
        position.setIndex(offset);
        return result;
    }

    @Override
    public Object parseObject(String string, TParsePosition position) {
        return parse(string, position);
    }

    private int match(String string, TParsePosition position, boolean last, String[] tokens) {
        int length = string.length();
        int offset = position.getIndex();
        int token = -1;
        while (offset < length && Character.isWhitespace(string.charAt(offset))) {
            offset++;
        }
        for (int i = tokens.length; --i >= 0;) {
            if (string.regionMatches(true, offset, tokens[i], 0, tokens[i].length())) {
                token = i;
                break;
            }
        }
        if (token == -1) {
            return -1;
        }
        offset += tokens[token].length();
        while (offset < length && Character.isWhitespace(string.charAt(offset))) {
            offset++;
        }
        char ch;
        if (offset < length) {
            ch = string.charAt(offset);
            if (ch == '}' || (!last && ch == ',')) {
                position.setIndex(offset + 1);
                return token;
            }
        }
        return -1;
    }

    private TFormat parseVariable(String string, TParsePosition position) {
        int length = string.length();
        int offset = position.getIndex();
        char ch;
        if (offset >= length) {
            throw new IllegalArgumentException("Missing element format");
        }

        ch = string.charAt(offset++);
        if (ch != '}' && ch != ',') {
            throw new IllegalArgumentException("Missing element format");
        }

        position.setIndex(offset);
        if (ch == '}') {
            return null;
        }
        int type = match(string, position, false, new String[] { "time", "date", "number", "choice" });
        if (type == -1) {
            throw new IllegalArgumentException("Unknown element format");
        }
        StringBuffer buffer = new StringBuffer();
        ch = string.charAt(position.getIndex() - 1);
        switch (type) {
            case 0: // time
            case 1: // date
                if (ch == '}') {
                    return type == 1
                            ? TDateFormat.getDateInstance(DateFormat.DEFAULT, locale)
                            : TDateFormat.getTimeInstance(DateFormat.DEFAULT, locale);
                }
                int dateStyle = match(string, position, true, new String[] { "full", "long", "medium", "short" });
                if (dateStyle == -1) {
                    TFormat.upToWithQuotes(string, position, buffer, '}', '{');
                    return new TSimpleDateFormat(buffer.toString(), locale);
                }
                switch (dateStyle) {
                    case 0:
                        dateStyle = DateFormat.FULL;
                        break;
                    case 1:
                        dateStyle = DateFormat.LONG;
                        break;
                    case 2:
                        dateStyle = DateFormat.MEDIUM;
                        break;
                    case 3:
                        dateStyle = DateFormat.SHORT;
                        break;
                }
                return type == 1
                        ? TDateFormat.getDateInstance(dateStyle, locale)
                        : TDateFormat.getTimeInstance(dateStyle, locale);
            case 2: // number
                if (ch == '}') {
                    return TNumberFormat.getInstance();
                }
                int numberStyle = match(string, position, true, new String[] { "currency", "percent", "integer" });
                if (numberStyle == -1) {
                    upToWithQuotes(string, position, buffer, '}', '{');
                    return new TDecimalFormat(buffer.toString(), new TDecimalFormatSymbols(locale));
                }
                switch (numberStyle) {
                    case 0: // currency
                        return TNumberFormat.getCurrencyInstance(locale);
                    case 1: // percent
                        return TNumberFormat.getPercentInstance(locale);
                }
                return TNumberFormat.getIntegerInstance(locale);
        }
        // choice
        try {
            upToWithQuotes(string, position, buffer, '}', '{');
        } catch (IllegalArgumentException e) {
            // ignored
        }
        return new TChoiceFormat(buffer.toString());
    }

    public void setFormat(int offset, TFormat format) {
        formats[offset] = format;
    }

    public void setFormats(TFormat[] formats) {
        int min = this.formats.length;
        if (formats.length < min) {
            min = formats.length;
        }
        for (int i = 0; i < min; i++) {
            this.formats[i] = formats[i];
        }
    }

    public void setLocale(TLocale locale) {
        this.locale = locale;
        for (int i = 0; i <= maxOffset; i++) {
            TFormat format = formats[i];
            if (format instanceof TDecimalFormat) {
                formats[i] = new TDecimalFormat(((TDecimalFormat) format).toPattern(),
                        new TDecimalFormatSymbols(locale));
            } else if (format instanceof TSimpleDateFormat) {
                formats[i] = new TSimpleDateFormat(((TSimpleDateFormat) format).toPattern(), locale);
            }

        }
    }

    private String decodeDecimalFormat(StringBuffer buffer, TFormat format) {
        buffer.append(",number");
        if (format.equals(TNumberFormat.getNumberInstance(locale))) {
            // Empty block
        } else if (format.equals(TNumberFormat.getIntegerInstance(locale))) {
            buffer.append(",integer");
        } else if (format.equals(TNumberFormat.getCurrencyInstance(locale))) {
            buffer.append(",currency");
        } else if (format.equals(TNumberFormat.getPercentInstance(locale))) {
            buffer.append(",percent");
        } else {
            buffer.append(',');
            return ((TDecimalFormat) format).toPattern();
        }
        return null;
    }

    private String decodeSimpleDateFormat(StringBuffer buffer, TFormat format) {
        if (format.equals(TDateFormat.getTimeInstance(DateFormat.DEFAULT, locale))) {
            buffer.append(",time");
        } else if (format.equals(TDateFormat.getDateInstance(DateFormat.DEFAULT, locale))) {
            buffer.append(",date");
        } else if (format.equals(TDateFormat.getTimeInstance(DateFormat.SHORT, locale))) {
            buffer.append(",time,short");
        } else if (format.equals(TDateFormat.getDateInstance(DateFormat.SHORT, locale))) {
            buffer.append(",date,short");
        } else if (format.equals(TDateFormat.getTimeInstance(DateFormat.LONG, locale))) {
            buffer.append(",time,long");
        } else if (format.equals(TDateFormat.getDateInstance(DateFormat.LONG, locale))) {
            buffer.append(",date,long");
        } else if (format.equals(TDateFormat.getTimeInstance(DateFormat.FULL, locale))) {
            buffer.append(",time,full");
        } else if (format.equals(TDateFormat.getDateInstance(DateFormat.FULL, locale))) {
            buffer.append(",date,full");
        } else {
            buffer.append(",date,");
            return ((TSimpleDateFormat) format).toPattern();
        }
        return null;
    }

    public String toPattern() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i <= maxOffset; i++) {
            appendQuoted(buffer, strings[i]);
            buffer.append('{');
            buffer.append(argumentNumbers[i]);
            TFormat format = formats[i];
            String pattern = null;
            if (format instanceof TChoiceFormat) {
                buffer.append(",choice,");
                pattern = ((TChoiceFormat) format).toPattern();
            } else if (format instanceof TDecimalFormat) {
                pattern = decodeDecimalFormat(buffer, format);
            } else if (format instanceof TSimpleDateFormat) {
                pattern = decodeSimpleDateFormat(buffer, format);
            } else if (format != null) {
                throw new IllegalArgumentException("Unknown format");
            }
            if (pattern != null) {
                boolean quote = false;
                int index = 0;
                int length = pattern.length();
                int count = 0;
                while (index < length) {
                    char ch = pattern.charAt(index++);
                    if (ch == '\'') {
                        quote = !quote;
                    }
                    if (!quote) {
                        if (ch == '{') {
                            count++;
                        }
                        if (ch == '}') {
                            if (count > 0) {
                                count--;
                            } else {
                                buffer.append("'}");
                                ch = '\'';
                            }
                        }
                    }
                    buffer.append(ch);
                }
            }
            buffer.append('}');
        }
        if (maxOffset + 1 < strings.length) {
            appendQuoted(buffer, strings[maxOffset + 1]);
        }
        return buffer.toString();
    }

    private void appendQuoted(StringBuffer buffer, String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            char ch = string.charAt(i);
            if (ch == '{' || ch == '}') {
                buffer.append('\'');
                buffer.append(ch);
                buffer.append('\'');
            } else {
                buffer.append(ch);
            }
        }
    }

    public static class Field extends TFormat.Field {
        public static final Field ARGUMENT = new Field("message argument field");

        protected Field(String fieldName) {
            super(fieldName);
        }
    }
}
