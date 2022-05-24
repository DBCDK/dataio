package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class IncludeFilterDataPartitionerTest {
    private SimpleDataPartitioner wrappedPartitioner;

    @Before
    public void setWrappedPartitioner() {
        wrappedPartitioner = new SimpleDataPartitioner();
    }

    @Test
    public void returnsEncodingOfWrappedDataPartitioner() {
        final IncludeFilterDataPartitioner partitioner = new IncludeFilterDataPartitioner(wrappedPartitioner, new BitSet());
        assertThat(partitioner.getEncoding(), is(wrappedPartitioner.getEncoding()));
    }

    @Test
    public void returnsBytesReadOfWrappedDataPartitioner() {
        final IncludeFilterDataPartitioner partitioner = new IncludeFilterDataPartitioner(wrappedPartitioner, new BitSet());
        assertThat(partitioner.getBytesRead(), is(wrappedPartitioner.getBytesRead()));
    }

    @Test
    public void iterates() {
        final BitSet includeFilter = new BitSet();
        includeFilter.set(3);
        includeFilter.set(4);
        final IncludeFilterDataPartitioner partitioner = new IncludeFilterDataPartitioner(wrappedPartitioner, includeFilter);

        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("has 4th result", iterator.hasNext(), is(true));
        assertThat("4th result position", iterator.next().getPositionInDatafile(), is(3));
        assertThat("has 5th result", iterator.hasNext(), is(true));
        assertThat("5th result position", iterator.next().getPositionInDatafile(), is(4));
        assertThat("has more results", iterator.hasNext(), is(true));
        assertThat("ran out of results", iterator.next(), is(nullValue()));
    }

    @Test
    public void skippedCount() {
        final BitSet includeFilter = new BitSet();
        includeFilter.set(2);
        includeFilter.set(4);
        includeFilter.set(6);

        final IncludeFilterDataPartitioner partitioner = new IncludeFilterDataPartitioner(wrappedPartitioner, includeFilter);
        for (DataPartitionerResult result : partitioner) {}
        assertThat("skipped count before reset", partitioner.getAndResetSkippedCount(), is(7));
        assertThat("skipped count after reset", partitioner.getAndResetSkippedCount(), is(0));
    }

    @Test
    public void drainsItems() {
        final BitSet includeFilter = new BitSet();
        includeFilter.set(0, 10);

        final IncludeFilterDataPartitioner partitioner = new IncludeFilterDataPartitioner(wrappedPartitioner, includeFilter);
        partitioner.drainItems(9 + 2); // +2 to account for the fake -1 empty results

        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("has 10th result", iterator.hasNext(), is(true));
        assertThat("10th result position", iterator.next().getPositionInDatafile(), is(9));
        assertThat("has no more results", iterator.hasNext(), is(false));
    }

    public static class SimpleDataPartitioner implements DataPartitioner {
        final List<Integer> data = new ArrayList<>(Arrays.asList(0, 1, 2, 3, -1, 4, 5, 6, -1, 7, 8, 9));
        final Iterator<Integer> dataIterator = data.iterator();

        @Override
        public Charset getEncoding() throws InvalidEncodingException {
            return StandardCharsets.UTF_8;
        }

        @Override
        public long getBytesRead() {
            return 42;
        }

        @Override
        public Iterator<DataPartitionerResult> iterator() {
            return new Iterator<DataPartitionerResult>() {
                @Override
                public boolean hasNext() {
                    return dataIterator.hasNext();
                }

                @Override
                public DataPartitionerResult next() {
                    final Integer next = dataIterator.next();
                    if (next != null) {
                        dataIterator.remove();
                        if (next == -1) { // Simulate empty result as they might happen from a ReorderingDataPartitioner
                            return DataPartitionerResult.EMPTY;
                        } else {
                            return new DataPartitionerResult(null, new RecordInfo(next.toString()), next);
                        }
                    }
                    return null;
                }
            };
        }
    }
}
