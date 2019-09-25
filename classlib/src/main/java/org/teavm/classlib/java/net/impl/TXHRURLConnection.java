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
package org.teavm.classlib.java.net.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.classlib.java.net.THttpURLConnection;
import org.teavm.classlib.java.net.TURL;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

public class TXHRURLConnection extends THttpURLConnection {
    private XMLHttpRequest xhr;
    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    private ByteArrayInputStream errorStream;
    private Map<String, String> responseHeaders = new HashMap<>();
    private String[] responseHeaderKeys;
    private String[] responseHeaderValues;
    private Map<String, List<String>> headerFields = new HashMap<>();
    private boolean requestPerformed;

    public TXHRURLConnection(TURL url) {
        super(url);
    }

    @Override
    public void disconnect() {
        connected = false;
        xhr = null;
        responseHeaders = null;
        responseHeaderKeys = null;
        responseHeaderValues = null;
        inputStream = null;
        outputStream = null;
        errorStream = null;
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }

        xhr = XMLHttpRequest.create();
        xhr.open(method, url.toString());
        for (Map.Entry<String, List<String>> entry : getRequestProperties().entrySet()) {
            for (String value : entry.getValue()) {
                xhr.setRequestHeader(entry.getKey(), value);
            }
        }
        xhr.setResponseType("arraybuffer");
        connected = true;
    }

    private void performRequestIfNecessary() {
        if (!requestPerformed) {
            requestPerformed = true;
            performRequest();
        }
    }

    @Async
    private native Boolean performRequest();
    private void performRequest(AsyncCallback<Boolean> callback) {
        xhr.setOnReadyStateChange(() -> {
            if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                return;
            }

            responseCode = xhr.getStatus();
            responseMessage = xhr.getStatusText();
            if (responseCode == 0) {
                responseCode = -1;
            }

            Int8Array array = Int8Array.create((ArrayBuffer) xhr.getResponse());
            byte[] bytes = new byte[array.getLength()];
            for (int i = 0; i < bytes.length; ++i) {
                bytes[i] = array.get(i);
            }
            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

            parseHeaders(xhr.getAllResponseHeaders());

            int responseGroup = responseCode / 100;
            if (responseGroup == 4 || responseGroup == 5) {
                errorStream = stream;
                inputStream = null;
            } else {
                inputStream = stream;
                errorStream = null;
            }
            callback.complete(Boolean.TRUE);
        });

        if (outputStream != null) {
            byte[] bytes = outputStream.toByteArray();
            Int8Array array = Int8Array.create(bytes.length);
            for (int i = 0; i < bytes.length; ++i) {
                array.set(i, bytes[i]);
            }
            xhr.send(array.getBuffer());
        } else {
            xhr.send();
        }
    }

    private void parseHeaders(String headers) {
        int index = 0;
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        responseHeaders = new HashMap<>();
        headerFields = new HashMap<>();
        while (index < headers.length()) {
            int next = headers.indexOf("\r\n", index);
            if (next < 0) {
                next = headers.length();
            }

            int colon = headers.indexOf(':', index);
            if (colon < 0) {
                colon = headers.length();
            }
            String key = colon < next ? headers.substring(index, colon) : headers.substring(index, next);
            String value = colon < next ? headers.substring(colon + 1, next).trim() : "";
            key = key.trim();
            keys.add(key);
            values.add(value);

            List<String> headerFieldValues = headerFields.get(key);
            if (headerFieldValues == null) {
                headerFieldValues = new ArrayList<>();
                headerFields.put(key, headerFieldValues);
            }
            headerFieldValues.add(value);

            key = key.toLowerCase();
            responseHeaders.put(key, value);

            index = next + 2;
        }

        responseHeaderKeys = keys.toArray(new String[keys.size()]);
        responseHeaderValues = values.toArray(new String[values.size()]);
    }

    @Override
    public String getHeaderFieldKey(int posn) {
        performRequestIfNecessary();
        if (responseHeaderKeys == null || posn >= responseHeaderKeys.length) {
            return null;
        }
        return responseHeaderKeys[posn];
    }

    @Override
    public String getHeaderField(int pos) {
        performRequestIfNecessary();
        if (responseHeaderValues == null || pos >= responseHeaderValues.length) {
            return null;
        }
        return responseHeaderValues[pos];
    }

    @Override
    public String getHeaderField(String key) {
        performRequestIfNecessary();
        return responseHeaders != null ? responseHeaders.get(key.toLowerCase()) : null;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        performRequestIfNecessary();

        int responseGroup = responseCode / 100;
        if (responseGroup == 4 || responseGroup == 5) {
            inputStream = new ByteArrayInputStream(new byte[0]);
            throw new IOException("HTTP status: " + responseCode + " " + responseMessage);
        }

        return inputStream;
    }

    @Override
    public int getResponseCode() throws IOException {
        connect();
        performRequestIfNecessary();
        return super.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        connect();
        performRequestIfNecessary();
        return super.getResponseMessage();
    }

    @Override
    public InputStream getErrorStream() {
        return errorStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new ByteArrayOutputStream();
        }
        return outputStream;
    }
}
