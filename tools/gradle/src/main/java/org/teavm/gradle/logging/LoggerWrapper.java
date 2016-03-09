/*
 *  Copyright 2016 MJ.
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

package org.teavm.gradle.logging;

import org.gradle.api.logging.Logger;
import org.teavm.tooling.TeaVMToolLog;

public class LoggerWrapper implements TeaVMToolLog {
    private final Logger logger;

    public LoggerWrapper(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(final String text) {
        logger.info(text);
    }

    @Override
    public void debug(final String text) {
        logger.debug(text);
    }

    @Override
    public void warning(final String text) {
        logger.warn(text);
    }

    @Override
    public void error(final String text) {
        logger.error(text);
    }

    @Override
    public void info(final String text, final Throwable e) {
        logger.info(text, e);
    }

    @Override
    public void debug(final String text, final Throwable e) {
        logger.debug(text, e);
    }

    @Override
    public void warning(final String text, final Throwable e) {
        logger.warn(text, e);
    }

    @Override
    public void error(final String text, final Throwable e) {
        logger.error(text, e);
    }
}
