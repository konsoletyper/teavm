/*
 *  Copyright 2017 Jaroslav Tulach.
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
package org.teavm.html4j.test;

import net.java.html.js.tests.JavaScriptBodyTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 *
 * @author Jaroslav Tulach
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class JavaScriptTCKTest extends JavaScriptBodyTest {

    @Test @Override
    public void globalValueInCallbackAvailable() throws Exception {
        super.globalValueInCallbackAvailable();
    }

    @Test @Override
    public void globalStringAvailable() throws Exception {
        super.globalStringAvailable();
    }

    @Test @Override
    public void callLater() throws Exception {
        super.callLater();
    }

    @Test @Override
    public void doubleInAnArray() throws Exception {
        super.doubleInAnArray();
    }

    @Test @Override
    public void problematicString() {
        super.problematicString();
    }

    @Test @Override
    public void callbackUnknownArray() {
        super.callbackUnknownArray();
    }

    @Test @Override
    public void callbackUnknown() {
        super.callbackUnknown();
    }

    @Test @Override
    public void callbackKnown() {
        super.callbackKnown();
    }

    @Test @Override
    public void returnUnknownArray() {
        super.returnUnknownArray();
    }

    @Test @Override
    public void returnUndefinedString() {
        super.returnUndefinedString();
    }

    @Test @Override
    public void returnUnknown() {
        super.returnUnknown();
    }

    @Test @Override
    public void primitiveTypes() {
        super.primitiveTypes();
    }

    @Test @Override
    public void iterateArray() {
        super.iterateArray();
    }

    @Test @Override
    public void asyncCallFromAJSCallbackNeedToFinishBeforeReturnToJS() {
        super.asyncCallFromAJSCallbackNeedToFinishBeforeReturnToJS();
    }

    @Test @Override
    public void delayCallback() {
        super.delayCallback();
    }

    @Test @Override
    public void staticCallback() {
        super.staticCallback();
    }

    @Test @Override
    public void sumArray() {
        super.sumArray();
    }

    @Test @Override
    public void factorial6() {
        super.factorial6();
    }

    @Test @Override
    public void factorial5() {
        super.factorial5();
    }

    @Test @Override
    public void factorial4() {
        super.factorial4();
    }

    @Test @Override
    public void factorial3() {
        super.factorial3();
    }

    @Test @Override
    public void factorial2() {
        super.factorial2();
    }

    @Test @Override
    public void truth() {
        super.truth();
    }

    @Test @Override
    public void sumMatrix() {
        super.sumMatrix();
    }

    @Test @Override
    public void sumVector() {
        super.sumVector();
    }

    @Test @Override
    public void callbackWithArray() {
        super.callbackWithArray();
    }

    @Test @Override
    public void modifyJavaArrayHasNoEffect() {
        super.modifyJavaArrayHasNoEffect();
    }

    @Test @Override
    public void javaArrayInOutIsCopied() {
        super.javaArrayInOutIsCopied();
    }

    @Test @Override
    public void isJavaArray() {
        super.isJavaArray();
    }

    @Test @Override
    public void lengthOfJavaArray() {
        super.lengthOfJavaArray();
    }

    @Test @Override
    public void selectFromObjectJavaArray() {
        super.selectFromObjectJavaArray();
    }

    @Test @Override
    public void selectFromStringJavaArray() {
        super.selectFromStringJavaArray();
    }

    @Test @Override
    public void callbackWithParameters() throws InterruptedException {
        super.callbackWithParameters();
    }

    @Test @Override
    public void callbackWithFalseResult() {
        super.callbackWithFalseResult();
    }

    @Test @Override
    public void callbackWithTrueResult() {
        super.callbackWithTrueResult();
    }

    @Test @Override
    public void nullIsNull() {
        super.nullIsNull();
    }

    @Test @Override
    public void encodingBackslashString() {
        super.encodingBackslashString();
    }

    @Test @Override
    public void encodingString() {
        super.encodingString();
    }

    @Test @Override
    public void identity() {
        super.identity();
    }

    @Test @Override
    public void doubleCallbackToRunnable() {
        super.doubleCallbackToRunnable();
    }

    @Test @Override
    public void computeInARunnable() {
        super.computeInARunnable();
    }

    @Test @Override
    public void toStringOfAnEnum() {
        super.toStringOfAnEnum();
    }

    @Test @Override
    public void typeOfDoubleValueOf() {
        super.typeOfDoubleValueOf();
    }

    @Test @Override
    public void typeOfStringValueOf() {
        super.typeOfStringValueOf();
    }

    @Test @Override
    public void typeOfIntegerValueOf() {
        super.typeOfIntegerValueOf();
    }

    @Test @Override
    public void typeOfBooleanValueOf() {
        super.typeOfBooleanValueOf();
    }

    @Test @Override
    public void typeOfDouble() {
        super.typeOfDouble();
    }

    @Test @Override
    public void typeOfString() {
        super.typeOfString();
    }

    @Test @Override
    public void typeOfInteger() {
        super.typeOfInteger();
    }

    @Test @Override
    public void typeOfPrimitiveBoolean() {
        super.typeOfPrimitiveBoolean();
    }

    @Test @Override
    public void typeOfBoolean() {
        super.typeOfBoolean();
    }

    @Test @Override
    public void typeOfCharacter() {
        super.typeOfCharacter();
    }

    @Test @Override
    public void asyncCallbackFlushed() throws InterruptedException {
        super.asyncCallbackFlushed();
    }

    @Test @Override
    public void asyncCallbackToRunnable() throws InterruptedException {
        super.asyncCallbackToRunnable();
    }

    @Test @Override
    public void callbackToRunnable() {
        super.callbackToRunnable();
    }

    @Test @Override
    public void callWithNoReturnType() {
        super.callWithNoReturnType();
    }

    @Test @Override
    public void accessJsObject() {
        super.accessJsObject();
    }

    @Test @Override
    public void sumFromCallback() {
        super.sumFromCallback();
    }

    @Test @Override
    public void sumTwoNumbers() {
        super.sumTwoNumbers();
    }

}
