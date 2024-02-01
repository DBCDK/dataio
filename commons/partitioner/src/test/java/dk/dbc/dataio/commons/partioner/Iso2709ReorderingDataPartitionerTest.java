package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertThrows(InvalidDataException.class, partitioner::nextDataPartitionerResult);
    }
}
