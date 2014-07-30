package org.teavm.chromerdp;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface ChromeRDPExchange {
    void send(String message);

    void addListener(ChromeRDPExchangeListener listener);

    void removeListener(ChromeRDPExchangeListener listener);
}
