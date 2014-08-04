package org.teavm.chromerdp.messages;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.teavm.chromerdp.data.PropertyDescriptorDTO;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetPropertiesResponse {
    private PropertyDescriptorDTO[] properties;

    public PropertyDescriptorDTO[] getProperties() {
        return properties;
    }

    public void setProperties(PropertyDescriptorDTO[] properties) {
        this.properties = properties;
    }
}
