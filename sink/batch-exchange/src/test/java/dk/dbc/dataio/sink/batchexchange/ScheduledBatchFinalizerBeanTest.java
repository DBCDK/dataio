package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledBatchFinalizerBeanTest {
    private BatchFinalizerBean batchFinalizerBean = mock(BatchFinalizerBean.class);

    @Test
    public void run_batchFinalizerBeanThrowsUncheckedException_noExceptionThrown() throws SinkException {
        when(batchFinalizerBean.finalizeNextCompletedBatch()).thenThrow(new RuntimeException());
        final ScheduledBatchFinalizerBean scheduledBatchFinalizerBean = createScheduledBatchFinalizerBean();
        scheduledBatchFinalizerBean.run();
    }

    @Test
    public void run_batchFinalizerBeanThrowsCheckedException_noExceptionThrown() throws SinkException {
        when(batchFinalizerBean.finalizeNextCompletedBatch()).thenThrow(new SinkException("DIED"));
        final ScheduledBatchFinalizerBean scheduledBatchFinalizerBean = createScheduledBatchFinalizerBean();
        scheduledBatchFinalizerBean.run();
    }

    private ScheduledBatchFinalizerBean createScheduledBatchFinalizerBean() {
        final ScheduledBatchFinalizerBean bean = new ScheduledBatchFinalizerBean();
        bean.batchFinalizerBean = batchFinalizerBean;
        return bean;
    }
}
