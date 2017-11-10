/*
 *  Copyright 2017 Alexey Andreev.
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

package org.teavm.classlib.java.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import org.teavm.classlib.impl.ResourceBundleImpl;

public abstract class TResourceBundle {
    protected TResourceBundle parent;

    private Locale locale;

    static class MissingBundle extends TResourceBundle {
        @Override
        public Enumeration<String> getKeys() {
            return null;
        }

        @Override
        public Object handleGetObject(String name) {
            return null;
        }
    }

    private static final TResourceBundle MISSING = new MissingBundle();

    private static final TResourceBundle MISSINGBASE = new MissingBundle();

    private static final Map<String, TResourceBundle> cache = new HashMap<>();

    private static final Map<String, Supplier<ResourceBundle>> bundleProviders =
            ResourceBundleImpl.createBundleMap(false);

    public TResourceBundle() {
    }

    public static TResourceBundle getBundle(String bundleName)
            throws MissingResourceException {
        return getBundleImpl(bundleName, Locale.getDefault(), ClassLoader.getSystemClassLoader());
    }

    public static TResourceBundle getBundle(String bundleName, Locale locale) {
        return getBundleImpl(bundleName, locale, ClassLoader.getSystemClassLoader());
    }

    public static TResourceBundle getBundle(String bundleName, Locale locale, ClassLoader loader)
            throws MissingResourceException {
        if (loader == null) {
            throw new NullPointerException();
        }
        if (bundleName != null) {
            TResourceBundle bundle;
            if (!locale.equals(Locale.getDefault())) {
                bundle = handleGetBundle(bundleName, "_" + locale, false);
                if (bundle != null) {
                    return bundle;
                }
            }
            bundle = handleGetBundle(bundleName, "_" + Locale.getDefault(), true);
            if (bundle != null) {
                return bundle;
            }
            throw new MissingResourceException("Bundle not found", bundleName + "_" + locale, "");
        }
        throw new NullPointerException();
    }

    private static TResourceBundle getBundleImpl(String bundleName, Locale locale, ClassLoader loader)
            throws MissingResourceException {
        if (bundleName != null) {
            TResourceBundle bundle;
            if (!locale.equals(Locale.getDefault())) {
                String localeName = locale.toString();
                if (localeName.length() > 0) {
                    localeName = "_" + localeName;
                }
                bundle = handleGetBundle(bundleName, localeName, false);
                if (bundle != null) {
                    return bundle;
                }
            }
            String localeName = Locale.getDefault().toString();
            if (localeName.length() > 0) {
                localeName = "_" + localeName;
            }
            bundle = handleGetBundle(bundleName, localeName, true);
            if (bundle != null) {
                return bundle;
            }
            throw new MissingResourceException("Bundle not found", bundleName + '_' + locale, "");
        }
        throw new NullPointerException();
    }

    public abstract Enumeration<String> getKeys();

    public Locale getLocale() {
        return locale;
    }

    public final Object getObject(String key) {
        TResourceBundle last;
        TResourceBundle theParent = this;
        do {
            Object result = theParent.handleGetObject(key);
            if (result != null) {
                return result;
            }
            last = theParent;
            theParent = theParent.parent;
        } while (theParent != null);
        throw new MissingResourceException("", last.getClass().getName(), key);
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    private static TResourceBundle handleGetBundle(String base, String locale, boolean loadBase) {
        TResourceBundle bundle;
        String bundleName = base + locale;
        TResourceBundle result = cache.get(bundleName);
        if (result != null) {
            if (result == MISSINGBASE) {
                return null;
            }
            if (result == MISSING) {
                if (!loadBase) {
                    return null;
                }
                String extension = strip(locale);
                if (extension == null) {
                    return null;
                }
                return handleGetBundle(base, extension, loadBase);
            }
            return result;
        }

        Supplier<ResourceBundle> provider = bundleProviders.get(bundleName);
        bundle = provider != null ? (TResourceBundle) (Object) provider.get() : null;

        String extension = strip(locale);
        if (bundle != null) {
            if (extension != null) {
                TResourceBundle parent = handleGetBundle(base, extension, true);
                if (parent != null) {
                    bundle.setParent(parent);
                }
            }
            cache.put(bundleName, bundle);
            return bundle;
        }

        if (extension != null && (loadBase || extension.length() > 0)) {
            bundle = handleGetBundle(base, extension, loadBase);
            if (bundle != null) {
                cache.put(bundleName, bundle);
                return bundle;
            }
        }
        cache.put(bundleName, loadBase ? MISSINGBASE : MISSING);
        return null;
    }

    protected abstract Object handleGetObject(String key);

    protected void setParent(TResourceBundle bundle) {
        parent = bundle;
    }

    private static String strip(String name) {
        int index = name.lastIndexOf('_');
        if (index != -1) {
            return name.substring(0, index);
        }
        return null;
    }

    private void setLocale(String name) {
        String language = "";
        String country = "";
        String variant = "";
        if (name.length() > 1) {
            int nextIndex = name.indexOf('_', 1);
            if (nextIndex == -1) {
                nextIndex = name.length();
            }
            language = name.substring(1, nextIndex);
            if (nextIndex + 1 < name.length()) {
                int index = nextIndex;
                nextIndex = name.indexOf('_', nextIndex + 1);
                if (nextIndex == -1) {
                    nextIndex = name.length();
                }
                country = name.substring(index + 1, nextIndex);
                if (nextIndex + 1 < name.length()) {
                    variant = name.substring(nextIndex + 1, name.length());
                }
            }
        }
        locale = new Locale(language, country, variant);
    }
}
