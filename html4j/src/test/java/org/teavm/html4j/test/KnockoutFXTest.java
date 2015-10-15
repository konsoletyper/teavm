/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.html4j.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.java.html.BrwsrCtx;
import net.java.html.js.JavaScriptBody;
import org.netbeans.html.boot.spi.Fn;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.json.spi.JSONCall;
import org.netbeans.html.json.spi.Technology;
import org.netbeans.html.json.spi.Transfer;
import org.netbeans.html.json.tck.KnockoutTCK;
import org.netbeans.html.ko4j.KO4J;
import org.testng.Assert;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public final class KnockoutFXTest extends KnockoutTCK implements Transfer {
    private static Class<?> browserClass;
    private static Fn.Presenter browserContext;
    private KO4J ko4j = new KO4J();
    private final Map<String, Request> urlMap = new HashMap<>();

    public KnockoutFXTest() {
    }

    static synchronized ClassLoader getClassLoader() throws InterruptedException {
        while (browserClass == null) {
            KnockoutFXTest.class.wait();
        }
        return browserClass.getClassLoader();
    }

    public static synchronized void initialized(Class<?> browserCls) throws Exception {
        browserClass = browserCls;
        browserContext = Fn.activePresenter();
        KnockoutFXTest.class.notifyAll();
    }

    public static void initialized() throws Exception {
        Assert.assertSame(
            KnockoutFXTest.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
            "No special classloaders"
        );
        KnockoutFXTest.initialized(KnockoutFXTest.class);
        browserContext = Fn.activePresenter();
    }

    @Override
    public BrwsrCtx createContext() {
        KO4J ko4j = new KO4J();
        return Contexts.newBuilder()
                .register(Transfer.class, this, 1)
                .register(Technology.class, ko4j.knockout(), 1)
                .build();
    }

    @Override
    public Object createJSON(Map<String, Object> values) {
        Object json = createJSON();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            setProperty(json, entry.getKey(), entry.getValue());
        }
        return json;
    }

    @JavaScriptBody(args = {}, body = "return new Object();")
    private static native Object createJSON();
    @JavaScriptBody(args = { "json", "key", "value" }, body = "json[key] = value;")
    private static native void setProperty(Object json, String key, Object value);

    @Override
    @JavaScriptBody(args = { "s", "args" }, body =
        "var f = new Function(s); " +
        "return f.apply(null, args);"
    )
    public native Object executeScript(String s, Object[] args);

    @JavaScriptBody(args = {  }, body =
          "var h;" +
          "if (!!window && !!window.location && !!window.location.href)\n" +
          "  h = window.location.href;\n" +
          "else " +
          "  h = null;" +
         "return h;\n")
    private static native String findBaseURL();

    @Override
    public URI prepareURL(String content, String mimeType, String[] parameters) {
        try {
            String url = "http://localhost/dynamic/" + urlMap.size();
            urlMap.put(url, new Request(content, mimeType, parameters));
            return new URI(url);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean canFailWebSocketTest() {
        return true;
    }

    @Override
    public void extract(Object obj, String[] props, Object[] values) {
        ko4j.transfer().extract(obj, props, values);
    }

    @Override
    public Object toJSON(InputStream is) throws IOException {
        return ko4j.transfer().toJSON(is);
    }

    @Override
    public void loadJSON(JSONCall call) {
        if (call.isJSONP()) {
            throw new IllegalArgumentException("This mock does not support JSONP calls");
        }
        String url = call.composeURL(null);
        Request data = urlMap.get(url);
        if (data != null) {
            String content = data.content;
            for (int i = 0;; i++) {
                String find = "$" + i;
                int at = content.indexOf(find);
                if (at == -1) {
                    break;
                }
                String value = data.parameters[i];
                if (value.startsWith("http.header.")) {
                    String header = value.substring(12);
                    int line = call.getHeaders().indexOf(header);
                    int end = call.getHeaders().indexOf("\n", line);
                    if (line >= 0 && end > line) {
                        value = call.getHeaders().substring(line + header.length() + 1, end).trim();
                    }
                }
                if (value.equals("http.method")) {
                    value = call.getMethod();
                }
                if (value.equals("http.requestBody")) {
                    value = call.getMessage();
                }
                content = content.substring(0, at) + value + content.substring(at + find.length());
            }
            try {
                if (data.mimeType.equals("text/plain")) {
                    call.notifySuccess(content);
                } else {
                    call.notifySuccess(toJSON(new ByteArrayInputStream(content.getBytes())));
                }
            } catch (IOException e) {
                call.notifyError(e);
            }
        } else {
            call.notifyError(new IllegalStateException());
        }
    }

    private static final class Request {
        final String content;
        final String mimeType;
        final String[] parameters;

        public Request(String content, String mimeType, String[] parameters) {
            this.content = content;
            this.mimeType = mimeType;
            this.parameters = parameters;
        }
    }
}
