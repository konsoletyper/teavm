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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileSystem;
import org.teavm.classlib.fs.VirtualFileSystemProvider;
import org.teavm.classlib.java.util.TRandom;

public class TFile implements Serializable, Comparable<TFile> {
    private String path;

    public static final char separatorChar = fs().isWindows() ? '\\' : '/';
    public static final String separator = String.valueOf(separatorChar);
    public static final char pathSeparatorChar = fs().isWindows() ? ';' : ':';
    public static final String pathSeparator = String.valueOf(pathSeparatorChar);

    private static int counter;

    public TFile(TFile dir, String name) {
        Objects.requireNonNull(name);
        path = dir == null ? fixSlashes(name) : calculatePath(dir.getPath(), name);
    }

    public TFile(String path) {
        Objects.requireNonNull(path);
        this.path = fixSlashes(path);
    }

    public TFile(String dir, String name) {
        Objects.requireNonNull(name);
        path = dir == null ? fixSlashes(name) : calculatePath(dir, name);
    }

    public TFile(URI uri) {
        // check pre-conditions
        checkURI(uri);
        String path = uri.getPath();
        if (fs().isWindows() && path.startsWith("/") && path.length() >= 4) {
            if (isDriveLetter(path.charAt(1)) && path.charAt(2) == ':' && path.charAt(3) == '/') {
                path = path.substring(1);
            }
        }
        this.path = fixSlashes(path);
    }

    private void checkURI(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException();
        } else if (!uri.getRawSchemeSpecificPart().startsWith("/")) {
            throw new IllegalArgumentException();
        }

        String temp = uri.getScheme();
        if (temp == null || !temp.equals("file")) {
            throw new IllegalArgumentException();
        }

        temp = uri.getRawPath();
        if (temp == null || temp.length() == 0) {
            throw new IllegalArgumentException();
        }

        if (uri.getRawAuthority() != null) {
            throw new IllegalArgumentException();
        }

        if (uri.getRawQuery() != null) {
            throw new IllegalArgumentException();
        }

