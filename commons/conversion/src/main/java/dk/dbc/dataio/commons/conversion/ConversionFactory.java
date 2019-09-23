/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.conversion;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ConversionFactory {
    private final DocumentBuilderFactory documentBuilderFactory;

    public ConversionFactory() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }

    public Conversion newConversion(ConversionParam param) {
        try {
            return new ConversionISO2709(param, documentBuilderFactory.newDocumentBuilder());
        } catch (ParserConfigurationException e) {
            throw new ConversionException("Unable to create XML document builder", e);
        }
    }
}
