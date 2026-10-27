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

package org.teavm.classlib.java.util.zip;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;
import java.util.Arrays;
import org.teavm.classlib.java.lang.TAutoCloseable;

public class TInflater implements TAutoCloseable {
    private boolean finished;
    private boolean nowrap;
    int inLength;
    int inRead;
    private boolean needsDictionary;
    // The zlib header's two bytes, kept as inflate consumes them. jzlib does not expose them.
    private final byte[] header = new byte[2];
    private Inflater impl;

    public TInflater() {
        this(false);
    }

    public TInflater(boolean noHeader) {
        nowrap = noHeader;
        try {
            impl = new Inflater(noHeader);
        } catch (GZIPException e) {
            // Do nothing
        }
    }

    public void end() {
        inRead = 0;
        inLength = 0;
        impl = null;
    }

    public boolean finished() {
        return finished;
    }

    public int getAdler() {
        if (impl == null) {
            throw new IllegalStateException();
        }
        return (int) impl.getAdler();
    }

    private native int getAdlerImpl(long handle);

    public long getBytesRead() {
        if (impl == null) {
            throw new IllegalStateException();
        }
        return impl.getTotalIn();
    }

    public long getBytesWritten() {
        if (impl == null) {
            throw new IllegalStateException();
        }
        return impl.getTotalOut();
    }

    public int getRemaining() {
        return inLength - inRead;
    }

    public int getTotalIn() {
        return (int) getBytesRead();
    }

    private native long getTotalInImpl(long handle);

    public int getTotalOut() {
        return (int) getBytesWritten();
    }

    private native long getTotalOutImpl(long handle);

    public int inflate(byte[] buf) throws TDataFormatException {
        return inflate(buf, 0, buf.length);
    }

    public int inflate(byte[] buf, int off, int nbytes) throws TDataFormatException {
        // avoid int overflow, check null buf
        if (off > buf.length || nbytes < 0 || off < 0 || buf.length - off < nbytes) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (impl == null) {
            throw new IllegalStateException();
        }

        // As in the JDK, 0 means "call needsInput() or needsDictionary()". jzlib reports an error for both:
        // before any setInput, and again for each call until setDictionary.
        if (needsDictionary || impl.next_in == null) {
            return 0;
        }

        long lastInSize = impl.total_in;
        long lastOutSize = impl.total_out;
        impl.setOutput(buf, off, nbytes);

        byte[] inArray = impl.next_in;
        int inIndex = impl.next_in_index;
        int errCode = impl.inflate(0);
        if (!nowrap && lastInSize < 2) {
            // Keep the header bytes as inflate consumes them, which may take more than one call.
            int upTo = (int) Math.min(2, impl.total_in);
            for (int k = (int) lastInSize; k < upTo; k++) {
                header[k] = inArray[inIndex + k - (int) lastInSize];
            }
            // jzlib accepts a header that fails the FCHECK test (CMF * 256 + FLG a multiple of 31) when its method
            // is deflate. It does check the method and the window size, as zlib does.
            if (upTo == 2 && (((header[0] & 0xFF) << 8) | (header[1] & 0xFF)) % 31 != 0) {
                throw new TDataFormatException();
            }
        }
        switch (errCode) {
            case JZlib.Z_OK:
                break;
            case JZlib.Z_NEED_DICT:
                needsDictionary = true;
                break;
            case JZlib.Z_STREAM_END:
                finished = true;
                break;
            case JZlib.Z_BUF_ERROR:
                // Zlib reports "no progress possible" this way. The JDK's Inflater returns 0 bytes for it, not an
                // error.
                break;
            default:
                throw new TDataFormatException("Error occurred: " + errCode);
        }

        inRead += impl.total_in - lastInSize;
        return (int) (impl.total_out - lastOutSize);
    }

    public boolean needsDictionary() {
        return needsDictionary;
    }

    public boolean needsInput() {
        return inRead == inLength;
    }

    public void reset() {
        if (impl == null) {
            throw new NullPointerException();
        }
        finished = false;
        needsDictionary = false;
        inLength = 0;
        inRead = 0;
        impl.init(nowrap);
    }

    private native void resetImpl(long handle);

    public void setDictionary(byte[] buf) {
        setDictionary(buf, 0, buf.length);
    }

    public void setDictionary(byte[] buf, int off, int nbytes) {
        if (impl == null) {
            throw new IllegalStateException();
        }
        // avoid int overflow, check null buf
        if (off <= buf.length && nbytes >= 0 && off >= 0 && buf.length - off >= nbytes) {
            if (off > 0) {
                buf = Arrays.copyOfRange(buf, off, buf.length);
            }
            impl.setDictionary(buf, nbytes);
            needsDictionary = false;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
    public void setInput(byte[] buf) {
        setInput(buf, 0, buf.length);
    }

    public void setInput(byte[] buf, int off, int nbytes) {
        if (impl == null) {
            throw new IllegalStateException();
        }
        // avoid int overflow, check null buf
        if (off <= buf.length && nbytes >= 0 && off >= 0 && buf.length - off >= nbytes) {
            inRead = 0;
            inLength = nbytes;
            impl.setInput(buf, off, nbytes, false);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public void close() throws Exception {
        end();
    }
}
