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

public abstract class THttpURLConnection extends TURLConnection {
    private String[] methodTokens = { "GET", "DELETE", "HEAD", "OPTIONS", "POST", "PUT", "TRACE" };
    protected String method = "GET";
    protected int responseCode = -1;
    protected String responseMessage;
    protected boolean instanceFollowRedirects = followRedirects;
    private static boolean followRedirects = true;
    protected int chunkLength = -1;
    protected int fixedContentLength = -1;
    private final static int DEFAULT_CHUNK_LENGTH = 1024;

    public final static int HTTP_ACCEPTED = 202;
    public final static int HTTP_BAD_GATEWAY = 502;
    public final static int HTTP_BAD_METHOD = 405;
    public final static int HTTP_BAD_REQUEST = 400;
    public final static int HTTP_CLIENT_TIMEOUT = 408;
    public final static int HTTP_CONFLICT = 409;
    public final static int HTTP_CREATED = 201;
    public final static int HTTP_ENTITY_TOO_LARGE = 413;
    public final static int HTTP_FORBIDDEN = 403;
    public final static int HTTP_GATEWAY_TIMEOUT = 504;
    public final static int HTTP_GONE = 410;
    public final static int HTTP_INTERNAL_ERROR = 500;
    public final static int HTTP_LENGTH_REQUIRED = 411;
    public final static int HTTP_MOVED_PERM = 301;
    public final static int HTTP_MOVED_TEMP = 302;
    public final static int HTTP_MULT_CHOICE = 300;
    public final static int HTTP_NO_CONTENT = 204;
    public final static int HTTP_NOT_ACCEPTABLE = 406;
    public final static int HTTP_NOT_AUTHORITATIVE = 203;
    public final static int HTTP_NOT_FOUND = 404;
    public final static int HTTP_NOT_IMPLEMENTED = 501;
    public final static int HTTP_NOT_MODIFIED = 304;
    public final static int HTTP_OK = 200;
    public final static int HTTP_PARTIAL = 206;
    public final static int HTTP_PAYMENT_REQUIRED = 402;
    public final static int HTTP_PRECON_FAILED = 412;
    public final static int HTTP_PROXY_AUTH = 407;
    public final static int HTTP_REQ_TOO_LONG = 414;
    public final static int HTTP_RESET = 205;
    public final static int HTTP_SEE_OTHER = 303;
    @Deprecated
    public final static int HTTP_SERVER_ERROR = 500;
    public final static int HTTP_USE_PROXY = 305;
    public final static int HTTP_UNAUTHORIZED = 401;
    public final static int HTTP_UNSUPPORTED_TYPE = 415;
    public final static int HTTP_UNAVAILABLE = 503;
    public final static int HTTP_VERSION = 505;

    protected THttpURLConnection(TURL url) {
        super(url);
    }

    public abstract void disconnect();

    public InputStream getErrorStream() {
        return null;
    }

    public static boolean getFollowRedirects() {
        return followRedirects;
    }

    public String getRequestMethod() {
        return method;
    }

    public int getResponseCode() throws IOException {
        connect();
        return responseCode;
    }

    public String getResponseMessage() throws IOException {
        connect();
        return responseMessage;
    }

    public static void setFollowRedirects(boolean auto) {
        followRedirects = auto;
    }

    public void setRequestMethod(String method) throws TProtocolException {
        if (connected) {
            throw new TProtocolException();
        }
        for (int i = 0; i < methodTokens.length; i++) {
            if (methodTokens[i].equals(method)) {
                // if there is a supported method that matches the desired
                // method, then set the current method and return
                this.method = methodTokens[i];
                return;
            }
        }
        throw new TProtocolException();
    }

    public boolean getInstanceFollowRedirects() {
        return instanceFollowRedirects;
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        instanceFollowRedirects = followRedirects;
    }

    @Override
    public long getHeaderFieldDate(String field, long defaultValue) {
        return super.getHeaderFieldDate(field, defaultValue);
    }

    public void setFixedLengthStreamingMode(int contentLength) {
        if (super.connected) {
            throw new IllegalStateException();
        }
        if (0 < chunkLength) {
            throw new IllegalStateException();
        }
        if (0 > contentLength) {
            throw new IllegalArgumentException();
        }
        this.fixedContentLength = contentLength;
    }

    public void setChunkedStreamingMode(int chunklen) {
        if (super.connected) {
            throw new IllegalStateException();
        }
        if (0 <= fixedContentLength) {
            throw new IllegalStateException();
        }
        if (0 >= chunklen) {
            chunkLength = DEFAULT_CHUNK_LENGTH;
        } else {
            chunkLength = chunklen;
        }
    }
}
