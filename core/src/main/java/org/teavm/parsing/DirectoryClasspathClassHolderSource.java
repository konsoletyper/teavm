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

package org.teavm.parsing;

import java.io.File;
import java.util.Properties;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.parsing.resource.DirectoryResourceReader;
import org.teavm.parsing.resource.MapperClassHolderSource;
import org.teavm.parsing.resource.ResourceClassHolderMapper;

public class DirectoryClasspathClassHolderSource implements ClassHolderSource {
    private MapperClassHolderSource innerClassSource;
    private ClasspathResourceMapper classPathMapper;

    public DirectoryClasspathClassHolderSource(File baseDir, Properties properties) {
        DirectoryResourceReader reader = new DirectoryResourceReader(baseDir);
        ResourceClassHolderMapper rawMapper = new ResourceClassHolderMapper(reader);
        classPathMapper = new ClasspathResourceMapper(properties, rawMapper);
        innerClassSource = new MapperClassHolderSource(classPathMapper);
    }

    public DirectoryClasspathClassHolderSource(File baseDir) {
        this(baseDir, new Properties());
    }

    @Override
    public ClassHolder get(String name) {
        return innerClassSource.get(name);
    }
}
