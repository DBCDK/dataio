package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;

import java.nio.charset.Charset;
import java.util.Iterator;

public interface DataPartitioner extends Iterable<DataPartitionerResult> {
    long NO_BYTE_COUNT_AVAILABLE = -128;

    Charset getEncoding() throws InvalidEncodingException;

    long getBytesRead();

    @SuppressWarnings("PMD.EmptyCatchBlock")
    default void drainItems(int itemsToRemove) {
        if (itemsToRemove < 0) throw new IllegalArgumentException("Unable to drain a negative number of items");
        final Iterator<DataPartitionerResult> iterator = this.iterator();
        while (--itemsToRemove >=0) {
            try {
                iterator.next();
            } catch (PrematureEndOfDataException e) {
                throw e;    // to potentially trigger a retry
            } catch (Exception e) {
                // we simply swallow these as they have already been handled in chunk items
            }
        }
    }

    default int getAndResetSkippedCount() {
        return 0;
    }
}
