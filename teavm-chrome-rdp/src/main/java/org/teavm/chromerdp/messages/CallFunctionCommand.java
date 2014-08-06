package org.teavm.chromerdp.messages;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.teavm.chromerdp.data.CallArgumentDTO;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFunctionCommand {
    private String objectId;
    private String functionDeclaration;
    private CallArgumentDTO[] arguments;
    private boolean returnByValue;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getFunctionDeclaration() {
        return functionDeclaration;
    }

    public void setFunctionDeclaration(String functionDeclaration) {
        this.functionDeclaration = functionDeclaration;
    }

    public CallArgumentDTO[] getArguments() {
        return arguments;
    }

    public void setArguments(CallArgumentDTO[] arguments) {
        this.arguments = arguments;
    }

    public boolean isReturnByValue() {
        return returnByValue;
    }

    public void setReturnByValue(boolean returnByValue) {
        this.returnByValue = returnByValue;
    }
}
