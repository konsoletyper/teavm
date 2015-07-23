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

import org.teavm.classlib.java.text.TAttributedCharacterIterator.Attribute;
import org.teavm.classlib.java.util.*;

public class TAttributedString {

    String text;

    TMap<TAttributedCharacterIterator.Attribute, TList<Range>> attributeMap;

    static class Range {
        int start;

        int end;

        Object value;

        Range(int s, int e, Object v) {
            start = s;
            end = e;
            value = v;
        }
    }

    static class AttributedIterator implements TAttributedCharacterIterator {

        private int begin;
        private int end;
        private int offset;

        private TAttributedString attrString;

        private THashSet<Attribute> attributesAllowed;

        AttributedIterator(TAttributedString attrString) {
            this.attrString = attrString;
            begin = 0;
            end = attrString.text.length();
            offset = 0;
        }

        AttributedIterator(TAttributedString attrString, TAttributedCharacterIterator.Attribute[] attributes, int begin,
                int end) {
            if (begin < 0 || end > attrString.text.length() || begin > end) {
                throw new IllegalArgumentException();
            }
            this.begin = begin;
            this.end = end;
            offset = begin;
            this.attrString = attrString;
            if (attributes != null) {
                THashSet<Attribute> set = new THashSet<>((attributes.length * 4 / 3) + 1);
                for (int i = attributes.length; --i >= 0;) {
                    set.add(attributes[i]);
                }
                attributesAllowed = set;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object clone() {
            try {
                AttributedIterator clone = (AttributedIterator) super.clone();
                if (attributesAllowed != null) {
                    clone.attributesAllowed = (THashSet<Attribute>) attributesAllowed.clone();
                }
                return clone;
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        @Override
        public char current() {
            if (offset == end) {
                return DONE;
            }
            return attrString.text.charAt(offset);
        }

        @Override
        public char first() {
            if (begin == end) {
                return DONE;
            }
            offset = begin;
            return attrString.text.charAt(offset);
        }

        @Override
        public int getBeginIndex() {
            return begin;
        }

        @Override
        public int getEndIndex() {
            return end;
        }

        @Override
        public int getIndex() {
            return offset;
        }

        private boolean inRange(Range range) {
            if (!(range.value instanceof TAnnotation)) {
                return true;
            }
            return range.start >= begin && range.start < end && range.end > begin && range.end <= end;
        }

        private boolean inRange(TList<Range> ranges) {
            TIterator<Range> it = ranges.iterator();
            while (it.hasNext()) {
                Range range = it.next();
                if (range.start >= begin && range.start < end) {
                    return !(range.value instanceof TAnnotation) || (range.end > begin && range.end <= end);
                } else if (range.end > begin && range.end <= end) {
                    return !(range.value instanceof TAnnotation) || (range.start >= begin && range.start < end);
                }
            }
            return false;
        }

        @Override
        public TSet<AttributedIterator.Attribute> getAllAttributeKeys() {
            if (begin == 0 && end == attrString.text.length() && attributesAllowed == null) {
                return attrString.attributeMap.keySet();
            }

            TSet<AttributedIterator.Attribute> result = new THashSet<>((attrString.attributeMap.size() * 4 / 3) + 1);
            TIterator<TMap.Entry<Attribute, TList<Range>>> it = attrString.attributeMap.entrySet().iterator();
            while (it.hasNext()) {
                TMap.Entry<Attribute, TList<Range>> entry = it.next();
                if (attributesAllowed == null || attributesAllowed.contains(entry.getKey())) {
                    TList<Range> ranges = entry.getValue();
                    if (inRange(ranges)) {
                        result.add(entry.getKey());
                    }
                }
            }
            return result;
        }

        private Object currentValue(TList<Range> ranges) {
            TIterator<Range> it = ranges.iterator();
            while (it.hasNext()) {
                Range range = it.next();
                if (offset >= range.start && offset < range.end) {
                    return inRange(range) ? range.value : null;
                }
            }
            return null;
        }

        @Override
        public Object getAttribute(TAttributedCharacterIterator.Attribute attribute) {
            if (attributesAllowed != null && !attributesAllowed.contains(attribute)) {
                return null;
            }
            TArrayList<Range> ranges = (TArrayList<Range>) attrString.attributeMap.get(attribute);
            if (ranges == null) {
                return null;
            }
            return currentValue(ranges);
        }

        @Override
        public TMap<Attribute, Object> getAttributes() {
            TMap<Attribute, Object> result = new THashMap<>((attrString.attributeMap.size() * 4 / 3) + 1);
            TIterator<TMap.Entry<Attribute, TList<Range>>> it = attrString.attributeMap.entrySet().iterator();
            while (it.hasNext()) {
                TMap.Entry<Attribute, TList<Range>> entry = it.next();
                if (attributesAllowed == null || attributesAllowed.contains(entry.getKey())) {
                    Object value = currentValue(entry.getValue());
                    if (value != null) {
                        result.put(entry.getKey(), value);
                    }
                }
            }
            return result;
        }

        @Override
        public int getRunLimit() {
            return getRunLimit(getAllAttributeKeys());
        }

        private int runLimit(TList<Range> ranges) {
            int result = end;
            TListIterator<Range> it = ranges.listIterator(ranges.size());
            while (it.hasPrevious()) {
                Range range = it.previous();
                if (range.end <= begin) {
                    break;
                }
                if (offset >= range.start && offset < range.end) {
                    return inRange(range) ? range.end : result;
                } else if (offset >= range.end) {
                    break;
                }
                result = range.start;
            }
            return result;
        }

        @Override
        public int getRunLimit(TAttributedCharacterIterator.Attribute attribute) {
            if (attributesAllowed != null && !attributesAllowed.contains(attribute)) {
                return end;
            }
            TArrayList<Range> ranges = (TArrayList<Range>) attrString.attributeMap.get(attribute);
            if (ranges == null) {
                return end;
            }
            return runLimit(ranges);
        }

        @Override
        public int getRunLimit(TSet<? extends Attribute> attributes) {
            int limit = end;
            TIterator<? extends Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                TAttributedCharacterIterator.Attribute attribute = it.next();
                int newLimit = getRunLimit(attribute);
                if (newLimit < limit) {
                    limit = newLimit;
                }
            }
            return limit;
        }

        @Override
        public int getRunStart() {
            return getRunStart(getAllAttributeKeys());
        }

        private int runStart(TList<Range> ranges) {
            int result = begin;
            TIterator<Range> it = ranges.iterator();
            while (it.hasNext()) {
                Range range = it.next();
                if (range.start >= end) {
                    break;
                }
                if (offset >= range.start && offset < range.end) {
                    return inRange(range) ? range.start : result;
                } else if (offset < range.start) {
                    break;
                }
                result = range.end;
            }
            return result;
        }

        @Override
        public int getRunStart(TAttributedCharacterIterator.Attribute attribute) {
            if (attributesAllowed != null && !attributesAllowed.contains(attribute)) {
                return begin;
            }
            TArrayList<Range> ranges = (TArrayList<Range>) attrString.attributeMap.get(attribute);
            if (ranges == null) {
                return begin;
            }
            return runStart(ranges);
        }

        @Override
        public int getRunStart(TSet<? extends Attribute> attributes) {
            int start = begin;
            TIterator<? extends Attribute> it = attributes.iterator();
            while (it.hasNext()) {
                TAttributedCharacterIterator.Attribute attribute = it.next();
                int newStart = getRunStart(attribute);
                if (newStart > start) {
                    start = newStart;
                }
            }
            return start;
        }

        @Override
        public char last() {
            if (begin == end) {
                return DONE;
            }
            offset = end - 1;
            return attrString.text.charAt(offset);
        }

        @Override
        public char next() {
            if (offset >= (end - 1)) {
                offset = end;
                return DONE;
            }
            return attrString.text.charAt(++offset);
        }

        @Override
        public char previous() {
            if (offset == begin) {
                return DONE;
            }
            return attrString.text.charAt(--offset);
        }

        @Override
        public char setIndex(int location) {
            if (location < begin || location > end) {
                throw new IllegalArgumentException();
            }
            offset = location;
            if (offset == end) {
                return DONE;
            }
            return attrString.text.charAt(offset);
        }
    }

    public TAttributedString(TAttributedCharacterIterator iterator) {
        if (iterator.getBeginIndex() > iterator.getEndIndex()) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = iterator.getBeginIndex(); i < iterator.getEndIndex(); i++) {
            buffer.append(iterator.current());
            iterator.next();
        }
        text = buffer.toString();
        TSet<TAttributedCharacterIterator.Attribute> attributes = iterator.getAllAttributeKeys();
        if (attributes == null) {
            return;
        }
        attributeMap = new THashMap<>((attributes.size() * 4 / 3) + 1);

        TIterator<Attribute> it = attributes.iterator();
        while (it.hasNext()) {
            TAttributedCharacterIterator.Attribute attribute = it.next();
            iterator.setIndex(0);
            while (iterator.current() != TCharacterIterator.DONE) {
                int start = iterator.getRunStart(attribute);
                int limit = iterator.getRunLimit(attribute);
                Object value = iterator.getAttribute(attribute);
                if (value != null) {
                    addAttribute(attribute, value, start, limit);
                }
                iterator.setIndex(limit);
            }
        }
    }

    private TAttributedString(TAttributedCharacterIterator iterator, int start, int end, TSet<Attribute> attributes) {
        if (start < iterator.getBeginIndex() || end > iterator.getEndIndex() || start > end) {
            throw new IllegalArgumentException();
        }

        if (attributes == null) {
            return;
        }

        StringBuilder buffer = new StringBuilder();
        iterator.setIndex(start);
        while (iterator.getIndex() < end) {
            buffer.append(iterator.current());
            iterator.next();
        }
        text = buffer.toString();
        attributeMap = new THashMap<>((attributes.size() * 4 / 3) + 1);

        TIterator<Attribute> it = attributes.iterator();
        while (it.hasNext()) {
            TAttributedCharacterIterator.Attribute attribute = it.next();
            iterator.setIndex(start);
            while (iterator.getIndex() < end) {
                Object value = iterator.getAttribute(attribute);
                int runStart = iterator.getRunStart(attribute);
                int limit = iterator.getRunLimit(attribute);
                if ((value instanceof TAnnotation && runStart >= start && limit <= end)
                        || (value != null && !(value instanceof TAnnotation))) {
                    addAttribute(attribute, value, (runStart < start ? start : runStart) - start, (limit > end ? end
                            : limit) - start);
                }
                iterator.setIndex(limit);
            }
        }
    }

    public TAttributedString(TAttributedCharacterIterator iterator, int start, int end) {
        this(iterator, start, end, iterator.getAllAttributeKeys());
    }

    public TAttributedString(TAttributedCharacterIterator iterator, int start, int end,
            TAttributedCharacterIterator.Attribute[] attributes) {
        this(iterator, start, end, new THashSet<>(TArrays.asList(attributes)));
    }

    public TAttributedString(String value) {
        if (value == null) {
            throw new NullPointerException();
        }
        text = value;
        attributeMap = new THashMap<>(11);
    }

    public TAttributedString(String value, TMap<? extends TAttributedCharacterIterator.Attribute, ?> attributes) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value.length() == 0 && !attributes.isEmpty()) {
            throw new IllegalArgumentException("Cannot add attributes to empty string");
        }
        text = value;
        attributeMap = new THashMap<>((attributes.size() * 4 / 3) + 1);
        TIterator<?> it = attributes.entrySet().iterator();
        while (it.hasNext()) {
            TMap.Entry<?, ?> entry = (TMap.Entry<?, ?>) it.next();
            TArrayList<Range> ranges = new TArrayList<>(1);
            ranges.add(new Range(0, text.length(), entry.getValue()));
            attributeMap.put((TAttributedCharacterIterator.Attribute) entry.getKey(), ranges);
        }
    }

