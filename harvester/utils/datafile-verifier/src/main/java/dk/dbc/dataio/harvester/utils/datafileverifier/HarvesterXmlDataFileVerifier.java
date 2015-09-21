/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Class used for verification of harvester data files in XML format
 */
public class HarvesterXmlDataFileVerifier {
    private final DomUtil domUtil;

    public HarvesterXmlDataFileVerifier() throws ParserConfigurationException {
        domUtil = new DomUtil();
    }

    /**
     * Verifies content of given XML file against given list of
     * expectations throwing assertion error unless all expectations can be met
     * @param dataFile harvester data file
     * @param expectations list of DataFileExpectation
     * @throws IOException if unable to read harvester data file
     * @throws SAXException if unable to parse harvester data file as XML
     */
    public void verify(File dataFile, List<DataFileExpectation> expectations) throws IOException, SAXException {
        final Document document = domUtil.asDocument(dataFile);
        final NodeList childNodes = document.getDocumentElement().getChildNodes();
        assertThat(childNodes, is(notNullValue()));
        assertThat(childNodes.getLength(), is(expectations.size()));
        verifyDataFileMembers(childNodes, expectations);
    }

    /* Verifies all harvester data file members
     */
    private void verifyDataFileMembers(NodeList memberNodes, List<DataFileExpectation> expectations) {
        for (int i = 0; i < expectations.size(); i++) {
            expectations.get(i).verify(memberNodes.item(i));
        }
    }
}
