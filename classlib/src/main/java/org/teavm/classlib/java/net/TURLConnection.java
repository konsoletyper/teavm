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

package org.teavm.classlib.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TURLConnection {
    protected TURL url;
    private static boolean defaultAllowUserInteraction;
    private static boolean defaultUseCaches = true;
    private long lastModified = -1;
    protected long ifModifiedSince;
    protected boolean useCaches = defaultUseCaches;
    protected boolean connected;
    protected boolean doOutput;
    protected boolean doInput = true;
    protected boolean allowUserInteraction = defaultAllowUserInteraction;
    private int readTimeout;
    private int connectTimeout;
    private Map<String, List<String>> requestProperties = new HashMap<>();

    protected TURLConnection(TURL url) {
        this.url = url;
    }

    public abstract void connect() throws IOException;

    public boolean getAllowUserInteraction() {
        return allowUserInteraction;
    }

    public String getContentEncoding() {
        return getHeaderField("Content-Encoding");
    }

    public int getContentLength() {
        return getHeaderFieldInt("Content-Length", -1);
    }

    public String getContentType() {
        return getHeaderField("Content-Type");
    }

    public long getDate() {
        return getHeaderFieldDate("Date", 0);
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    @Deprecated
    public static String getDefaultRequestProperty(String field) {
        return null;
    }

    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }

    public boolean getDoInput() {
        return doInput;
    }

    public boolean getDoOutput() {
        return doOutput;
    }

    public long getExpiration() {
        return getHeaderFieldDate("Expires", 0);
    }

    public String getHeaderField(int pos) {
        return null;
    }

    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    public Map<String, List<String>> getRequestProperties() {
        if (connected) {
            throw new IllegalStateException();
        }

        HashMap<String, List<String>> map = new HashMap<>();
        for (String key : requestProperties.keySet()) {
            map.put(key, Collections.unmodifiableList(requestProperties.get(key)));
        }
        return Collections.unmodifiableMap(map);
    }

    public void addRequestProperty(String field, String newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        if (field == null) {
            throw new NullPointerException();
        }

        List<String> valuesList = requestProperties.get(field);
        if (valuesList == null) {
            valuesList = new ArrayList<String>();
            valuesList.add(0, newValue);
            requestProperties.put(field, valuesList);
        } else {
            valuesList.add(0, newValue);
        }
    }

    public String getHeaderField(String key) {
        return null;
    }

    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String field, long defaultValue) {
        String date = getHeaderField(field);
        if (date == null) {
            return defaultValue;
        }
        try {
            return Date.parse(date);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getHeaderFieldInt(String field, int defaultValue) {
        try {
            return Integer.parseInt(getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getHeaderFieldKey(int posn) {
        return null;
    }

    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    public InputStream getInputStream() throws IOException {
        throw new TUnknownServiceException();
    }

    public long getLastModified() {
        if (lastModified == -1) {
            lastModified = getHeaderFieldDate("Last-Modified", 0);
        }
        return lastModified;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new TUnknownServiceException();
    }

    public String getRequestProperty(String field) {
        if (connected) {
            throw new IllegalStateException();
        }
        List<String> valuesList = requestProperties.get(field);
        if (valuesList == null) {
            return null;
        }
        return valuesList.get(0);
    }

    public TURL getURL() {
        return url;
    }

    public boolean getUseCaches() {
        return useCaches;
    }

    private String parseTypeString(String typeString) {
        StringBuilder typeStringBuffer = new StringBuilder(typeString);
        for (int i = 0; i < typeStringBuffer.length(); i++) {
            // if non-alphanumeric, replace it with '_'
            char c = typeStringBuffer.charAt(i);
            if (!(Character.isLetter(c) || Character.isDigit(c) || c == '.')) {
                typeStringBuffer.setCharAt(i, '_');
            }
        }
        return typeStringBuffer.toString();
    }

    public void setAllowUserInteraction(boolean newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        this.allowUserInteraction = newValue;
    }

    public static void setDefaultAllowUserInteraction(boolean allows) {
        defaultAllowUserInteraction = allows;
    }

    @Deprecated
    public static void setDefaultRequestProperty(String field, String value) {
    }

    public void setDefaultUseCaches(boolean newValue) {
        if (connected) {
            throw new IllegalAccessError();
        }
        defaultUseCaches = newValue;
    }

    public void setDoInput(boolean newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        this.doInput = newValue;
    }

    public void setDoOutput(boolean newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        this.doOutput = newValue;
    }

    public void setIfModifiedSince(long newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        this.ifModifiedSince = newValue;
    }

    public void setRequestProperty(String field, String newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        if (field == null) {
            throw new NullPointerException();
        }

        List<String> valuesList = new ArrayList<String>();
        valuesList.add(newValue);
        requestProperties.put(field, valuesList);
    }

    public void setUseCaches(boolean newValue) {
        if (connected) {
            throw new IllegalStateException();
        }
        this.useCaches = newValue;
    }

    public void setConnectTimeout(int timeout) {
        if (0 > timeout) {
            throw new IllegalArgumentException();
        }
        this.connectTimeout = timeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setReadTimeout(int timeout) {
        if (0 > timeout) {
            throw new IllegalArgumentException();
        }
        this.readTimeout = timeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public String toString() {
        return getClass().getName() + ":" + url.toString();
    }
}
