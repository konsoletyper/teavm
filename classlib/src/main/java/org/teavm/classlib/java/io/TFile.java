/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.io;

/**
 *
 * @author bora
 */
public class TFile {

    public static final TFile ROOT = new TFile("");

    public static final char separatorChar = '/';

    public static final String separator = String.valueOf(separatorChar);

    public static final char pathSeparatorChar = ':';

    public static final String pathSeparator = "" + pathSeparatorChar;

    TFile parent;
    String name;
    boolean absolute;

    public TFile(String pathname) {
        while (pathname.endsWith(separator) && pathname.length() > 0) {
            pathname = pathname.substring(0, pathname.length() - 1);
        }

        int cut = pathname.lastIndexOf(separatorChar);
        if (cut == -1) {
            name = pathname;
        } else if (cut == 0) {
            name = pathname.substring(cut);
            parent = name.length()==0 ? null : ROOT;
        } else {
            name = pathname.substring(cut + 1);
            parent = new TFile(pathname.substring(0, cut));
        }
    }

    public TFile(String parent, String child) {
        this(new TFile(parent), child);
    }

    public TFile(TFile parent, String child) {
        this.parent = parent;
        this.name = child;
    }
    
    public String getName() {
        return name;
    }

    public String getParent() {
        return parent == null ? "" : parent.getPath();
    }

    public TFile getParentFile() {
        return parent;
    }

    public String getPath() {
        return parent == null ? name
                : (parent.getPath() + separatorChar + name);
    }

    private boolean isRoot() {
        return name.length()==0 && parent == null;
    }

    public boolean isAbsolute() {
        if (isRoot()) {
            return true;
        }
        if (parent == null) {
            return false;
        }
        return parent.isAbsolute();
    }

    public String getAbsolutePath() {
        String path = getAbsoluteFile().getPath();
        return path.length() == 0 ? "/" : path;
    }

    public TFile getAbsoluteFile() {
        if (isAbsolute()) {
            return this;
        }
        if (parent == null) {
            return new TFile(ROOT, name);
        }
        return new TFile(parent.getAbsoluteFile(), name);
    }

    public String getCanonicalPath() {
        return getCanonicalFile().getAbsolutePath();
    }

    public TFile getCanonicalFile() {
        TFile cParent = parent == null ? null : parent.getCanonicalFile();
        if (name.equals(".")) {
            return cParent == null ? ROOT : cParent;
        }
        if (cParent != null && cParent.name.length()==0) {
            cParent = null;
        }
        if (name.equals("..")) {
            if (cParent == null) {
                return ROOT;
            }
            if (cParent.parent == null) {
                return ROOT;
            }
            return cParent.parent;
        }
        if (cParent == null && name.length()>0) {
            return new TFile(ROOT, name);
        }
        return new TFile(cParent, name);
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return true;
    }

    public boolean exists() {
        return true;
    }

    public boolean isDirectory() {
        return false;
    }

    public boolean isFile() {

        return false;

    }

    public boolean isHidden() {
        return false;
    }

    public long lastModified() {
        return 0;
    }

    public long length() {

        return 0;

    }

    public boolean createNewFile() throws TIOException {
        if (exists())
            return false;
        if (!parent.exists())
            return false;

        return true;
    }

    public boolean delete() {

        if (!exists()) {
            return false;
        }

        return true;

    }

    public void deleteOnExit() {
        throw new RuntimeException("not implemented");
    }

    public String[] list() {
        throw new RuntimeException("not implemented");
    }

    public TFile[] listFiles() {
        return listFiles(null);
    }

    public TFile[] listFiles(TFilenameFilter filter) {

        return null;
    }

    public boolean mkdir() {

        if (parent != null && !parent.exists()) {
            return false;
        }
        if (exists()) {
            return false;
        }

        return true;

    }

    public boolean mkdirs() {
        if (parent != null) {
            parent.mkdirs();
        }
        return mkdir();
    }

    public boolean renameTo(TFile dest) {
        throw new RuntimeException("renameTo()");
    }

    public boolean setLastModified(long time) {
        return false;
    }

    public boolean setReadOnly() {
        return false;
    }

    public static TFile[] listRoots() {
        return new TFile[] { ROOT };
    }

    public static TFile createTempFile(String prefix, String suffix,
            TFile directory) throws TIOException {
        throw new RuntimeException("not implemented");
    }

    public static TFile createTempFile(String prefix, String suffix)
            throws TIOException {
        throw new RuntimeException("not implemented");
    }

    public int compareTo(TFile pathname) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TFile)) {
            return false;
        }
        return getPath().equals(((TFile) obj).getPath());
    }

    @Override
    public int hashCode() {
        return parent != null ? parent.hashCode() + name.hashCode() : name
                .hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}