    /**
     * Applies a given attribute to this string.
     *
     * @param attribute
     *            the attribute that will be applied to this string.
     * @param value
     *            the value of the attribute that will be applied to this
     *            string.
     * @throws IllegalArgumentException
     *             if the length of this attributed string is 0.
     * @throws NullPointerException
     *             if {@code attribute} is {@code null}.
     */
    public void addAttribute(TAttributedCharacterIterator.Attribute attribute, Object value) {
        if (null == attribute) {
            throw new NullPointerException();
        }
        if (text.length() == 0) {
            throw new IllegalArgumentException();
        }

        TList<Range> ranges = attributeMap.get(attribute);
        if (ranges == null) {
            ranges = new TArrayList<>(1);
            attributeMap.put(attribute, ranges);
        } else {
            ranges.clear();
        }
        ranges.add(new Range(0, text.length(), value));
    }

    /**
     * Applies a given attribute to the given range of this string.
     *
     * @param attribute
     *            the attribute that will be applied to this string.
     * @param value
     *            the value of the attribute that will be applied to this
     *            string.
     * @param start
     *            the start of the range where the attribute will be applied.
     * @param end
     *            the end of the range where the attribute will be applied.
     * @throws IllegalArgumentException
     *             if {@code start < 0}, {@code end} is greater than the length
     *             of this string, or if {@code start >= end}.
     * @throws NullPointerException
     *             if {@code attribute} is {@code null}.
     */
    public void addAttribute(TAttributedCharacterIterator.Attribute attribute, Object value, int start, int end) {
        if (null == attribute) {
            throw new NullPointerException();
        }
        if (start < 0 || end > text.length() || start >= end) {
            throw new IllegalArgumentException();
        }

        if (value == null) {
            return;
        }

        TList<Range> ranges = attributeMap.get(attribute);
        if (ranges == null) {
            ranges = new TArrayList<>(1);
            ranges.add(new Range(start, end, value));
            attributeMap.put(attribute, ranges);
            return;
        }
        TListIterator<Range> it = ranges.listIterator();
        while (it.hasNext()) {
            Range range = it.next();
            if (end <= range.start) {
                it.previous();
                break;
            } else if (start < range.end || (start == range.end && value.equals(range.value))) {
                Range r1 = null;
                Range r3;
                it.remove();
                r1 = new Range(range.start, start, range.value);
                r3 = new Range(end, range.end, range.value);

                while (end > range.end && it.hasNext()) {
                    range = it.next();
                    if (end <= range.end) {
                        if (end > range.start || (end == range.start && value.equals(range.value))) {
                            it.remove();
                            r3 = new Range(end, range.end, range.value);
                            break;
                        }
                    } else {
                        it.remove();
                    }
                }

                if (value.equals(r1.value)) {
                    if (value.equals(r3.value)) {
                        it.add(new Range(r1.start < start ? r1.start : start, r3.end > end ? r3.end : end, r1.value));
                    } else {
                        it.add(new Range(r1.start < start ? r1.start : start, end, r1.value));
                        if (r3.start < r3.end) {
                            it.add(r3);
                        }
                    }
                } else {
                    if (value.equals(r3.value)) {
                        if (r1.start < r1.end) {
                            it.add(r1);
                        }
                        it.add(new Range(start, r3.end > end ? r3.end : end, r3.value));
                    } else {
                        if (r1.start < r1.end) {
                            it.add(r1);
                        }
                        it.add(new Range(start, end, value));
                        if (r3.start < r3.end) {
                            it.add(r3);
                        }
                    }
                }
                return;
            }
        }
        it.add(new Range(start, end, value));
    }

