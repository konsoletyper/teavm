package org.teavm.chromerdp.messages;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.teavm.chromerdp.data.RemoteObjectDTO;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFunctionResponse {
    private RemoteObjectDTO result;
    private boolean wasThrown;

    public RemoteObjectDTO getResult() {
        return result;
    }

    public void setResult(RemoteObjectDTO result) {
        this.result = result;
    }

    public boolean isWasThrown() {
        return wasThrown;
    }

    public void setWasThrown(boolean wasThrown) {
        this.wasThrown = wasThrown;
    }
}
