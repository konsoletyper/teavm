package org.teavm.chromerdp.messages;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetPropertiesCommand {
    private String objectId;
    private boolean ownProperties;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public boolean isOwnProperties() {
        return ownProperties;
    }

    public void setOwnProperties(boolean ownProperties) {
        this.ownProperties = ownProperties;
    }
}
