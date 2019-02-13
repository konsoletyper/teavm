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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.teavm.classlib.java.net.impl.TDummyStreamHandler;
import org.teavm.classlib.java.net.impl.TXHRStreamHandler;

public final class TURL implements Serializable {
    private int hashCode;
    private String file;
    private String protocol;
    private String host;
    private int port = -1;
    private String authority;
    private transient String userInfo;
    private transient String path;
    private transient String query;
    private String ref;
    private static Map<String, TURLStreamHandler> streamHandlers = new HashMap<>();
    transient TURLStreamHandler strmHandler;
    private static TURLStreamHandlerFactory streamHandlerFactory;

    public static void setURLStreamHandlerFactory(TURLStreamHandlerFactory streamFactory) {
        Objects.requireNonNull(streamFactory);
        streamHandlers.clear();
        streamHandlerFactory = streamFactory;
    }

    public TURL(String spec) throws TMalformedURLException {
        this(null, spec, (TURLStreamHandler) null);
    }

    public TURL(TURL context, String spec) throws TMalformedURLException {
        this(context, spec, null);
    }

    public TURL(TURL context, String spec, TURLStreamHandler handler) throws TMalformedURLException {
        strmHandler = handler;

        if (spec == null) {
            throw new TMalformedURLException();
        }
        spec = spec.trim();

        // The spec includes a protocol if it includes a colon character
        // before the first occurrence of a slash character. Note that,
        // "protocol" is the field which holds this URLs protocol.
        int index;
        try {
            index = spec.indexOf(':');
        } catch (NullPointerException e) {
            throw new TMalformedURLException(e.toString());
        }
        int startIPv6Addr = spec.indexOf('[');
        if (index >= 0) {
            if (startIPv6Addr == -1 || index < startIPv6Addr) {
                protocol = spec.substring(0, index);
                // According to RFC 2396 scheme part should match
                // the following expression:
                // alpha *( alpha | digit | "+" | "-" | "." )
                char c = protocol.charAt(0);
                boolean valid = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
                for (int i = 1; valid && (i < protocol.length()); i++) {
                    c = protocol.charAt(i);
                    valid = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9')
                            || c == '+' || c == '-' || c == '.';
                }
                if (!valid) {
                    protocol = null;
                    index = -1;
                } else {
                    protocol = protocol.toLowerCase();
                }
            }
        }

        if (protocol != null) {
            // If the context was specified, and it had the same protocol
            // as the spec, then fill in the receiver's slots from the values
            // in the context but still allow them to be over-ridden later
            // by the values in the spec.
            if (context != null && protocol.equals(context.getProtocol())) {
                String cPath = context.getPath();
                if (cPath != null && cPath.startsWith("/")) {
                    set(protocol, context.getHost(), context.getPort(), context.getAuthority(), context.getUserInfo(),
                            cPath, context.getQuery(), null);
                }
                if (strmHandler == null) {
                    strmHandler = context.strmHandler;
                }
            }
        } else {
            // If the spec did not include a protocol, then the context
            // *must* be specified. Fill in the receiver's slots from the
            // values in the context, but still allow them to be over-ridden
            // by the values in the ("relative") spec.
            if (context == null) {
                throw new TMalformedURLException();
            }
            set(context.getProtocol(), context.getHost(), context.getPort(), context.getAuthority(),
                    context.getUserInfo(), context.getPath(), context.getQuery(), null);
            if (strmHandler == null) {
                strmHandler = context.strmHandler;
            }
        }

        // If the stream handler has not been determined, set it
        // to the default for the specified protocol.
        if (strmHandler == null) {
            setupStreamHandler();
            if (strmHandler == null) {
                throw new TMalformedURLException();
            }
        }

        // Let the handler parse the TURL. If the handler throws
        // any exception, throw MalformedURLException instead.
        //
        // Note: We want "index" to be the index of the start of the scheme
        // specific part of the TURL. At this point, it will be either
        // -1 or the index of the colon after the protocol, so we
        // increment it to point at either character 0 or the character
        // after the colon.
        try {
            strmHandler.parseURL(this, spec, ++index, spec.length());
        } catch (Exception e) {
            throw new TMalformedURLException(e.toString());
        }

        if (port < -1) {
            throw new TMalformedURLException();
        }
    }

    public TURL(String protocol, String host, String file) throws TMalformedURLException {
        this(protocol, host, -1, file, null);
    }

