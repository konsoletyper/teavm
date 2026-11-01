/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.junit.spi;

public final class ExternalCompilationResult {
    private static final ExternalCompilationResult SUCCESS = new ExternalCompilationResult(null);
    private final String errorMessage;

    private ExternalCompilationResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static ExternalCompilationResult success() {
        return SUCCESS;
    }

    public static ExternalCompilationResult failure(String errorMessage) {
        return new ExternalCompilationResult(errorMessage);
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
