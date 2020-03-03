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

import net.java.html.json.tests.ConvertTypesTest;
import net.java.html.json.tests.JSONTest;
import net.java.html.json.tests.KnockoutTest;
import net.java.html.json.tests.MinesTest;
import net.java.html.json.tests.OperationsTest;
import net.java.html.json.tests.WebSocketTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

/**
 *
 * @author Jaroslav Tulach
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@WholeClassCompilation
public class KnockoutTCKTest {
    private final ConvertTypesTest convertTypesTest = new ConvertTypesTest();
    private final JSONTest jsonTest = new JSONTest();
    private final KnockoutTest knockoutTest = new KnockoutTest();
    private final MinesTest minesTest = new MinesTest();
    private final OperationsTest operationsTest = new OperationsTest();
    private final WebSocketTest webSocketTest = new WebSocketTest();

    @Test
    public void connectUsingWebSocket() throws Throwable {
        webSocketTest.connectUsingWebSocket();
    }

    @Test
    public void errorUsingWebSocket() throws Throwable {
        webSocketTest.errorUsingWebSocket();
    }

    @Test
    public void haveToOpenTheWebSocket() throws Throwable {
        webSocketTest.haveToOpenTheWebSocket();
    }

    @Test
    public void syncOperation() {
        operationsTest.syncOperation();
    }

    @Test
    public void asyncOperation() throws InterruptedException {
        operationsTest.asyncOperation();
    }

    @Test
    public void paintTheGridOnClick() throws Throwable {
        withInterrupt(minesTest::paintTheGridOnClick);
    }

    @Test
    public void countAround() throws Exception {
        minesTest.countAround();
    }

    @Test
    public void modifyValueAssertChangeInModelOnEnum() throws Throwable {
        knockoutTest.modifyValueAssertChangeInModelOnEnum();
    }

    @Test
    public void modifyRadioValueOnEnum() throws Throwable {
        knockoutTest.modifyRadioValueOnEnum();
    }

    @Test
    public void modifyValueAssertChangeInModelOnDouble() throws Throwable {
        knockoutTest.modifyValueAssertChangeInModelOnDouble();
    }

    @Test
    public void rawObject() throws Exception {
        knockoutTest.rawObject();
    }

    @Test
    public void modifyComputedProperty() throws Throwable {
        knockoutTest.modifyComputedProperty();
    }

    @Test
    public void modifyValueAssertChangeInModelOnBoolean() throws Throwable {
        knockoutTest.modifyValueAssertChangeInModelOnBoolean();
    }

    @Test
    public void modifyValueAssertChangeInModel() throws Exception {
        knockoutTest.modifyValueAssertChangeInModel();
    }

    @Test
    public void selectWorksOnModels() throws Exception {
        knockoutTest.selectWorksOnModels();
    }

    @Test
    public void nestedObjectEqualsChange() throws Exception {
        knockoutTest.nestedObjectEqualsChange();
    }

    @Test
    public void nestedObjectChange() throws Exception {
        knockoutTest.nestedObjectChange();
    }

    @Test
    public void modifyValueAssertAsyncChangeInModel() throws Throwable {
        withInterrupt(knockoutTest::modifyValueAssertAsyncChangeInModel);
    }

    @Test
    public void nonMutableDouble() throws Exception {
        knockoutTest.nonMutableDouble();
    }

    @Test
    public void nonMutableString() throws Exception {
        knockoutTest.nonMutableString();
    }

    @Test
    public void nonMutableBoolean() throws Exception {
        knockoutTest.nonMutableBoolean();
    }

    @Test
    public void nonMutableIntArray() throws Exception {
        knockoutTest.nonMutableIntArray();
    }

    @Test
    public void displayContentOfArray() throws Exception {
        knockoutTest.displayContentOfArray();
    }

    @Test
    public void displayContentOfAsyncArray() throws Throwable {
        withInterrupt(knockoutTest::displayContentOfAsyncArray);
    }

    @Test
    public void displayContentOfComputedArray() throws Exception {
        knockoutTest.displayContentOfComputedArray();
    }

    @Test
    public void displayContentOfComputedArrayOnASubpair() throws Exception {
        knockoutTest.displayContentOfComputedArrayOnASubpair();
    }

    @Test
    public void displayContentOfComputedArrayOnComputedASubpair() throws Exception {
        knockoutTest.displayContentOfComputedArrayOnComputedASubpair();
    }

    @Test
    public void checkBoxToBooleanBinding() throws Exception {
        knockoutTest.checkBoxToBooleanBinding();
    }

    @Test
    public void displayContentOfDerivedArray() throws Exception {
        knockoutTest.displayContentOfDerivedArray();
    }

    @Test
    public void displayContentOfArrayOfPeople() throws Exception {
        knockoutTest.displayContentOfArrayOfPeople();
    }

    @Test
    public void accessFirstPersonWithOnFunction() throws Exception {
        knockoutTest.accessFirstPersonWithOnFunction();
    }

    @Test
    public void onPersonFunction() throws Exception {
        knockoutTest.onPersonFunction();
    }

    @Test
    public void stringArrayModificationVisible() throws Exception {
        knockoutTest.stringArrayModificationVisible();
    }

    @Test
    public void intArrayModificationVisible() throws Exception {
        knockoutTest.intArrayModificationVisible();
    }

    @Test
    public void derivedIntArrayModificationVisible() throws Exception {
        knockoutTest.derivedIntArrayModificationVisible();
    }

    @Test
    public void archetypeArrayModificationVisible() throws Exception {
        knockoutTest.archetypeArrayModificationVisible();
    }

    @Test
    public void fromJsonWithUTF8() throws Throwable {
        jsonTest.fromJsonWithUTF8();
    }
    @Test
    public void fromJsonEmptyValues() throws Throwable {
        jsonTest.fromJsonEmptyValues();
    }
    
    @Test
    public void toJSONInABrowser() throws Throwable {
        jsonTest.toJSONInABrowser();
    }

    @Test
    public void toJSONWithEscapeCharactersInABrowser() throws Throwable {
        jsonTest.toJSONWithEscapeCharactersInABrowser();
    }

    @Test
    public void toJSONWithDoubleSlashInABrowser() throws Throwable {
        jsonTest.toJSONWithDoubleSlashInABrowser();
    }

    @Test
    public void toJSONWithApostrophInABrowser() throws Throwable {
        jsonTest.toJSONWithApostrophInABrowser();
    }

    @Test
    public void loadAndParseJSON() throws InterruptedException {
        jsonTest.loadAndParseJSON();
    }

    @Test
    public void loadAndParsePlainText() throws Exception {
        jsonTest.loadAndParsePlainText();
    }

    @Test
    public void loadAndParsePlainTextOnArray() throws Exception {
        jsonTest.loadAndParsePlainTextOnArray();
    }

    @Test
    public void loadAndParseJSONP() throws InterruptedException, Exception {
        jsonTest.loadAndParseJSONP();
    }

    @Test
    public void putPeopleUsesRightMethod() throws InterruptedException, Exception {
        jsonTest.putPeopleUsesRightMethod();
    }

    @Test
    public void loadAndParseJSONSentToArray() throws InterruptedException {
        jsonTest.loadAndParseJSONSentToArray();
    }

    @Test
    public void loadAndParseJSONArraySingle() throws InterruptedException {
        jsonTest.loadAndParseJSONArraySingle();
    }

    @Test
    public void loadAndParseArrayInPeople() throws InterruptedException {
        jsonTest.loadAndParseArrayInPeople();
    }

    @Test
    public void loadAndParseArrayInPeopleWithHeaders() throws InterruptedException {
        jsonTest.loadAndParseArrayInPeopleWithHeaders();
    }

    @Test
    public void loadAndParseArrayOfIntegers() throws InterruptedException {
        jsonTest.loadAndParseArrayOfIntegers();
    }

    @Test
    public void loadAndParseArrayOfEnums() throws InterruptedException {
        jsonTest.loadAndParseArrayOfEnums();
    }

    @Test
    public void loadAndParseJSONArray() throws InterruptedException {
        jsonTest.loadAndParseJSONArray();
    }

    @Test
    public void loadError() throws InterruptedException {
        jsonTest.loadError();
    }

    @Test
    public void parseNullNumber() throws Exception {
        jsonTest.parseNullNumber();
    }

    @Test
    public void deserializeWrongEnum() throws Exception {
        jsonTest.deserializeWrongEnum();
    }

    @Test
    public void testConvertToPeople() throws Exception {
        convertTypesTest.testConvertToPeople();
    }

    @Test
    public void parseConvertToPeople() throws Exception {
        convertTypesTest.parseConvertToPeople();
    }

    @Test
    public void parseConvertToPeopleWithAddress() throws Exception {
        convertTypesTest.parseConvertToPeopleWithAddress();
    }

    @Test
    public void parseConvertToPeopleWithAddressIntoAnArray() throws Exception {
        convertTypesTest.parseConvertToPeopleWithAddressIntoAnArray();
    }

    @Test
    public void parseNullValue() throws Exception {
        convertTypesTest.parseNullValue();
    }

    @Test
    public void parseNullArrayValue() throws Exception {
        convertTypesTest.parseNullArrayValue();
    }

    @Test
    public void testConvertToPeopleWithoutSex() throws Exception {
        convertTypesTest.testConvertToPeopleWithoutSex();
    }

    @Test
    public void parseConvertToPeopleWithoutSex() throws Exception {
        convertTypesTest.parseConvertToPeopleWithoutSex();
    }

    @Test
    public void parseConvertToPeopleWithAddressOnArray() throws Exception {
        convertTypesTest.parseConvertToPeopleWithAddressOnArray();
    }

    @Test
    public void parseConvertToPeopleWithoutSexOnArray() throws Exception {
        convertTypesTest.parseConvertToPeopleWithoutSexOnArray();
    }

    @Test
    public void parseFirstElementFromAbiggerArray() throws Exception {
        convertTypesTest.parseFirstElementFromAbiggerArray();
    }

    @Test
    public void parseAllElementFromAbiggerArray() throws Exception {
        convertTypesTest.parseAllElementFromAbiggerArray();
    }

    @Test
    public void parseFiveElementsAsAnArray() throws Exception {
        convertTypesTest.parseFiveElementsAsAnArray();
    }

    @Test
    public void parseInnerElementAsAnArray() throws Exception {
        convertTypesTest.parseInnerElementAsAnArray();
    }


    @Test
    public void parseOnEmptyArray() throws Exception {
        convertTypesTest.parseOnEmptyArray();
    }

    private void withInterrupt(R r) throws Throwable {
        for (int i = 0; i < 10; i++) {
            try {
                r.run();
                break;
            } catch (InterruptedException ex) {
                Thread.sleep(100);
                continue;
            }
        }
    }

    interface R {
        void run() throws Throwable;
    }
}
