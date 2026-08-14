package dk.dbc.dataio.jobstore.service.ejb;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ScheduledWatermarkPurgeBeanTest {
    private final WatermarkPurgeBean watermarkPurgeBean = mock(WatermarkPurgeBean.class);

    @Test
    public void run_watermarkPurgeBeanThrowsUncheckedException_noExceptionThrown() {
        final ScheduledWatermarkPurgeBean bean = createScheduledWatermarkPurgeBean();
        doThrow(new RuntimeException("DIED")).when(watermarkPurgeBean).purgeStaleWatermarks();
        bean.run();
    }

    private ScheduledWatermarkPurgeBean createScheduledWatermarkPurgeBean() {
        final ScheduledWatermarkPurgeBean bean = new ScheduledWatermarkPurgeBean();
        bean.watermarkPurgeBean = watermarkPurgeBean;
        return bean;
    }
}
