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
package org.teavm.cache;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

/**
 *
 * @author Alexey Andreev
 */
public class ClassPathClassDateProvider implements ClassDateProvider {
    private ClassLoader classLoader;

    public ClassPathClassDateProvider(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Date getModificationDate(String className) {
        URL url = classLoader.getResource(className);
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file")) {
            try {
                File file = new File(url.toURI());
                return file.exists() ? new Date(file.lastModified()) : null;
            } catch (URISyntaxException e) {
                // If URI is invalid, we just report that class should be reparsed
                return null;
            }
        } else if (url.getProtocol().equals("jar")) {
            int exclIndex = url.getPath().indexOf('!');
            String jarFileName = exclIndex >= 0 ? url.getPath().substring(0, exclIndex) : url.getPath();
            File file = new File(jarFileName);
            return file.exists() ? new Date(file.lastModified()) : null;
        } else {
            return null;
        }
    }
}
