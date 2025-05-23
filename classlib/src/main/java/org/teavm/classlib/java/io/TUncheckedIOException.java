/*
 *  Copyright 2023 Jonathan Coates.
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
package org.teavm.classlib.java.io;

import java.io.IOException;

public class TUncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = 1645785175445590213L;

    public TUncheckedIOException(IOException cause) {
        super(cause);
    }

    public TUncheckedIOException(String message, IOException cause) {
        super(message, cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
