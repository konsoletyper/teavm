package org.teavm.model.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author konsoletyper
 */
public interface ResourceReader {
    boolean hasResource(String name);

    InputStream openResource(String name) throws IOException;
}
