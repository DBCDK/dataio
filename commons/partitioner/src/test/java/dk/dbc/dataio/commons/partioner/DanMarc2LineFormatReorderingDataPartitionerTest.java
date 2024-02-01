package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DanMarc2LineFormatReorderingDataPartitionerTest {
    private static final JobItemReorderer JOB_ITEM_REORDERER = mock(JobItemReorderer.class);

    private final DanMarc2LineFormatReorderingDataPartitioner partitioner =
            DanMarc2LineFormatReorderingDataPartitioner.newInstance(
                    StringUtil.asInputStream(""), "latin1", JOB_ITEM_REORDERER);

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
        assertThrows(InvalidDataException.class, partitioner::nextDataPartitionerResult);
    }
}
