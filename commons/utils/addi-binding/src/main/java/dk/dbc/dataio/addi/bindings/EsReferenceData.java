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

package dk.dbc.dataio.addi.bindings;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "referencedata", namespace = EsReferenceData.ES_NAMESPACE)
public class EsReferenceData {
    public static final String ES_NAMESPACE = "http://oss.dbc.dk/ns/es";
    public static final String DATAIO_DIRECTIVES_NAMESPACE = "dk.dbc.dataio.processing";
    private static final String ES_REFERENCE_DATA_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">%s</es:referencedata>";

    @JacksonXmlProperty(localName = "info", namespace = ES_NAMESPACE)
    public EsDirectives esDirectives;

    @JacksonXmlProperty(localName = "sink-processing", namespace = DATAIO_DIRECTIVES_NAMESPACE)
    public SinkDirectives sinkDirectives;

    @JacksonXmlProperty(localName = "sink-update-template", namespace = DATAIO_DIRECTIVES_NAMESPACE)
    public UpdateSinkDirectives updateSinkDirectives;

    public EsReferenceData withEsDirectives(EsDirectives esDirectives) {
        this.esDirectives = esDirectives;
        return this;
    }

    public EsReferenceData withSinkDirectives(SinkDirectives sinkDirectives) {
        this.sinkDirectives = sinkDirectives;
        return this;
    }

    public EsReferenceData withUpdateSinkDirectives(UpdateSinkDirectives updateSinkDirectives) {
        this.updateSinkDirectives = updateSinkDirectives;
        return this;
    }

    /**
     * @return XML string without any dataIO specific directives
     */
    public String toXmlString() {
        if (esDirectives != null) {
            return String.format(ES_REFERENCE_DATA_XML_TEMPLATE, esDirectives.toXmlString("es"));
        }
        return String.format(ES_REFERENCE_DATA_XML_TEMPLATE, "");
    }
}
