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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ResourceBundleTest {
    @Test
    public void getObjectLjava_lang_String() {
        // Regression test for Harmony-5698
        try {
            ResourceBundle bundle;
            String name = "tests.support.Support_TestResource";
            Locale.setDefault(new Locale("en", "US"));
            bundle = ResourceBundle.getBundle(name, new Locale("fr", "FR", "VAR"));
            bundle.getObject("not exist");
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            assertNotNull(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getBundle_getClassName() {
        // Regression test for Harmony-1759
        Locale locale = Locale.GERMAN;
        String nonExistentBundle = "Non-ExistentBundle";
        try {
            ResourceBundle.getBundle(nonExistentBundle, locale, this.getClass().getClassLoader());
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }
        
        try {
            ResourceBundle.getBundle(nonExistentBundle, locale);
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }

        locale = Locale.getDefault();
        try {
            ResourceBundle.getBundle(nonExistentBundle);
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }

    }

    @Test
    public void simpleBundle() {
        ResourceBundle bundle = ResourceBundle.getBundle("testBundle", Locale.ENGLISH);
        assertEquals("testBundle", bundle.getBaseBundleName());
        assertEquals("Test passed", bundle.getString("a"));
        try {
            bundle.getString("b");
            fail("MissingResourceException not thrown");
        } catch (MissingResourceException e) {
            assertEquals("b", e.getKey());
        }
    }

    /*
     * the class and constructor must be public so ResourceBundle has the
     * possibility to instantiate
     */
    public static class GetBundleTest {
        public GetBundleTest() {
            // Try to load a resource with the same name as the class.
            // getBundle() should not try to instantiate the class since
            // its not a ResourceBundle. If a .properties file exists it
            // would be loaded.
            ResourceBundle.getBundle("org.apache.harmony.luni.tests.java.util.ResourceBundleTest$GetBundleTest");
        }
    }

    @Test
    public void getBundleLjava_lang_String() {
        try {
            new GetBundleTest();
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
    }
}