        if (uri.getRawFragment() != null) {
            throw new IllegalArgumentException();
        }
    }

    public boolean canRead() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null && virtualFile.canRead();
    }

    public boolean canWrite() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null && virtualFile.canWrite();
    }

    @Override
    public int compareTo(TFile o) {
        return path.compareTo(o.path);
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        int separatorIndex = path.lastIndexOf(separator);
        return separatorIndex < 0 ? path : path.substring(separatorIndex + 1, path.length());
    }

    private static VirtualFileSystem fs() {
        return VirtualFileSystemProvider.getInstance();
    }

    public String getAbsolutePath() {
        if (isAbsolute()) {
            return path;
        }

        String userdir = fs().getUserDir();

        if (path.isEmpty()) {
            return userdir;
        }
        int length = userdir.length();

        StringBuilder result = new StringBuilder(userdir);
        if (userdir.charAt(length - 1) != separatorChar) {
            if (path.charAt(0) != separatorChar) {
                result.append(separator);
            }
        } else if (fs().isWindows() && path.charAt(0) == separatorChar) {
            result.setLength(3);
        }
        result.append(path);

        return result.toString();
    }

    public TFile getAbsoluteFile() {
        return new TFile(getAbsolutePath());
    }

    public boolean isAbsolute() {
        return isAbsolutePath(path);
    }

    private boolean isAbsolutePath(String path) {
        if (fs().isWindows()) {
            if (path.length() < 3) {
                return false;
            }
            return isDriveLetter(path.charAt(0)) && path.charAt(1) == ':' && path.charAt(2) == '\\';
        } else {
            return !path.isEmpty() && path.charAt(0) == separatorChar;
        }
    }

    private static boolean isDriveLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public boolean isDirectory() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null && virtualFile.isDirectory();
    }

    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public boolean isFile() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null && virtualFile.isFile();
    }

    public String getCanonicalPath() throws IOException {
        return fs().canonicalize(getCanonicalPathImpl());
    }

    private String getCanonicalPathImpl() {
        String result = getAbsolutePath();

        int numSeparators = 1;
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == separatorChar) {
                numSeparators++;
            }
        }
        int[] sepLocations = new int[numSeparators];
        int rootLoc = fs().isWindows() ? 2 : 0;
        char[] newResult = new char[result.length() + 1];
        int newLength = 0;
        int lastSlash = 0;
        int foundDots = 0;
        sepLocations[lastSlash] = rootLoc;
        for (int i = 0; i <= result.length(); i++) {
            if (i < rootLoc) {
                newResult[newLength++] = result.charAt(i);
            } else {
                if (i == result.length() || result.charAt(i) == separatorChar) {
                    if (i == result.length() && foundDots == 0) {
                        break;
                    }
                    if (foundDots == 1) {
                        /* Don't write anything, just reset and continue */
                        foundDots = 0;
                        continue;
                    }
                    if (foundDots > 1) {
                        /* Go back N levels */
                        lastSlash = lastSlash > (foundDots - 1) ? lastSlash - (foundDots - 1) : 0;
                        newLength = sepLocations[lastSlash] + 1;
                        foundDots = 0;
                        continue;
                    }
                    sepLocations[++lastSlash] = newLength;
                    newResult[newLength++] = separatorChar;
                    continue;
                }
                if (result.charAt(i) == '.') {
                    foundDots++;
                    continue;
                }
                /* Found some dots within text, write them out */
                if (foundDots > 0) {
                    for (int j = 0; j < foundDots; j++) {
                        newResult[newLength++] = (byte) '.';
                    }
                }
                newResult[newLength++] = result.charAt(i);
                foundDots = 0;
            }
        }
        // remove trailing slash
        if (newLength > (rootLoc + 1) && newResult[newLength - 1] == separatorChar) {
            newLength--;
        }
        return new String(newResult, 0, newLength);
    }

    public TFile getCanonicalFile() throws IOException {
        return new TFile(getCanonicalPath());
    }

    public String getParent() {
        int length = path.length();
        int index = path.lastIndexOf(separatorChar);
        if (index == -1 || path.charAt(length - 1) == separatorChar) {
            return null;
        }
        if (path.indexOf(separatorChar) == index && (isAbsolutePath(path) || index == 0)) {
            return path.substring(0, index + 1);
        }
        return path.substring(0, index);
    }

    public TFile getParentFile() {
        String path = getParent();
        return path != null ? new TFile(getParent()) : null;
    }

    public static TFile[] listRoots() {
        return new TFile[] { new TFile("/") };
    }

    public String[] list() {
        VirtualFile virtualFile = findVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.listFiles();
    }

    public String[] list(TFilenameFilter filter) {
        String[] result = list();
        if (result == null) {
            return null;
        }
        int j = 0;
        for (String name : result) {
            if (filter.accept(this, name)) {
                result[j++] = name;
            }
        }
        if (j < result.length) {
            result = Arrays.copyOf(result, j);
        }
        return result;
    }

    public TFile[] listFiles() {
        VirtualFile virtualFile = findVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        String[] entries = virtualFile.listFiles();
        if (entries == null) {
            return null;
        }
        TFile[] files = new TFile[entries.length];
        for (int i = 0; i < entries.length; ++i) {
            files[i] = new TFile(this, entries[i]);
        }

        return files;
    }

    public TFile[] listFiles(TFileFilter filter) {
        TFile[] result = listFiles();
        if (result == null) {
            return null;
        }
        int j = 0;
        for (TFile file : result) {
            if (filter.accept(file)) {
                result[j++] = file;
            }
        }
        if (j < result.length) {
            result = Arrays.copyOf(result, j);
        }
        return result;
    }

    public TFile[] listFiles(TFilenameFilter filter) {
        TFile[] result = listFiles();
        if (result == null) {
            return null;
        }
        int j = 0;
        for (TFile file : result) {
            if (filter.accept(this, file.getName())) {
                result[j++] = file;
            }
        }
        if (j < result.length) {
            result = Arrays.copyOf(result, j);
        }
        return result;
    }

    public boolean exists() {
        VirtualFile virtualFile = findVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        return virtualFile.isDirectory() || virtualFile.isFile();
    }

    public long lastModified() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null ? virtualFile.lastModified() : 0;
    }

    public boolean setLastModified(long time) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        VirtualFile file = findVirtualFile();
        if (file == null) {
            return false;
        }

        return file.setLastModified(time);
    }

    public boolean setReadOnly() {
        VirtualFile file = findVirtualFile();
        if (file == null) {
            return false;
        }
        return file.setReadOnly(true);
    }

    public boolean setWritable(boolean writable) {
        VirtualFile file = findVirtualFile();
        return file.setReadOnly(!writable);
    }

    public long length() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile != null && virtualFile.isFile() ? virtualFile.length() : 0;
    }

    public boolean createNewFile() throws IOException {
        VirtualFile parentVirtualFile = findParentFile();
        if (parentVirtualFile == null) {
            throw new IOException("Can't create file " + getPath() + " since parent directory does not exist");
        }

        return parentVirtualFile.createFile(getName());
    }

    public boolean mkdir() {
        VirtualFile virtualFile = findParentFile();
        return virtualFile != null && virtualFile.createDirectory(getName());
    }

    public boolean mkdirs() {
        String path = getCanonicalPathImpl();

        if (path.indexOf(separatorChar) < 0) {
            return false;
        }
        int i = path.length();

        do {
            VirtualFile file = fs().getFile(path.substring(0, i));
            if (file.isDirectory()) {
                break;
            } else if (file.isFile()) {
                return false;
            }

            i = path.lastIndexOf(separatorChar, i - 1);
        } while (i >= 0);

        i++;

        while (i < path.length()) {
            int next = path.indexOf(separatorChar, i);
            if (next < 0) {
                next = path.length();
            }
            if (next == i + 1) {
                break;
            }

            VirtualFile file = fs().getFile(path.substring(0, i));
            if (!file.createDirectory(path.substring(i, next))) {
                return false;
            }
            i = next + 1;
        }

        return true;
    }

    public boolean delete() {
        VirtualFile virtualFile = findVirtualFile();
        return virtualFile.delete();
    }

    public void deleteOnExit() {
        // Do nothing
    }

    public boolean renameTo(TFile dest) {
        VirtualFile targetDir = dest.findParentFile();
        if (targetDir == null) {
            return false;
        }

        VirtualFile virtualFile = findVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        return targetDir.adopt(virtualFile, dest.getName());
    }

    public URI toURI() {
        String name = getAbsoluteName();
        try {
            if (!name.startsWith("/")) {
                // start with sep.
                return new URI("file", null, "/" + name, null, null);
            } else if (name.startsWith("//")) {
                return new URI("file", "", name, null);
            }
            return new URI("file", null, name, null, null);
        } catch (URISyntaxException e) {
            // this should never happen
            return null;
        }
    }

    private String getAbsoluteName() {
        TFile f = getAbsoluteFile();
        String name = f.getPath();

        if (f.isDirectory() && name.charAt(name.length() - 1) != separatorChar) {
            // Directories must end with a slash
            name = new StringBuilder(name.length() + 1).append(name).append('/').toString();
        }
        if (fs().isWindows()) { // Must convert slashes.
            name = name.replace(separatorChar, '/');
        }
        return name;
    }

    public static TFile createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    public static TFile createTempFile(String prefix, String suffix, TFile directory) throws IOException {
        // Force a prefix null check first
        if (prefix.length() < 3) {
            throw new IllegalArgumentException();
        }
        String newSuffix = suffix == null ? ".tmp" : suffix;
        TFile tmpDirFile;
        if (directory == null) {
            String tmpDir = System.getProperty("java.io.tmpdir", ".");
            tmpDirFile = new TFile(tmpDir);
            tmpDirFile.mkdirs();
        } else {
            tmpDirFile = directory;
        }
        TFile result;
        do {
            result = genTempFile(prefix, newSuffix, tmpDirFile);
        } while (!result.createNewFile());
        return result;
    }

    private static TFile genTempFile(String prefix, String suffix, TFile directory) {
        int identify;
        if (counter == 0) {
            int newInt = new TRandom().nextInt();
            counter = ((newInt / 65535) & 0xFFFF) + 0x2710;
        }
        identify = counter++;

        StringBuilder newName = new StringBuilder();
        newName.append(prefix);
        newName.append(counter);
        newName.append(identify);
        newName.append(suffix);
        return new TFile(directory, newName.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TFile)) {
            return false;
        }
        return fs().isWindows()
            ? path.equalsIgnoreCase(((File) obj).getPath())
            : path.equals(((File) obj).getPath());
    }

    @Override
    public int hashCode() {
        return fs().isWindows() ? path.toLowerCase().hashCode() : path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }

    private static String fixSlashes(String origPath) {
        int uncIndex = 0;
        int length = origPath.length();
        int newLength = 0;

        if (fs().isWindows() && length == 3) {
            if (isDriveLetter(origPath.charAt(0)) && origPath.charAt(1) == ':'
                && (origPath.charAt(2) == '/' || origPath.charAt(2) == '\\')) {
                return origPath.substring(0, 2) + "\\";
            }
        }

        boolean foundSlash = false;
        char[] newPath = origPath.toCharArray();
        for (int i = 0; i < length; i++) {
            char pathChar = newPath[i];
            if (pathChar == '/' || pathChar == separatorChar) {
                if (!foundSlash || i == uncIndex) {
                    newPath[newLength++] = separatorChar;
                    foundSlash = true;
                }
            } else {
                newPath[newLength++] = pathChar;
                foundSlash = false;
            }
        }
        if (foundSlash && (newLength > uncIndex + 1 || newLength == 2 && newPath[0] != '/')) {
            newLength--;
        }

        return new String(newPath, 0, newLength);
    }

    private static String calculatePath(String dirPath, String name) {
        dirPath = fixSlashes(dirPath);
        if (!name.isEmpty() || dirPath.isEmpty()) {
            name = fixSlashes(name);

            int separatorIndex = 0;
            while (separatorIndex < name.length() && name.charAt(separatorIndex) == separatorChar) {
                separatorIndex++;
            }
            if (separatorIndex > 0) {
                name = name.substring(separatorIndex);
            }

            if (!dirPath.isEmpty() && dirPath.charAt(dirPath.length() - 1) == separatorChar) {
                return dirPath + name;
            }
            return dirPath + separatorChar + name;
        }

        return dirPath;
    }

    VirtualFile findVirtualFile() {
        return fs().getFile(getCanonicalPathImpl());
    }

    VirtualFile findParentFile() {
        String path = getCanonicalPathImpl();
        if (path.isEmpty() || path.equals("/")) {
            return null;
        }
        return new TFile(path).getParentFile().findVirtualFile();
    }

    private boolean isRoot(String path) {
        if (fs().isWindows()) {
            return path.length() == 3 && isAbsolutePath(path);
        } else {
            return path.equals("/");
        }
    }
}
