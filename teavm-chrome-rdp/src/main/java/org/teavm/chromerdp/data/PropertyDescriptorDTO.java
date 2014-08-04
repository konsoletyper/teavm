package org.teavm.chromerdp.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyDescriptorDTO {
    private String name;
    private RemoteObjectDTO value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RemoteObjectDTO getValue() {
        return value;
    }

    public void setValue(RemoteObjectDTO value) {
        this.value = value;
    }
}
