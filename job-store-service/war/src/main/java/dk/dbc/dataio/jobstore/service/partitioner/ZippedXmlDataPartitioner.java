package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    private int positionInDatafile = 0;

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
                    positionInDatafile++;

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
        ByteArrayOutputStream compressedItems = new ByteArrayOutputStream();
        int len;
        while( (len = zipStream.read(buffer)) > 0) {
            compressedItems.write(buffer, 0, len);
        }

        // Get inputstream with the uncompressed chunk
        ByteArrayInputStream items = new ByteArrayInputStream(compressedItems.toByteArray());
        return new DefaultXmlDataPartitioner(items, encodingExpected, positionInDatafile);
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
            LOGGER.info("getNextEntryWithData() failed due to exception: " + ioe.getMessage());
            throw new UnrecoverableDataException(ioe);
        }
    }
}