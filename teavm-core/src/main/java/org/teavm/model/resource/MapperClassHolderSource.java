package org.teavm.model.resource;

import org.teavm.codegen.ConcurrentCachedMapper;
import org.teavm.codegen.Mapper;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class MapperClassHolderSource implements ClassHolderSource {
    private Mapper<String, ClassHolder> mapper;

    public MapperClassHolderSource(Mapper<String, ClassHolder> mapper) {
        this.mapper = new ConcurrentCachedMapper<>(mapper);
    }

    @Override
    public ClassHolder getClassHolder(String name) {
        return mapper.map(name);
    }
}
