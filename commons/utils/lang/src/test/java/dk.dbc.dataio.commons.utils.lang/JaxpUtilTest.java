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

package dk.dbc.dataio.commons.utils.lang;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JaxpUtil unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JaxpUtilTest {
    private final String xmlRecord = "<data/>";
    private final String xmlRecordWithNamespace = "<ns:data xmlns:ns=\"http://test.dbc.dk/ns\"/>";

    @Test(expected=NullPointerException.class)
    public void parseDocumentTakingInputSourceParameter_xmlArgIsNull_throwsNullPointerException() throws Exception {
        JaxpUtil.parseDocument((InputSource) null);
    }

    @Test(expected=SAXException.class)
    public void parseDocumentTakingInputSourceParameter_xmlArgIsInvalidXml_throwsSAXException() throws Exception {
        JaxpUtil.parseDocument(stringToInputSource("not xml"));
    }

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsValidXml_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(stringToInputSource(xmlRecord));
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsValidXmlWithNs_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(stringToInputSource(xmlRecordWithNamespace));
        assertThat(doc, is(notNullValue()));
    }

    @Test(expected=NullPointerException.class)
    public void parseDocumentTakingStringParameter_xmlArgIsNull_throwsNullPointerException() throws Exception {
        String xml = null;
        JaxpUtil.parseDocument(xml);
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseDocumentTakingStringParameter_xmlArgIsEmpty_throwsIllegalArgumentException() throws Exception {
        JaxpUtil.parseDocument("");
    }

    @Test(expected=SAXException.class)
    public void parseDocumentTakingStringParameter_xmlArgIsInvalidXml_throwsSAXException() throws Exception {
        JaxpUtil.parseDocument("not xml");
    }

    @Test
    public void parseDocumentTakingStringParameter_xmlArgIsValidXml_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecord);
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void parseDocumentTakingInputStringParameter_xmlArgIsValidXmlWithNs_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecordWithNamespace);
        assertThat(doc, is(notNullValue()));
    }

    @Test(expected=NullPointerException.class)
    public void asString_nodeArgIsNull_throwsNullPointerException() throws Exception {
        JaxpUtil.asString(null);
    }

    @Test
    public void asString_nodeArgIsValid_returnsStringRepesentation() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecord);
        String docAsString = JaxpUtil.asString(doc.getDocumentElement());
        assertThat(docAsString, is(xmlRecord));
    }

    @Test
    public void asString_nodeArgIsValidWithNamespace_returnsStringRepesentation() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecordWithNamespace);
        String docAsString = JaxpUtil.asString(doc.getDocumentElement());
        assertThat(docAsString, is(xmlRecordWithNamespace));
    }

    private InputSource stringToInputSource(String str) {
        return new InputSource(new StringReader(str));
    }
}
