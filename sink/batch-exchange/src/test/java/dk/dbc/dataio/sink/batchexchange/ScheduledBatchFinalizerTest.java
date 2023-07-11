package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledBatchFinalizerTest {
    private BatchFinalizer batchFinalizer = mock(BatchFinalizer.class);

    @Test
    public void run_batchFinalizerBeanThrowsUncheckedException_noExceptionThrown() throws SinkException {
        when(batchFinalizer.finalizeNextCompletedBatch()).thenThrow(new RuntimeException());
        final ScheduledBatchFinalizerBean scheduledBatchFinalizerBean = createScheduledBatchFinalizerBean();
        scheduledBatchFinalizerBean.run();
    }

    @Test
    public void run_batchFinalizerBeanThrowsCheckedException_noExceptionThrown() throws SinkException {
        when(batchFinalizer.finalizeNextCompletedBatch()).thenThrow(new SinkException("DIED"));
        final ScheduledBatchFinalizerBean scheduledBatchFinalizerBean = createScheduledBatchFinalizerBean();
        scheduledBatchFinalizerBean.run();
    }

    private ScheduledBatchFinalizerBean createScheduledBatchFinalizerBean() {
        final ScheduledBatchFinalizerBean bean = new ScheduledBatchFinalizerBean();
        bean.batchFinalizerBean = batchFinalizer;
        return bean;
    }
}
