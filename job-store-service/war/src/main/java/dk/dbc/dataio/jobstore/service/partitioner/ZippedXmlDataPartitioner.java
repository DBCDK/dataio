package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        // ZipInputStream may throw and IOException which we cast as
        // an UnrecoverableDataException to be caught by the caller
        try {
            // Process all entries
            // Todo: Most likely, we must add a root element
            while((zipEntry = zipStream.getNextEntry()) != null) {
                LOGGER.info("Zip entry " + zipEntry.getName() + " with uncompressed size " + zipEntry.getSize());

                // Read all data from this entry.
                byte[] buffer = new byte[(int) zipEntry.getSize()];
                //while(zipStream.available() > 0)
                zipStream.read(buffer, 0, (int) zipEntry.getSize());
                uncompressedOutputStream.write(buffer, 0, buffer.length);
                zipStream.closeEntry();
            }

            // Add the uncompressed output to a new stream that becomes the input stream
            // for the super (DefaultXmlDataPartitioner) when we construct the partitioner
            ByteArrayInputStream uncompressedInputStream = new ByteArrayInputStream(uncompressedOutputStream.toByteArray());

            // Give new stream as input to the DefaultXmlDataPartitioner
            return new ZippedXmlDataPartitioner(uncompressedInputStream, encoding);
        }
        catch( IOException ioe )
        {
            LOGGER.error("Caught IOException when uncompressing zipped input data: " + ioe.getMessage());
            throw new UnrecoverableDataException(ioe);
        }
    }

    protected ZippedXmlDataPartitioner(InputStream inputStream, String encoding) {
        super(inputStream, encoding);
    }
}