/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.nio.file.impl;

import java.io.IOException;
import java.util.Iterator;
import org.teavm.classlib.java.net.TURI;
import org.teavm.classlib.java.nio.file.TLinkOption;
import org.teavm.classlib.java.nio.file.TPath;

public class TDefaultPath implements TPath {
    private TDefaultFileSystem fs;
    public final String pathString;
    private int[] segments;

    public TDefaultPath(TDefaultFileSystem fs, String pathString) {
        this.fs = fs;
        this.pathString = pathString;
    }

    @Override
    public TDefaultFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        if (!fs.vfs.isWindows()) {
            return pathString.startsWith("/");
        } else {
            if (pathString.length() < 3) {
                return false;
            }
            var c = Character.toUpperCase(pathString.charAt(0));
            if (c < 'A' || c > 'Z') {
                return false;
            }
            return pathString.charAt(1) == ':' && pathString.charAt(2) == '\\';
        }
    }

    @Override
    public TDefaultPath getRoot() {
        if (!isAbsolute()) {
            return null;
        }
        if (fs.vfs.isWindows()) {
            return new TDefaultPath(fs, pathString.substring(0, 2));
        } else {
            return new TDefaultPath(fs, "/");
        }
    }

    @Override
    public TDefaultPath getFileName() {
        var sep = fs.getSeparatorChar();
        var index = pathString.lastIndexOf(sep);
        if (index == 0 && pathString.length() == 1) {
            return null;
        }
        return index >= 0 ? new TDefaultPath(fs, pathString.substring(index + 1)) : this;
    }

    @Override
    public TDefaultPath getParent() {
        var sep = fs.getSeparatorChar();
        var index = pathString.lastIndexOf(sep);
        if (index == 0) {
            return pathString.length() == 1 ? null : new TDefaultPath(fs, pathString.substring(0, 1));
        }
        return index > 0 ? new TDefaultPath(fs, pathString.substring(0, index)) : null;
    }

    @Override
    public int getNameCount() {
        initSegments();
        return segments.length - 1;
    }

    @Override
    public TPath getName(int index) {
        return subpath(index, index + 1);
    }

    @Override
    public TPath subpath(int beginIndex, int endIndex) {
        if (beginIndex >= endIndex || beginIndex < 0) {
            throw new IllegalArgumentException();
        }
        initSegments();
        if (endIndex > segments.length - 1) {
            throw new IllegalArgumentException();
        }
        if (beginIndex == 0 && endIndex == segments.length) {
            return this;
        }
        return new TDefaultPath(fs, pathString.substring(segments[beginIndex] + 1, segments[endIndex]));
    }

    @Override
    public boolean startsWith(TPath other) {
        if (!(other instanceof TDefaultPath)) {
            return false;
        }
        var otherPath = (TDefaultPath) other;
        if (otherPath.pathString.isEmpty()) {
            return pathString.isEmpty();
        }
        if (fs.vfs.isWindows()) {
            if (pathString.length() <= otherPath.pathString.length()
                    && pathString.regionMatches(true, 0, otherPath.pathString, 0, otherPath.pathString.length())) {
                return false;
            }
        } else {
            if (!pathString.startsWith(otherPath.pathString)) {
                return false;
            }
        }
        if (pathString.length() == otherPath.pathString.length()) {
            return true;
        }
        var sep = fs.getSeparatorChar();
        return otherPath.pathString.charAt(otherPath.pathString.length() - 1) == sep
                || pathString.charAt(otherPath.pathString.length()) == sep;
    }

    @Override
    public boolean endsWith(TPath other) {
        if (!(other instanceof TDefaultPath)) {
            return false;
        }
        var otherPath = (TDefaultPath) other;
        if (fs.vfs.isWindows()) {
            if (pathString.length() <= otherPath.pathString.length()
                    && pathString.regionMatches(true, pathString.length() - otherPath.pathString.length(),
                    otherPath.pathString, 0, otherPath.pathString.length())) {
                return false;
            }
        } else {
            if (!pathString.endsWith(otherPath.pathString)) {
                return false;
            }
        }
        return pathString.length() == otherPath.pathString.length()
                || pathString.charAt(pathString.length() - otherPath.pathString.length() - 1) == fs.getSeparatorChar();
    }

    @Override
    public TDefaultPath normalize() {
        var result = new char[pathString.length()];
        var resultIndex = 0;
        initSegments();
        var resultSegments = new int[this.segments.length];
        var resultSegmentIndex = 0;
        var abs = isAbsolute();
        if (abs) {
            pathString.getChars(0, segments[0], result, 0);
            resultIndex = segments[0];
        }
        var initialLength = resultIndex;
        for (var i = 0; i < segments.length - 1; ++i) {
            var start = segments[i] + 1;
            var end = segments[i + 1];
            if (start + 2 == end && pathString.charAt(start) == '.' && pathString.charAt(start + 1) == '.') {
                if (resultSegmentIndex > 0) {
                    resultIndex = resultSegments[--resultSegmentIndex];
                } else if (!abs) {
                    start = Math.max(0, start - 1);
                    pathString.getChars(start, end, result, resultIndex);
                    resultIndex += end - start;
                }
            } else if (start + 1 != end || pathString.charAt(start) != '.') {
                resultSegments[resultSegmentIndex++] = resultIndex;
                pathString.getChars(start - 1, end, result, resultIndex);
                resultIndex += end - start + 1;
            }
        }
        if (resultIndex == pathString.length()) {
            return this;
        }
        if (abs && initialLength == resultIndex) {
            result[resultIndex++] = fs.getSeparatorChar();
        }
        return new TDefaultPath(fs, new String(result, 0, resultIndex));
    }

    @Override
    public TPath resolve(TPath other) {
        if (other.isAbsolute()) {
            return other;
        }
        if (other.getNameCount() == 1 && other.getFileName().toString().isEmpty()) {
            return this;
        }
        if (other instanceof TDefaultPath) {
            return new TDefaultPath(fs, pathString + fs.getSeparatorChar() + ((TDefaultPath) other).pathString);
        }
        var sb = new StringBuilder(pathString);
        var count = other.getNameCount();
        for (var i = 0; i < count; ++i) {
            sb.append(fs.getSeparatorChar());
            sb.append(other.getName(i).toString());
        }
        return new TDefaultPath(fs, sb.toString());
    }

    @Override
    public TDefaultPath relativize(TPath other) {
        if (isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException();
        }
        if (other.equals(this)) {
            return new TDefaultPath(fs, "");
        }
        var otherPath = (TDefaultPath) other;
        initSegments();
        otherPath.initSegments();
        var count = Math.min(otherPath.segments.length, segments.length) - 1;
        var index = 0;
        for (; index < count; ++index) {
            if (segments[index] != otherPath.segments[index]
                    || segments[index + 1] != otherPath.segments[index + 1]) {
                break;
            }
            if (!pathString.regionMatches(fs.vfs.isWindows(), segments[index] + 1, otherPath.pathString,
                    segments[index] + 1, segments[index + 1] - segments[index] - 1)) {
                break;
            }
        }
        var sb = new StringBuilder();
        var sep = fs.getSeparatorChar();
        for (var i = index + 1; i < segments.length; ++i) {
            sb.append("..").append(sep);
        }
        sb.append(otherPath.pathString, otherPath.segments[index] + 1, otherPath.pathString.length());
        return new TDefaultPath(fs, sb.toString());
    }

    @Override
    public TDefaultPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        return new TDefaultPath(fs, toAbsolutePathString());
    }

    private String toAbsolutePathString() {
        var userdir = fs.vfs.getUserDir();

        if (pathString.isEmpty()) {
            return userdir;
        }
        int length = userdir.length();

        var separatorChar = fs.getSeparatorChar();
        var result = new StringBuilder(userdir);
        if (userdir.charAt(length - 1) != separatorChar) {
            if (pathString.charAt(0) != separatorChar) {
                result.append(separatorChar);
            }
        } else if (fs.vfs.isWindows() && pathString.charAt(0) == separatorChar) {
            result.setLength(3);
        }
        result.append(pathString);

        return result.toString();
    }

    @Override
    public TURI toUri() {
        return new TURI("file", null, toAbsolutePathString(), null, null);
    }

    @Override
    public TPath toRealPath(TLinkOption... options) throws IOException {
        var pathString = toAbsolutePath().normalize().pathString;
        return new TDefaultPath(fs, fs.vfs.canonicalize(pathString));
    }

    @Override
    public int compareTo(TPath o) {
        var other = (TDefaultPath) o;
        if (this.fs != other.fs) {
            throw new ClassCastException();
        }
        return fs.vfs.isWindows()
                ? pathString.compareToIgnoreCase(other.pathString)
                : pathString.compareTo(other.pathString);
    }

    @Override
    public Iterator<TPath> iterator() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return false;
        }
        if (!(obj instanceof TDefaultPath)) {
            return false;
        }
        var other = (TDefaultPath) obj;
        if (fs != other.fs) {
            return false;
        }
        return fs.vfs.isWindows()
            ? pathString.equalsIgnoreCase(other.pathString)
            : pathString.equals(other.pathString);
    }

    @Override
    public int hashCode() {
        return fs.vfs.isWindows() ? pathString.toLowerCase().hashCode() : pathString.hashCode();
    }

    @Override
    public String toString() {
        return pathString;
    }

    private void initSegments() {
        if (segments == null) {
            var sep = fs.getSeparatorChar();
            var count = 1;
            var startIndex = 0;
            if (!pathString.isEmpty() && pathString.charAt(0) == sep) {
                ++startIndex;
                if (startIndex == pathString.length()) {
                    segments = new int[] { 0 };
                    return;
                }
            }

            var index = startIndex;
            while (true) {
                var next = pathString.indexOf(sep, index);
                if (next < 0) {
                    break;
                }
                ++count;
                index = next + 1;
            }

            var result = new int[count + 1];
            var resultIndex = 0;
            segments = result;
            index = startIndex;
            result[resultIndex++] = startIndex - 1;
            while (true) {
                var next = pathString.indexOf(sep, index);
                if (next < 0) {
                    break;
                }
                result[resultIndex++] = next;
                index = next + 1;
            }
            result[resultIndex] = pathString.length();
        }
    }
}
