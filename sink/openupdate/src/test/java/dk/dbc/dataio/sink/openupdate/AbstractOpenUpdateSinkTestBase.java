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
package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import org.xmlunit.matchers.CompareMatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class AbstractOpenUpdateSinkTestBase {
    public static final String ES_INFO_SUBMITTER_ATTRIBUTE_VALUE = "870970";
    public static final String UPDATE_TEMPLATE_ATTRIBUTE_VALUE = "bog";

    private static final String META_XML =
        "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
            "<es:info format=\"basis\" language=\"dan\" submitter=\"" + ES_INFO_SUBMITTER_ATTRIBUTE_VALUE + "\"/>" +
            "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" updateTemplate=\"" + UPDATE_TEMPLATE_ATTRIBUTE_VALUE + "\"/>" +
        "</es:referencedata>";

    protected static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    protected String getMetaXml() {
        return  META_XML;
    }

    protected String getInvalidMetaXml(String referenceDataChildren) {
        return  "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                    referenceDataChildren +
                "</es:referencedata>";
    }

    protected String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    protected AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected byte[] getAddi(String metaXml, String contentXml) {
        return new AddiRecordWrapper(metaXml, contentXml).getAddiRecordAsString().getBytes();
    }
    protected byte[] getAddi(List<AddiRecordWrapper> addiRecords) {

        return addiRecords.stream().map(AddiRecordWrapper::getAddiRecordAsString).collect(Collectors.joining(System.lineSeparator())).getBytes();
    }

    protected class AddiRecordWrapper {
        private String metaXml;
        private String contentXml;

        public AddiRecordWrapper(String metaXml, String contentXml) {
            this.metaXml = metaXml;
            this.contentXml = contentXml;
        }

        public String getMetaXml() {return this.metaXml;}
        public String getContentXml() {return this.contentXml;}

        private byte[] getMetaXmlAsBytes() {return this.metaXml.trim().getBytes();}
        private byte[] getContentXmlAsBytes() {return this.contentXml.trim().getBytes();}

        public String getAddiRecordAsString() {
            return this.getMetaXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getMetaXml()
                    + System.lineSeparator()
                    + this.getContentXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getContentXml();
        }
    }
}
