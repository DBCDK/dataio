package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.invariant.InvariantUtil;

import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Iterator;

/**
 * Specialized {@link DataPartitioner} implementation wrapping an existing partitioner and only returning results
 * indicated by the bits set in the accompanying {@link BitSet}
 * <p>
 * Be advised that this type of {@link DataPartitioner} is liable to return null as the last value of an iteration
 * since the include filter may very likely status it to run out af values from the wrapped partitioner before a final
 * return value can be produced.
 * </p>
 */
public class IncludeFilterDataPartitioner implements DataPartitioner {
    private final DataPartitioner wrappedDataPartitioner;
    private final BitSet includeFilter;
    private int skippedCount = 0;

    public static IncludeFilterDataPartitioner newInstance(DataPartitioner wrappedDataPartitioner, BitSet includeFilter) {
        return new IncludeFilterDataPartitioner(wrappedDataPartitioner, includeFilter);
    }

    IncludeFilterDataPartitioner(DataPartitioner wrappedDataPartitioner, BitSet includeFilter)
            throws NullPointerException {
        this.wrappedDataPartitioner = InvariantUtil.checkNotNullOrThrow(wrappedDataPartitioner, "wrappedDataPartitioner");
        this.includeFilter = InvariantUtil.checkNotNullOrThrow(includeFilter, "includeFilter");
    }

    public DataPartitioner getWrappedDataPartitioner() {
        return wrappedDataPartitioner;
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return wrappedDataPartitioner.getEncoding();
    }

    @Override
    public long getBytesRead() {
        return wrappedDataPartitioner.getBytesRead();
    }

    @Override
    public void drainItems(int itemsToRemove) {
        wrappedDataPartitioner.drainItems(itemsToRemove);
    }

    @Override
    public int getAndResetSkippedCount() {
        final int valueBeforeReset = skippedCount;
        skippedCount = 0;
        return valueBeforeReset;
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() {
        return new Iterator<DataPartitionerResult>() {
            final Iterator<DataPartitionerResult> wrappedIterator = wrappedDataPartitioner.iterator();

            @Override
            public boolean hasNext() {
                return wrappedIterator.hasNext();
            }

            @Override
            public DataPartitionerResult next() {
                DataPartitionerResult next = wrappedIterator.next();
                while (next != null) {
                    if (!next.isEmpty()) {
                        if (includeFilter.get(next.getPositionInDatafile())) {
                            return next;
                        } else {
                            skippedCount++;
                        }
                    }
                    if (!wrappedIterator.hasNext()) {
                        return null;
                    }
                    next = wrappedIterator.next();
                }
                return null;
            }
        };
    }
}
