package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.common.utils.io.BoundedInputStream;
import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class TarredXmlDataPartitioner implements DataPartitioner {
    private final String encodingExpected;
    private final ByteCountingInputStream inputStream;
    private TarArchiveInputStream tarStream;
    private Iterator<DataPartitionerResult> iterator;
    private Iterator<DataPartitionerResult> currentTarEntryIterator = null;
    private int positionInDatafile = 0;

    public static TarredXmlDataPartitioner newInstance(InputStream inputStream, String encoding)
            throws NullPointerException, IllegalArgumentException, UnrecoverableDataException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");

        return new TarredXmlDataPartitioner(inputStream, encoding);
    }

    protected TarredXmlDataPartitioner(InputStream inputStream, String encoding) {
        this.encodingExpected = encoding;
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.tarStream = new TarArchiveInputStream(this.inputStream);
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
            getNextArchiveEntry();

            iterator = new Iterator<>() {
                @Override
                public boolean hasNext() throws UnrecoverableDataException, PrematureEndOfDataException {
                    return currentTarEntryIterator != null;
                }

                @Override
                public DataPartitionerResult next() throws UnrecoverableDataException {
                    final DataPartitionerResult result = currentTarEntryIterator.next();
                    positionInDatafile++;

                    if (!currentTarEntryIterator.hasNext()) {
                        // Try to advance to the next archive entry
                        getNextArchiveEntry();
                    }

                    return result;
                }
            };
        }
        return iterator;
    }

    private void getNextArchiveEntry() {
        try {
            TarArchiveEntry tarEntry = null;
            while (tarEntry == null || !currentTarEntryIterator.hasNext()) {
                tarEntry = tarStream.getNextTarEntry();
                if (tarEntry == null) {
                    currentTarEntryIterator = null;
                    return;
                }
                if (!tarEntry.isFile()) {
                    tarEntry = null;
                    continue;
                }

                final BoundedInputStream boundedInputStream = new BoundedInputStream(tarStream, tarEntry.getSize());
                currentTarEntryIterator = new DefaultXmlDataPartitioner(
                        boundedInputStream, encodingExpected, positionInDatafile)
                        .iterator();
            }
        } catch (IOException e) {
            throw new UnrecoverableDataException(e);
        }
    }
}
