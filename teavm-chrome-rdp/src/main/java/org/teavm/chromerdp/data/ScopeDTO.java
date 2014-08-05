package org.teavm.chromerdp.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScopeDTO {
    private RemoteObjectDTO object;
    private String type;

    public RemoteObjectDTO getObject() {
        return object;
    }

    public void setObject(RemoteObjectDTO object) {
        this.object = object;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
