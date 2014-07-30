package org.teavm.chromerdp;

import java.io.IOException;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface ChromeRDPExchangeListener {
    void received(String message) throws IOException;
}
