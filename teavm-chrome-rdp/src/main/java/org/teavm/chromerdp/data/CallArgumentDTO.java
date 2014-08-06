package org.teavm.chromerdp.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallArgumentDTO {
    private String objectId;
    private JsonNode value;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
    }
}