    /**
     * Applies a given set of attributes to the given range of the string.
     *
     * @param attributes
     *            the set of attributes that will be applied to this string.
     * @param start
     *            the start of the range where the attribute will be applied.
     * @param end
     *            the end of the range where the attribute will be applied.
     * @throws IllegalArgumentException
     *             if {@code start < 0}, {@code end} is greater than the length
     *             of this string, or if {@code start >= end}.
     */
    public void addAttributes(TMap<? extends TAttributedCharacterIterator.Attribute, ?> attributes,
            int start, int end) {
        TIterator<?> it = attributes.entrySet().iterator();
        while (it.hasNext()) {
            TMap.Entry<?, ?> entry = (TMap.Entry<?, ?>) it.next();
            addAttribute((TAttributedCharacterIterator.Attribute) entry.getKey(), entry.getValue(), start, end);
        }
    }

    /**
     * Returns an {@code AttributedCharacterIterator} that gives access to the
     * complete content of this attributed string.
     *
     * @return the newly created {@code AttributedCharacterIterator}.
     */
    public TAttributedCharacterIterator getIterator() {
        return new AttributedIterator(this);
    }

    /**
     * Returns an {@code AttributedCharacterIterator} that gives access to the
     * complete content of this attributed string. Only attributes contained in
     * {@code attributes} are available from this iterator if they are defined
     * for this text.
     *
     * @param attributes
     *            the array containing attributes that will be in the new
     *            iterator if they are defined for this text.
     * @return the newly created {@code AttributedCharacterIterator}.
     */
    public TAttributedCharacterIterator getIterator(TAttributedCharacterIterator.Attribute[] attributes) {
        return new AttributedIterator(this, attributes, 0, text.length());
    }

    /**
     * Returns an {@code AttributedCharacterIterator} that gives access to the
     * contents of this attributed string starting at index {@code start} up to
     * index {@code end}. Only attributes contained in {@code attributes} are
     * available from this iterator if they are defined for this text.
     *
     * @param attributes
     *            the array containing attributes that will be in the new
     *            iterator if they are defined for this text.
     * @param start
     *            the start index of the iterator on the underlying text.
     * @param end
     *            the end index of the iterator on the underlying text.
     * @return the newly created {@code AttributedCharacterIterator}.
     */
    public TAttributedCharacterIterator getIterator(TAttributedCharacterIterator.Attribute[] attributes, int start,
            int end) {
        return new AttributedIterator(this, attributes, start, end);
    }
}
