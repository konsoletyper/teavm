package org.teavm.chromerdp.messages;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.teavm.chromerdp.data.PropertyDescriptorDTO;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetPropertiesResponse {
    private PropertyDescriptorDTO[] result;

    public PropertyDescriptorDTO[] getResult() {
        return result;
    }

    public void setResult(PropertyDescriptorDTO[] properties) {
        this.result = properties;
    }
}
