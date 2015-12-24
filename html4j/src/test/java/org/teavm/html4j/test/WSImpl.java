package org.teavm.html4j.test;

import org.netbeans.html.json.spi.JSONCall;

/**
 *
 * @author Alexey Andreev
 */
class WSImpl {
    String url;
    JSONCall callback;
    String content;

    public WSImpl(String url, String content) {
        this.url = url;
        this.content = content;
    }
}
