package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Iso2709ReorderingDataPartitionerTest {
    private static final JobItemReorderer JOB_ITEM_REORDERER = mock(JobItemReorderer.class);

    private final Iso2709ReorderingDataPartitioner partitioner =
            Iso2709ReorderingDataPartitioner.newInstance(
                    StringUtil.asInputStream(""), "LATIN-1", JOB_ITEM_REORDERER);

    @Test
    public void hasNextDataPartitionerResult_jobItemReordererIsPartOfIteration() {
        when(JOB_ITEM_REORDERER.hasNext())
                .thenReturn(true)
                .thenReturn(false);
        assertThat(partitioner.hasNextDataPartitionerResult(), is(true));
        assertThat(partitioner.hasNextDataPartitionerResult(), is(false));
    }

    @Test
    public void nextDataPartitionerResult_reordererThrows_throws() {
        when(JOB_ITEM_REORDERER.next(any(DataPartitionerResult.class)))
                .thenThrow(new RuntimeException());
        assertThat(partitioner::nextDataPartitionerResult, isThrowing(InvalidDataException.class));
    }
}
