package org.teavm.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class MinifyingAliasProvider implements AliasProvider {
    private static String startLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private int lastSuffix;

    @Override
    public String getAlias(FieldReference field) {
        return getNewAlias();
    }

    @Override
    public String getAlias(MethodReference method) {
        return getNewAlias();
    }

    @Override
    public String getAlias(String className) {
        return getNewAlias();
    }

    private String getNewAlias() {
        int index = lastSuffix++;
        StringBuilder sb = new StringBuilder();
        sb.append(startLetters.charAt(index % startLetters.length()));
        index /= startLetters.length();
        while (index > 0) {
            sb.append(letters.charAt(index % letters.length()));
            index /= letters.length();
        }
        return sb.toString();
    }
}
