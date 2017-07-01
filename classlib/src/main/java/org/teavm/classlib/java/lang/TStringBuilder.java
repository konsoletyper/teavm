/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

public class TStringBuilder extends TAbstractStringBuilder implements TAppendable {
    public TStringBuilder(int capacity) {
        super(capacity);
    }

    public TStringBuilder() {
        super();
    }

    public TStringBuilder(TString value) {
        super(value);
    }

    public TStringBuilder(TCharSequence value) {
        super(value);
    }

    @Override
    public TStringBuilder append(TString string) {
        super.append(string);
        return this;
    }

    @Override
    public TStringBuilder append(int value) {
        super.append(value);
        return this;
    }

    @Override
    public TStringBuilder append(long value) {
        super.append(value);
        return this;
    }

    @Override
    public TStringBuilder append(float value) {
        super.append(value);
        return this;
    }

    @Override
    public TStringBuilder append(double value) {
        super.append(value);
        return this;
    }

    @Override
    public TStringBuilder append(char c) {
        super.append(c);
        return this;
    }

    @Override
    public TStringBuilder append(char[] chars, int offset, int len) {
        super.append(chars, offset, len);
        return this;
    }

    @Override
    public TStringBuilder append(char[] chars) {
        super.append(chars);
        return this;
    }

    @Override
    public TStringBuilder appendCodePoint(int codePoint) {
        super.appendCodePoint(codePoint);
        return this;
    }

    @Override
    public TStringBuilder append(TCharSequence s, int start, int end) {
        super.append(s, start, end);
        return this;
    }

    @Override
    public TStringBuilder append(TCharSequence s) {
        super.append(s);
        return this;
    }

    @Override
    public TStringBuilder append(TStringBuffer s) {
        super.append(s);
        return this;
    }

    @Override
    public TStringBuilder append(TObject obj) {
        super.append(obj);
        return this;
    }

    @Override
    public TStringBuilder append(boolean b) {
        super.append(b);
        return this;
    }

    @Override
    public TStringBuilder insert(int target, long value) {
        super.insert(target, value);
        return this;
    }

    @Override
    public TStringBuilder insert(int target, float value) {
        super.insert(target, value);
        return this;
    }

    @Override
    public TStringBuilder insert(int target, double value) {
        super.insert(target, value);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, TCharSequence s, int start, int end) {
        super.insert(index, s, start, end);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, TCharSequence s) {
        super.insert(index, s);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, int value) {
        super.insert(index, value);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, char[] chars, int offset, int len) {
        super.insert(index, chars, offset, len);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, TObject obj) {
        super.insert(index, obj);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, char[] chars) {
        super.insert(index, chars);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, boolean b) {
        super.insert(index, b);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, char c) {
        super.insert(index, c);
        return this;
    }

    @Override
    public TStringBuilder delete(int start, int end) {
        super.delete(start, end);
        return this;
    }

    @Override
    public TStringBuilder replace(int start, int end, TString str) {
        super.replace(start, end, str);
        return this;
    }

    @Override
    public TStringBuilder deleteCharAt(int index) {
        super.deleteCharAt(index);
        return this;
    }

    @Override
    public TStringBuilder insert(int index, TString string) {
        super.insert(index, string);
        return this;
    }

    @Override
    public TStringBuilder reverse() {
        super.reverse();
        return this;
    }
}
