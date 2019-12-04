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
package org.teavm.debugging.information;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class URLDebugInformationProvider implements DebugInformationProvider {
    private String baseURL;

    public URLDebugInformationProvider(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public DebugInformation getDebugInformation(String script) {
        try {
            URI uri = new URI(baseURL + script);
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath() + ".teavmdbg", uri.getQuery(), uri.getFragment());
            try (InputStream input = uri.toURL().openStream()) {
                return DebugInformation.read(input);
            }
        } catch (IOException | URISyntaxException e) {
            return null;
        }
    }
}
