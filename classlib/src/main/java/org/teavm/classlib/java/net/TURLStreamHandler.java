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
import java.util.Objects;

public abstract class TURLStreamHandler {
    protected abstract TURLConnection openConnection(TURL u) throws IOException;

    protected void parseURL(TURL u, String str, int start, int end) {
        if (end < start || end < 0) {
            // Checks to ensure string index exception ahead of
            // security exception for compatibility.
            if (end <= Integer.MIN_VALUE + 1 && (start >= str.length() || start < 0)
                    || str.startsWith("//", start) && str.indexOf('/', start + 2) == -1) {
                throw new StringIndexOutOfBoundsException(end);
            }
            return;
        }
        String parseString = str.substring(start, end);
        end -= start;
        int fileIdx = 0;

        // Default is to use info from context
        String host = u.getHost();
        int port = u.getPort();
        String ref = u.getRef();
        String file = u.getPath();
        String query = u.getQuery();
        String authority = u.getAuthority();
        String userInfo = u.getUserInfo();

        int refIdx = parseString.indexOf('#', 0);
        if (parseString.startsWith("//") && !parseString.startsWith("////")) {
            int hostIdx = 2;
            int portIdx;
            port = -1;
            fileIdx = parseString.indexOf('/', hostIdx);
            int questionMarkIndex = parseString.indexOf('?', hostIdx);
            if ((questionMarkIndex != -1)
                    && ((fileIdx == -1) || (fileIdx > questionMarkIndex))) {
                fileIdx = questionMarkIndex;
            }
            if (fileIdx == -1) {
                fileIdx = end;
                // Use default
                file = "";
            }
            int hostEnd = fileIdx;
            if (refIdx != -1 && refIdx < fileIdx) {
                hostEnd = refIdx;
            }
            int userIdx = parseString.lastIndexOf('@', hostEnd);
            authority = parseString.substring(hostIdx, hostEnd);
            if (userIdx > -1) {
                userInfo = parseString.substring(hostIdx, userIdx);
                hostIdx = userIdx + 1;
            }

            portIdx = parseString.indexOf(':', userIdx == -1 ? hostIdx
                    : userIdx);
            int endOfIPv6Addr = parseString.indexOf(']');
            // if there are square braces, ie. IPv6 address, use last ':'
            if (endOfIPv6Addr != -1) {
                try {
                    if (parseString.length() > endOfIPv6Addr + 1) {
                        char c = parseString.charAt(endOfIPv6Addr + 1);
                        if (c == ':') {
                            portIdx = endOfIPv6Addr + 1;
                        } else {
                            portIdx = -1;
                        }
                    } else {
                        portIdx = -1;
                    }
                } catch (Exception e) {
                    // Ignored
                }
            }

            if (portIdx == -1 || portIdx > fileIdx) {
                host = parseString.substring(hostIdx, hostEnd);
            } else {
                host = parseString.substring(hostIdx, portIdx);
                String portString = parseString.substring(portIdx + 1, hostEnd);
                if (!portString.isEmpty()) {
                    port = Integer.parseInt(portString);
                }
            }
        }

        if (refIdx > -1) {
            ref = parseString.substring(refIdx + 1, end);
        }
        int fileEnd = refIdx == -1 ? end : refIdx;

        int queryIdx = parseString.lastIndexOf('?', fileEnd);
        boolean canonicalize = false;
        if (queryIdx > -1) {
            query = parseString.substring(queryIdx + 1, fileEnd);
            if (queryIdx == 0 && file != null) {
                if (file.equals("")) {
                    file = "/";
                } else if (file.startsWith("/")) {
                    canonicalize = true;
                }
                int last = file.lastIndexOf('/') + 1;
                file = file.substring(0, last);
            }
            fileEnd = queryIdx;
        } else
        // Don't inherit query unless only the ref is changed
        if (refIdx != 0) {
            query = null;
        }

        if (fileIdx > -1) {
            if (fileIdx < end && parseString.charAt(fileIdx) == '/') {
                file = parseString.substring(fileIdx, fileEnd);
            } else if (fileEnd > fileIdx) {
                if (file == null) {
                    file = "";
                } else if (file.equals("")) {
                    file = "/";
                } else if (file.startsWith("/")) {
                    canonicalize = true;
                }
                int last = file.lastIndexOf('/') + 1;
                if (last == 0) {
                    file = parseString.substring(fileIdx, fileEnd);
                } else {
                    file = file.substring(0, last) + parseString.substring(fileIdx, fileEnd);
                }
            }
        }
        if (file == null) {
            file = "";
        }

        if (host == null) {
            host = "";
        }

        if (canonicalize) {
            // modify file if there's any relative referencing
            file = canonicalizePath(file);
        }

        setURL(u, u.getProtocol(), host, port, authority, userInfo, file, query, ref);
    }

