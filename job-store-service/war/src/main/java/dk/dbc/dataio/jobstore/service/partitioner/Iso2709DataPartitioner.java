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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Iterator;
import dk.dbc.marc.Iso2709IteratorReadError;
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
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Iso2709DataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Iso2709DataPartitioner.class);

    private final Iso2709Iterator inputStream;
    private Charset encoding;
    private String specifiedEncoding;

    private Iterator<ChunkItem> iterator;
    private DanMarc2Charset danMarc2Charset;
    private BufferedInputStream bufferedInputStream;
    private DocumentBuilderFactory documentBuilderFactory;

    /**
     * Creates new instance of default Iso2709 DataPartitioner
     * @param inputStream stream from which Iso2709 data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of default Iso2709 DataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    public static Iso2709DataPartitioner newInstance(InputStream inputStream, String specifiedEncoding) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        return new Iso2709DataPartitioner(inputStream, specifiedEncoding);
    }

    protected Iso2709DataPartitioner(InputStream inputStream, String specifiedEncoding) {
        this.bufferedInputStream = getInputStreamAsBufferedInputStream(inputStream);
        this.inputStream = new Iso2709Iterator(bufferedInputStream);
        this.encoding = StandardCharsets.UTF_8;
        this.specifiedEncoding = specifiedEncoding;
        validateSpecifiedEncoding();
        danMarc2Charset = new DanMarc2Charset();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return encoding;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getTotalBytesRead();
    }

    @Override
    public Iterator<ChunkItem> iterator() throws UnrecoverableDataException {
        iterator = new Iterator<ChunkItem>() {

            @Override
            public boolean hasNext() {
                return inputStream.hasNext();
            }

            @Override
            public ChunkItem next() {
                byte[] recordAsBytes = inputStream.next();

                try {
                    Document document = Iso2709Unpacker.createMarcXChangeRecord(recordAsBytes, danMarc2Charset, documentBuilderFactory);
                    if (document == null) {
                        return null;
                    }
                    return new ChunkItem(0, domToString(document).getBytes(StandardCharsets.UTF_8), ChunkItem.Status.SUCCESS );
                } catch (ParserConfigurationException e) {
                    LOGGER.error("Exception caught while creating MarcXChange Record", e);
                    throw new InvalidDataException(e);
                } catch (TransformerException e) {
                    LOGGER.error("Unrecoverable error occurred during transformation", e);
                    throw new InvalidDataException(e);
                } catch( Iso2709IteratorReadError e) {
                    return null;
                } catch (Exception e) {
                    LOGGER.error("Exception caught while decoding 2709", e);
                    ChunkItem result = ObjectFactory.buildFailedChunkItem(0, recordAsBytes);
                    result.appendDiagnostics(new Diagnostic(Diagnostic.Level.FATAL,"Exception caught while decoding 2709", e ));
                    return result;
                }
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


    private static BufferedInputStream getInputStreamAsBufferedInputStream(InputStream inputStream) {
        if( inputStream instanceof BufferedInputStream ) return (BufferedInputStream) inputStream;
        return new BufferedInputStream(inputStream);
    }
}
