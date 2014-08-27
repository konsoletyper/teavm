package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class FileNameIterator {
    private DebugInformation debugInformation;
    private int index;

    FileNameIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
    }

    public boolean isEndReached() {
        return index < debugInformation.fileMapping.size();
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return DebugInformation.key(debugInformation.fileMapping.get(index));
    }

    public int getFileNameId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return debugInformation.fileMapping.get(index).get(2);
    }

    public String getFileName() {
        int fileNameId = getFileNameId();
        return fileNameId >= 0 ? debugInformation.getFileName(fileNameId) : null;
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        ++index;
    }
}
