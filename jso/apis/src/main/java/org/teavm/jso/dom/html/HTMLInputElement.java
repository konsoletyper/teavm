/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;
import org.teavm.jso.file.FileList;
import org.teavm.jso.popover.PopoverInvokerElement;

public abstract class HTMLInputElement extends HTMLElement implements PopoverInvokerElement {
    @JSProperty
    public abstract boolean isChecked();

    @JSProperty
    public abstract void setChecked(boolean checked);

    @JSProperty
    public abstract boolean isDisabled();

    @JSProperty
    public abstract void setDisabled(boolean disabled);

    @JSProperty
    public abstract int getMaxLength();

    @JSProperty
    public abstract void setMaxLength(int maxLength);

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty
    public abstract boolean isReadOnly();

    @JSProperty
    public abstract void setReadOnly(boolean readOnly);

    @JSProperty
    public abstract int getSize();

    @JSProperty
    public abstract void setSize(int size);

    @JSProperty
    public abstract String getType();

    @JSProperty
    public abstract void setType(String type);

    @JSProperty
    public abstract String getValue();

    @JSProperty
    public abstract void setValue(String value);
    
    public abstract void setCustomValidity(String validationFailure);
    
    public abstract boolean checkValidity();
    
    public abstract boolean reportValidity();

    public abstract void select();

    @JSProperty
    public abstract String getPlaceholder();

    @JSProperty
    public abstract void setPlaceholder(String value);

    @JSProperty
    public abstract FileList getFiles();
}
