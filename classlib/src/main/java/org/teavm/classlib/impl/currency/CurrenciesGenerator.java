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
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class CurrenciesGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        Document doc;
        try (InputStream input = context.getClassLoader().getResourceAsStream(
                "org/teavm/classlib/impl/currency/iso4217.xml")) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.parse(new BufferedInputStream(input));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Error reading ISO 4217 medata from file");
        }

        ResourceArray<CurrencyResource> currencies = context.createResourceArray();
        Element root = doc.getDocumentElement();
        for (Element elem : childElements(root)) {
            if (elem.getTagName().equals("CcyTbl")) {
                parseCurrencies(context, elem, currencies);
            }
        }
        return currencies;
    }

    private void parseCurrencies(MetadataGeneratorContext context, Element tableElem,
            ResourceArray<CurrencyResource> currencies) {
        for (Element currencyElem : childElements(tableElem)) {
            if (!currencyElem.getTagName().equals("CcyNtry")) {
                continue;
            }
            CurrencyResource currency = context.createResource(CurrencyResource.class);
            for (Element propertyElem : childElements(currencyElem)) {
                switch (propertyElem.getTagName()) {
                    case "Ccy":
                        currency.setCode(getText(propertyElem));
                        break;
                    case "CcyNbr":
                        currency.setNumericCode(Integer.parseInt(getText(propertyElem)));
                        break;
                    case "CcyMnrUnts":
                        String value = getText(propertyElem);
                        if (value.equals("N.A.")) {
                            currency.setFractionDigits(-1);
                        } else {
                            currency.setFractionDigits(Integer.parseInt(value));
                        }
                        break;
                }
            }
            currencies.add(currency);
        }
    }

    private Iterable<Element> childElements(final Element parent) {
        return new Iterable<Element>() {
            NodeList nodes = parent.getChildNodes();
            @Override
            public Iterator<Element> iterator() {
                return new Iterator<Element>() {
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

    private String getText(Element element) {
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
}
