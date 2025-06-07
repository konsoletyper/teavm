/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.impl.currency;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.teavm.common.binary.BinaryParser;
import org.teavm.common.binary.Blob;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CurrenciesGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        byte[] bytes;
        try (InputStream input = context.getResourceProvider().getResource(
                "org/teavm/classlib/impl/currency/iso4217.bin").open()) {
            bytes = input.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error reading ISO 4217 metadata from file");
        }
        return parseBinaryData(bytes);
    }

    public static void main(String[] args) throws IOException {
        Document doc;
        try (var input = new FileInputStream(args[0])) {
            var builderFactory = DocumentBuilderFactory.newInstance();
            var builder = builderFactory.newDocumentBuilder();
            doc = builder.parse(new BufferedInputStream(input));
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Error reading ISO 4217 metadata from file");
        }

        var binaryData = convertToBinary(doc);

        try (var output = new FileOutputStream(args[1])) {
            output.write(binaryData);
        }
    }

    private static void extractCurrencyData(Element tableElem, List<CurrencyData> currencies) {
        for (var currencyElem : childElements(tableElem)) {
            if (!currencyElem.getTagName().equals("CcyNtry")) {
                continue;
            }

            var currency = new CurrencyData();
            boolean hasCode = false;

            for (Element propertyElem : childElements(currencyElem)) {
                switch (propertyElem.getTagName()) {
                    case "Ccy":
                        currency.code = getText(propertyElem);
                        hasCode = true;
                        break;
                    case "CcyNbr":
                        currency.numericCode = Integer.parseInt(getText(propertyElem));
                        break;
                    case "CcyMnrUnts":
                        var value = getText(propertyElem);
                        if (value.equals("N.A.")) {
                            currency.fractionDigits = -1;
                        } else {
                            currency.fractionDigits = Integer.parseInt(value);
                        }
                        break;
                }
            }

            if (hasCode) {
                currencies.add(currency);
            }
        }
    }

    private void parseCurrencies(Element tableElem, ResourceArrayBuilder<CurrencyResourceBuilder> currencies) {
        for (Element currencyElem : childElements(tableElem)) {
            if (!currencyElem.getTagName().equals("CcyNtry")) {
                continue;
            }
            var currency = new CurrencyResourceBuilder();
            for (Element propertyElem : childElements(currencyElem)) {
                switch (propertyElem.getTagName()) {
                    case "Ccy":
                        currency.code = getText(propertyElem);
                        break;
                    case "CcyNbr":
                        currency.numericCode = Integer.parseInt(getText(propertyElem));
                        break;
                    case "CcyMnrUnts":
                        String value = getText(propertyElem);
                        if (value.equals("N.A.")) {
                            currency.fractionDigits = -1;
                        } else {
                            currency.fractionDigits = Integer.parseInt(value);
                        }
                        break;
                }
            }
            currencies.values.add(currency);
        }
    }

    private static Iterable<Element> childElements(final Element parent) {
        return new Iterable<>() {
            NodeList nodes = parent.getChildNodes();
            @Override
            public Iterator<Element> iterator() {
                return new Iterator<>() {
                    int index = -1;
                    {
                        following();
                    }
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                    @Override
                    public Element next() {
                        Element result = (Element) nodes.item(index);
                        following();
                        return result;
                    }
                    @Override
                    public boolean hasNext() {
                        return index < nodes.getLength();
                    }
                    private void following() {
                        while (++index < nodes.getLength()) {
                            if (nodes.item(index).getNodeType() == Node.ELEMENT_NODE) {
                                break;
                            }
                        }
                    }
                };
            }
        };
    }

    private static String getText(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node child = nodes.item(i);
            switch (child.getNodeType()) {
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE:
                    CharacterData cdata = (CharacterData) child;
                    sb.append(cdata.getData());
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    EntityReference ref = (EntityReference) child;
                    sb.append(ref.getNodeValue());
                    break;
            }
        }
        return sb.toString();
    }

    private static byte[] convertToBinary(Document doc) {
        var output = new Blob();
        var currencies = new ArrayList<CurrencyData>();

        var root = doc.getDocumentElement();
        for (var elem : childElements(root)) {
            if (elem.getTagName().equals("CcyTbl")) {
                extractCurrencyData(elem, currencies);
            }
        }

        output.writeLEB(currencies.size());

        for (CurrencyData currency : currencies) {
            output.writeString(currency.code);
            output.writeLEB(currency.numericCode);
            output.writeLEB(currency.fractionDigits + 1);
        }

        return output.toArray();
    }

    private static ResourceBuilder parseBinaryData(byte[] data) {
        var currencies = new ResourceArrayBuilder<CurrencyResourceBuilder>();
        var input = new BinaryParser();
        input.data = data;

        int count = input.readLEB();
        for (int i = 0; i < count; i++) {
            var currency = new CurrencyResourceBuilder();
            currency.code = input.readString();
            currency.numericCode = input.readLEB();
            currency.fractionDigits = input.readLEB() - 1;
            currencies.values.add(currency);
        }

        return currencies;
    }

    private static class CurrencyData {
        String code;
        int numericCode;
        int fractionDigits;
    }
}
