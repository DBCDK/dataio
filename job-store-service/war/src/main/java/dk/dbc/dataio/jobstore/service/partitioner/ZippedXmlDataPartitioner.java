package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Todo: Fix start import
import java.io.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZippedXmlDataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZippedXmlDataPartitioner.class);

    private final ByteCountingInputStream inputStream;
    private final String encodingExpected;
    private ZipInputStream zipStream;
    private Iterator<DataPartitionerResult> iterator;
    private Iterator<DataPartitionerResult> currentZipIterator = null;

    public static ZippedXmlDataPartitioner newInstance(InputStream inputStream, String encoding)
            throws NullPointerException, IllegalArgumentException, UnrecoverableDataException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");

        return new ZippedXmlDataPartitioner(inputStream, encoding);
    }

    protected ZippedXmlDataPartitioner(InputStream inputStream, String encoding) {
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.encodingExpected = encoding;
        this.zipStream = new ZipInputStream(inputStream);
    }

    // Todo: Add constructors for fetching parts of the inputstream

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return StandardCharsets.UTF_8;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() throws UnrecoverableDataException, PrematureEndOfDataException {

        if (iterator == null) {

            // We need to read ahead since there is no other way of known if we are at the end of the zipfile.
            getNextEntryWithData();

            // Create iterator
            iterator = new Iterator<DataPartitionerResult>() {

                /**
                 * @inheritDoc
                 */
                @Override
                public boolean hasNext() throws UnrecoverableDataException, PrematureEndOfDataException {
                    return currentZipIterator != null;
                }

                /**
                 * @inheritDoc
                 */
                @Override
                public DataPartitionerResult next() throws UnrecoverableDataException {

                    // Get next chunk
                    DataPartitionerResult result = currentZipIterator.next();

                    // If no more chunks available, try to advance the iterator
                    if( !currentZipIterator.hasNext() ) {
                        getNextEntryWithData();
                    }

                    return result;
                }

                /**
                 * @inheritDoc This method does not do anything.
                 */
                @Override
                public void remove() {
                }
            };

        }
        return iterator;
    }

    private DefaultXmlDataPartitioner getXmlDataPartitioner(ZipInputStream zipStream) throws IOException {

        // Read all data from this entry. Due to the compressed format, we can not read the entire
        // unzipped file in one go, hence the need for the loop.
        byte[] buffer = new byte[(int) 2048];
        ByteArrayOutputStream chunks = new ByteArrayOutputStream();
        int len;
        while( (len = zipStream.read(buffer)) > 0) {
            chunks.write(buffer, 0, len);
        }

        // Get inputstream with the uncompressed chunk
        ByteArrayInputStream uncompressedChunks = new ByteArrayInputStream(chunks.toByteArray());
        return new DefaultXmlDataPartitioner(uncompressedChunks, encodingExpected);
    }

    private void getNextEntryWithData() {

        try {
            do {
                // Fetch next entry, check if we have reached eof
                ZipEntry entry = zipStream.getNextEntry();
                if(entry == null) {
                    currentZipIterator = null;
                    return;
                }

                // Partition chunks contained in the new entry, get the iterator
                DefaultXmlDataPartitioner partitioner = getXmlDataPartitioner(zipStream);
                currentZipIterator = partitioner.iterator();
            } while(!currentZipIterator.hasNext());
        } catch(IOException ioe) {
            throw new UnrecoverableDataException(ioe);
        }
    }
}