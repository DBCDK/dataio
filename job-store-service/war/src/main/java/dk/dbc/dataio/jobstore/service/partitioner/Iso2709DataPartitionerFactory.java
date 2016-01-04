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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Unpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Iso2709DataPartitionerFactory implements DataPartitionerFactory {

    /**
     * Creates new instance of default Iso2709 DataPartitioner
     *
     * @param inputStream stream from which Iso2709 data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of default Iso2709 DataPartitioner
     *
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    @Override
    public DataPartitioner createDataPartitioner(InputStream inputStream, String specifiedEncoding)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "encoding");
        return new Iso2709DataPartitioner(inputStream, specifiedEncoding);
    }

    private static class Iso2709DataPartitioner implements DataPartitioner {
        private static final Logger LOGGER = LoggerFactory.getLogger(Iso2709DataPartitioner.class);

        private final ByteCountingInputStream inputStream;
        private Charset encoding;
        private String specifiedEncoding;

        private Iterator<ChunkItem> iterator;
        private DanMarc2Charset danMarc2Charset;
        private BufferedInputStream bufferedInputStream;
        private DocumentBuilderFactory documentBuilderFactory;


        public Iso2709DataPartitioner(InputStream inputStream, String specifiedEncoding) {
            this.inputStream = new ByteCountingInputStream(inputStream);
            this.encoding = StandardCharsets.UTF_8;
            this.specifiedEncoding = specifiedEncoding;
        }

        @Override
        public Charset getEncoding() throws InvalidEncodingException {
            return encoding;
        }

        @Override
        public long getBytesRead() {
            return inputStream.getBytesRead();
        }

        @Override
        public Iterator<ChunkItem> iterator() throws UnrecoverableDataException {
            if (iterator == null) {
                validateSpecifiedEncoding();
                danMarc2Charset = new DanMarc2Charset();
                bufferedInputStream = new BufferedInputStream(inputStream);
                documentBuilderFactory = DocumentBuilderFactory.newInstance();

            }
            iterator = new Iterator<ChunkItem>() {
                private Document document = null;

                @Override
                public boolean hasNext() {
                    try {
                        document = Iso2709Unpacker.createMarcXChangeRecord(bufferedInputStream, danMarc2Charset, documentBuilderFactory);
                    } catch (IOException | ParserConfigurationException e) {
                        LOGGER.error("Exception caught while creating MarcXChange Record", e);
                        throw new InvalidDataException(e);
                    }
                    return document != null;
                }

                @Override
                public ChunkItem next() {
                    if(document != null) {
                        try {
                            return new ChunkItem(0, domToString(document).getBytes(StandardCharsets.UTF_8), ChunkItem.Status.SUCCESS );
                        } catch (TransformerException e) {
                            LOGGER.error("Unrecoverable error occurred during transformation", e);
                            throw new InvalidDataException(e);
                        }
                    }
                    return null;
                }
            };
            return iterator;
        }

        /*
         * Private methods
         */

        /**
         * This method verifies if the specified encoding is latin1
         */
        private void validateSpecifiedEncoding()  {
            if(!EncodingsUtil.isEquivalent(specifiedEncoding, "latin1")) {
                throw new InvalidEncodingException(String.format(
                        "Specified encoding not supported: '%s' ", specifiedEncoding));
            }
        }

        /**
         *
         * This method converts a document to a String
         * @param document the document to convert
         * @return String representation of the document
         *
         * @throws TransformerException if it was not possible to create a Transformer instance or
         *         if an unrecoverable error occurs during the course of the transformation.
         */
        private String domToString(Document document) throws TransformerException {
            DOMSource domSource = new DOMSource(document);
            StreamResult result = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(domSource, result);
            return result.getWriter().toString();
        }
    }
}
