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
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.json.spi.JSONCall;
import org.netbeans.html.json.spi.Technology;
import org.netbeans.html.json.spi.Transfer;
import org.netbeans.html.json.spi.WSTransfer;
import org.netbeans.html.json.tck.KnockoutTCK;
import org.netbeans.html.ko4j.KO4J;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.junit.WholeClassCompilation;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
@WholeClassCompilation
public final class KnockoutFXTest extends KnockoutTCK implements Transfer, WSTransfer<WSImpl> {
    private KO4J ko4j = new KO4J();
    private final Map<String, Request> urlMap = new HashMap<>();
    private final Map<String, WSImpl> wsUrlMap = new HashMap<>();

    public KnockoutFXTest() {
    }

    static Class<?>[] allTestClasses() {
        return testClasses();
    }

    @Override
    public BrwsrCtx createContext() {
        KO4J ko4j = new KO4J();
        return Contexts.newBuilder()
                .register(Transfer.class, this, 1)
                .register(Technology.class, ko4j.knockout(), 1)
                .register(WSTransfer.class, this, 1)
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
    @JavaScriptBody(args = { "s", "args" }, body = ""
        + "var f = new Function(s);"
        + "return f.apply(null, args);"
    )
    public native Object executeScript(String s, Object[] args);

    @Override
    public URI prepareURL(String content, String mimeType, String[] parameters) {
        try {
            boolean isWS = false;
            for (String param : parameters) {
                if (param.equals("protocol:ws")) {
                    isWS = true;
                    break;
                }
            }
            String url = "http://localhost/dynamic/" + urlMap.size();
            if (!isWS) {
                urlMap.put(url, new Request(content, mimeType, parameters));
                return new URI(url);
            } else {
                wsUrlMap.put(url, new WSImpl(url, content));
                return new URI(url);
            }
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
        String callback = call.isJSONP() ? "jsonpTest" + urlMap.size() : null;
        String url = call.composeURL(call.isJSONP() ? callback : null);
        if (callback != null) {
            defineCallback(callback, call);
        }

        int paramsIndex = url.indexOf('?');
        Map<String, String> parameters = new HashMap<>();
        if (paramsIndex >= 0) {
            int current = paramsIndex + 1;
            while (current < url.length()) {
                int next = url.indexOf('&', current);
                if (next == -1) {
                    next = url.length();
                }
                int eq = url.indexOf('=', current);
                String key = url.substring(current, eq);
                String value = url.substring(eq + 1, next);
                parameters.put(Window.decodeURI(key), Window.decodeURI(value));
                current = next + 1;
            }
            url = url.substring(0, paramsIndex);
        }

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
                } else if (value.equals("http.method")) {
                    value = call.getMethod();
                } else if (value.equals("http.requestBody")) {
                    value = call.getMessage();
                } else {
                    value = parameters.get(value);
                }
                content = content.substring(0, at) + value + content.substring(at + find.length());
            }

            if (call.isJSONP()) {
                String scriptContent = content;
                HTMLElement script = HTMLDocument.current().createElement("script")
                        .withText(scriptContent)
                        .withAttr("id", "jsonp-" + callback)
                        .withAttr("type", "text/javascript");
                HTMLDocument.current().getBody().appendChild(script);
            } else {
                try {
                    if (data.mimeType.equals("text/plain")) {
                        call.notifySuccess(content);
                    } else {
                        call.notifySuccess(toJSON(new ByteArrayInputStream(content.getBytes())));
                    }
                } catch (IOException e) {
                    call.notifyError(e);
                }
            }
        } else {
            call.notifyError(new IllegalStateException());
        }
    }

    private static void defineCallback(String name, JSONCall call) {
        defineFunction(name, data -> {
            removeFunction(name);
            HTMLDocument.current().getElementById("jsonp-" + name).delete();
            call.notifySuccess(data);
        });
    }

    @Override
    public WSImpl open(String url, JSONCall callback) {
        WSImpl impl = wsUrlMap.get(url);
        if (impl != null) {
            impl.callback = callback;
            callback.notifySuccess(null);
        } else {
            callback.notifyError(null);
        }
        return impl;
    }

    @Override
    public void send(WSImpl socket, JSONCall data) {
        String content = socket.content;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '$') {
                int digit = content.charAt(++i) - '0';
                if (digit == 0) {
                    sb.append(data.getMessage());
                } else {
                    sb.append('$').append(content.charAt(i));
                }
            } else {
                sb.append(content.charAt(i));
            }
        }
        socket.callback.notifySuccess(eval("(" + sb + ")"));
    }

    @Override
    public void close(WSImpl socket) {
        wsUrlMap.remove(socket.url);
        socket.callback.notifyError(null);
    }

    @JSBody(params = { "name", "callback" }, script = "window[name] = callback;")
    private static native void defineFunction(String name, JSONPCallback callback);

    @JSBody(params = "name", script = "delete window[name];")
    private static native void removeFunction(String name);

    @JSFunctor
    interface JSONPCallback extends JSObject {
        void dataReceived(JSObject dataReceived);
    }

    @JSBody(params = "text", script = "return eval(text);")
    private static native JSObject eval(String text);

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
