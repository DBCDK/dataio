/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.marc.Iso2709Packer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class ConversionISO2709 implements Conversion {
    private final DocumentBuilder documentBuilder;
    private final Charset encoding;

    ConversionISO2709(DocumentBuilder documentBuilder, Charset encoding) {
        this.documentBuilder = documentBuilder;
        this.encoding = encoding;
    }

    @Override
    public byte[] apply(byte[] bytes) {
        documentBuilder.reset();
        try {
            final Document document = documentBuilder.parse(new ByteArrayInputStream(bytes));
            return Iso2709Packer.create2709FromMarcXChangeRecord(document, encoding);
        } catch (SAXException | IOException e) {
            throw new ConversionException("Unable to parse XML", e);
        }
    }
}