    public TURL(String protocol, String host, int port, String file) throws TMalformedURLException {
        this(protocol, host, port, file, null);
    }

    public TURL(String protocol, String host, int port, String file, TURLStreamHandler handler)
            throws TMalformedURLException {
        if (port < -1) {
            throw new TMalformedURLException();
        }

        if (host != null && host.contains(":") && host.charAt(0) != '[') {
            host = "[" + host + "]";
        }

        Objects.requireNonNull(protocol);

        this.protocol = protocol;
        this.host = host;
        this.port = port;

        // Set the fields from the arguments. Handle the case where the
        // passed in "file" includes both a file and a reference part.
        int index;
        index = file.indexOf("#", file.lastIndexOf("/"));
        if (index >= 0) {
            this.file = file.substring(0, index);
            ref = file.substring(index + 1);
        } else {
            this.file = file;
        }
        fixURL(false);

        // Set the stream handler for the TURL either to the handler
        // argument if it was specified, or to the default for the
        // receiver's protocol if the handler was null.
        if (handler == null) {
            setupStreamHandler();
            if (strmHandler == null) {
                throw new TMalformedURLException();
            }
        } else {
            strmHandler = handler;
        }
    }

    void fixURL(boolean fixHost) {
        int index;
        if (host != null && host.length() > 0) {
            authority = host;
            if (port != -1) {
                authority = authority + ":" + port;
            }
        }
        if (fixHost) {
            index = -1;
            if (host != null) {
                index = host.lastIndexOf('@');
            }
            if (index >= 0) {
                userInfo = host.substring(0, index);
                host = host.substring(index + 1);
            } else {
                userInfo = null;
            }
        }

        index = -1;
        if (file != null) {
            index = file.indexOf('?');
        }
        if (index >= 0) {
            query = file.substring(index + 1);
            path = file.substring(0, index);
        } else {
            query = null;
            path = file;
        }
    }

    protected void set(String protocol, String host, int port, String file, String ref) {
        if (this.protocol == null) {
            this.protocol = protocol;
        }
        this.host = host;
        this.file = file;
        this.port = port;
        this.ref = ref;
        hashCode = 0;
        fixURL(true);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        return strmHandler.equals(this, (TURL) o);
    }

    public boolean sameFile(TURL otherURL) {
        return strmHandler.sameFile(this, otherURL);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = strmHandler.hashCode(this);
        }
        return hashCode;
    }

    void setupStreamHandler() {
        // Check for a cached (previously looked up) handler for
        // the requested protocol.
        strmHandler = streamHandlers.get(protocol);
        if (strmHandler != null) {
            return;
        }

        // If there is a stream handler factory, then attempt to
        // use it to create the handler.
        if (streamHandlerFactory != null) {
            strmHandler = streamHandlerFactory.createURLStreamHandler(protocol);
            if (strmHandler != null) {
                streamHandlers.put(protocol, strmHandler);
                return;
            }
        }

        switch (protocol) {
            case "http":
            case "https":
                strmHandler = new TXHRStreamHandler();
                break;
            case "ftp":
                strmHandler = new TDummyStreamHandler(21);
                break;
            default:
                strmHandler = new TDummyStreamHandler(-1);
                break;
        }

    }

    public InputStream openStream() throws IOException {
        return openConnection().getInputStream();
    }

    public TURLConnection openConnection() throws IOException {
        return strmHandler.openConnection(this);
    }

    public TURI toURI() throws TURISyntaxException {
        return new TURI(toExternalForm());
    }

    @Override
    public String toString() {
        return toExternalForm();
    }

    public String toExternalForm() {
        if (strmHandler == null) {
            return "unknown protocol(" + protocol + ")://" + host + file;
        }
        return strmHandler.toExternalForm(this);
    }

    public String getFile() {
        return file;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRef() {
        return ref;
    }

    public String getQuery() {
        return query;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getAuthority() {
        return authority;
    }

    protected void set(String protocol, String host, int port, String authority, String userInfo, String path,
            String query, String ref) {
        String filePart = path;
        if (query != null && !query.isEmpty()) {
            filePart = filePart != null ? filePart + "?" + query : "?" + query;
        }
        set(protocol, host, port, filePart, ref);
        this.authority = authority;
        this.userInfo = userInfo;
        this.path = path;
        this.query = query;
    }

    public int getDefaultPort() {
        return strmHandler.getDefaultPort();
    }
}
