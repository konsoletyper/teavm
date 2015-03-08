package org.teavm.maven;

import org.apache.maven.plugin.logging.Log;
import org.teavm.tooling.TeaVMToolLog;

/**
 *
 * @author Alexey Andreev
 */
public class MavenTeaVMToolLog implements TeaVMToolLog {
    private Log log;

    public MavenTeaVMToolLog(Log log) {
        this.log = log;
    }

    @Override
    public void info(String text) {
        log.info(text);
    }

    @Override
    public void debug(String text) {
        log.debug(text);
    }

    @Override
    public void warning(String text) {
        log.warn(text);
    }

    @Override
    public void error(String text) {
        log.error(text);
    }

    @Override
    public void info(String text, Throwable e) {
        log.info(text, e);
    }

    @Override
    public void debug(String text, Throwable e) {
        log.debug(text, e);
    }

    @Override
    public void warning(String text, Throwable e) {
        log.warn(text, e);
    }

    @Override
    public void error(String text, Throwable e) {
        log.error(text, e);
    }
}
