package org.teavm.javascript;

import java.util.HashMap;
import java.util.Map;
import org.teavm.javascript.ni.JSObject;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;

/**
 *
 * @author Alexey Andreev
 */
class NativeJavascriptClassRepository {
    private ClassHolderSource classSource;
    private Map<String, Boolean> knownJavaScriptClasses = new HashMap<>();

    public NativeJavascriptClassRepository(ClassHolderSource classSource) {
        this.classSource = classSource;
        knownJavaScriptClasses.put(JSObject.class.getName(), true);
    }

    public boolean isJavaScriptClass(String className) {
        Boolean known = knownJavaScriptClasses.get(className);
        if (known == null) {
            known = figureOutIfJavaScriptClass(className);
            knownJavaScriptClasses.put(className, known);
        }
        return known;
    }

    private boolean figureOutIfJavaScriptClass(String className) {
        ClassHolder cls = classSource.getClassHolder(className);
        if (cls == null || !cls.getModifiers().contains(ElementModifier.INTERFACE)) {
            return false;
        }
        for (String iface : cls.getInterfaces()) {
            if (isJavaScriptClass(iface)) {
                return true;
            }
        }
        return false;
    }
}
