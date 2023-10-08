/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.junit;

import static org.teavm.junit.PropertyNames.SOURCE_DIRS;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.teavm.backend.wasm.WasmRuntimeType;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.backend.wasm.generate.DirectorySourceFileResolver;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.TeaVM;

abstract class BaseWebAssemblyPlatformSupport extends TestPlatformSupport<WasmTarget> {
    public BaseWebAssemblyPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    String getExtension() {
        return ".wasm";
    }

    protected abstract WasmRuntimeType getRuntimeType();

    @Override
    CompileResult compile(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<WasmTarget> configuration, File path) {
        Supplier<WasmTarget> targetSupplier = () -> {
            WasmTarget target = new WasmTarget();
            target.setRuntimeType(getRuntimeType());
            var sourceDirs = System.getProperty(SOURCE_DIRS);
            if (sourceDirs != null) {
                var dirs = new ArrayList<File>();
                for (var tokenizer = new StringTokenizer(sourceDirs, Character.toString(File.pathSeparatorChar));
                     tokenizer.hasMoreTokens();) {
                    var dir = new File(tokenizer.nextToken());
                    if (dir.isDirectory()) {
                        dirs.add(dir);
                    }
                }
                if (!dirs.isEmpty()) {
                    target.setSourceFileResolver(new DirectorySourceFileResolver(dirs));
                }
            }
            return target;
        };
        return compile(configuration, targetSupplier, TestNativeEntryPoint.class.getName(), path,
                ".wasm", null, additionalProcessing, baseName);
    }

    @Override
    boolean usesFileName() {
        return true;
    }
}
