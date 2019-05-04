package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

public class ZippedXmlDataPartitioner extends DefaultXmlDataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZippedXmlDataPartitioner.class);

    public static ZippedXmlDataPartitioner newInstance(InputStream inputStream, String encoding)
            throws NullPointerException, IllegalArgumentException, UnrecoverableDataException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");

        // Setup input- and outputstreams for unzipping
        ZipInputStream zipStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        ByteArrayOutputStream uncompressedOutputStream = new ByteArrayOutputStream();

        // Read all entries and combine into one doc.
        // The sequential read of the zipfile ensures that the individual chunks will always
        // end up the same place in the combined document
        try {

            // Combined document
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder outputDocumentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document outputDocument = outputDocumentBuilder.newDocument();
            Element root = null;

            // Process all entries
            while((zipEntry = zipStream.getNextEntry()) != null) {
                LOGGER.info("Reading zipped file " + zipEntry.getName() + " with uncompressed size " + zipEntry.getSize());

                // Read all data from this entry. Due to the compressed format, we can not read the entire
                // unzipped file in one go, hence the need for the loop.
                byte[] buffer = new byte[(int) 2048];
                ByteArrayOutputStream chunk = new ByteArrayOutputStream();
                int len;
                while( (len = zipStream.read(buffer)) > 0) {
                    chunk.write(buffer, 0, len);
                }
                zipStream.closeEntry();

                // Load file into DOM document
                ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunk.toByteArray());
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document originalDocument = documentBuilder.parse(chunkStream);

                // If we have no root element yet, create it from the input root.
                // We can safely assume that the root element is the same for all documents in the
                // zipfile, and besides, the root element is dropped by the xml partitioner
                if( root == null ) {
                    root = (Element) outputDocument.importNode(originalDocument.getFirstChild(), false);
                    outputDocument.appendChild(root);
                }

                // Import all childnodes under the root into the output document
                NodeList innerXml = originalDocument.getDocumentElement().getChildNodes();
                for( int nid = 0; nid < innerXml.getLength(); nid++ ) {
                    Node node = innerXml.item(nid);
                    Node importedNode = outputDocument.importNode(node, true);
                    root.appendChild(importedNode);
                }
            }

            // Write completed document to the output stream
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(outputDocument);
            StreamResult streamResult = new StreamResult(uncompressedOutputStream);
            transformer.transform(domSource, streamResult);

            // Convert to input stream, ready for the xml parser
            ByteArrayInputStream uncompressedInputStream = new ByteArrayInputStream(uncompressedOutputStream.toByteArray());

            // Create a new DefaultXMLDataPartitioner (parent class)
            return new ZippedXmlDataPartitioner(uncompressedInputStream, encoding);
        }
        catch( IOException ioe ) {
            LOGGER.error("Caught IOException when uncompressing zipped input data: " + ioe.getMessage());
            throw new UnrecoverableDataException(ioe);
        }
        catch( javax.xml.transform.TransformerException te) {
            LOGGER.error("Caught TransformerException when writing combined xml document: " + te.getMessage());
            throw new UnrecoverableDataException(te);
        }
        catch( javax.xml.parsers.ParserConfigurationException pce ) {
            LOGGER.error("Caught ParserConfigurationException when parsing one file from the zip stream: " + pce.getMessage());
            throw new UnrecoverableDataException(pce);
        }
        catch(org.xml.sax.SAXException se) {
            LOGGER.error("SAXException: " + se.getMessage());
            throw new UnrecoverableDataException(se);
        }
    }

    protected ZippedXmlDataPartitioner(InputStream inputStream, String encoding) {
        super(inputStream, encoding);
    }
}