    private static String canonicalizePath(String path) {
        int dirIndex;

        while (true) {
            dirIndex = path.indexOf("/./");
            if (dirIndex < 0) {
                break;
            }
            path = path.substring(0, dirIndex + 1) + path.substring(dirIndex + 3);
        }

        if (path.endsWith("/.")) {
            path = path.substring(0, path.length() - 1);
        }

        while (true) {
            dirIndex = path.indexOf("/../");
            if (dirIndex < 0) {
                break;
            }
            if (dirIndex != 0) {
                path = path.substring(0, path.lastIndexOf('/', dirIndex - 1)) + path.substring(dirIndex + 3);
            } else {
                path = path.substring(3);
            }
        }

        if (path.endsWith("/..") && path.length() > 3) {
            path = path.substring(0, path.lastIndexOf('/', path.length() - 4) + 1);
        }
        return path;
    }

    @Deprecated
    protected void setURL(TURL u, String protocol, String host, int port, String file, String ref) {
        u.set(protocol, host, port, file, ref);
    }

    protected void setURL(TURL u, String protocol, String host, int port, String authority, String userInfo,
            String file, String query, String ref) {
        u.set(protocol, host, port, authority, userInfo, file, query, ref);
    }

    protected String toExternalForm(TURL url) {
        StringBuilder answer = new StringBuilder();
        answer.append(url.getProtocol());
        answer.append(':');
        String authority = url.getAuthority();
        if (authority != null && authority.length() > 0) {
            answer.append("//");
            answer.append(url.getAuthority());
        }

        String file = url.getFile();
        String ref = url.getRef();
        if (file != null) {
            answer.append(file);
        }
        if (ref != null) {
            answer.append('#');
            answer.append(ref);
        }
        return answer.toString();
    }

    protected boolean equals(TURL url1, TURL url2) {
        if (!sameFile(url1, url2)) {
            return false;
        }
        return Objects.equals(url1.getRef(), url2.getRef()) && Objects.equals(url1.getQuery(), url2.getQuery());
    }

    protected int getDefaultPort() {
        return -1;
    }

    protected int hashCode(TURL url) {
        return toExternalForm(url).hashCode();
    }

    protected boolean hostsEqual(TURL url1, TURL url2) {
        // Compare by name.
        String host1 = getHost(url1);
        String host2 = getHost(url2);
        if (host1 == null && host2 == null) {
            return true;
        }
        return host1 != null && host1.equalsIgnoreCase(host2);
    }

    protected boolean sameFile(TURL url1, TURL url2) {
        if (!Objects.equals(url1.getProtocol(), url2.getProtocol())
                || !Objects.equals(url1.getFile(), url2.getFile())) {
            return false;
        }
        if (!hostsEqual(url1, url2)) {
            return false;
        }
        int p1 = url1.getPort();
        if (p1 == -1) {
            p1 = getDefaultPort();
        }
        int p2 = url2.getPort();
        if (p2 == -1) {
            p2 = getDefaultPort();
        }
        return p1 == p2;
    }

    private static String getHost(TURL url) {
        String host = url.getHost();
        if ("file".equals(url.getProtocol()) && "".equals(host)) {
            host = "localhost";
        }
        return host;
    }
}
