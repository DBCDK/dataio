package dk.dbc.dataio.sink.es;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * EsThrottlerBean unit tests.
 * The test methods of this class uses the following naming convention:
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsThrottlerBeanTest {
    private static final int RECORDS_CAPACITY = 42;

    @Test
    public void acquireRecordSlots_requestedNumberOfRecordSlotsIsZero_returns() {
        getInitializedBean().acquireRecordSlots(0);
    }

    @Test
    public void acquireRecordSlots_requestedNumberOfRecordSlotsIsAvailable_returns() {
        getInitializedBean().acquireRecordSlots(RECORDS_CAPACITY);
    }

    @Test
    public void releaseRecordSlots_numOfSlotsArgIsZero_returns() {
        getInitializedBean().releaseRecordSlots(0);
    }

    @Test
    public void acquireFollowedByRelease() {
        final EsThrottlerBean throttler = getInitializedBean();
        assertThat(throttler.acquireRecordSlots(RECORDS_CAPACITY*2), is(false));
        assertThat(throttler.acquireRecordSlots(2), is(true));
        assertThat(throttler.getAvailableSlots(), is(RECORDS_CAPACITY - 2));
        throttler.releaseRecordSlots(2);
        assertThat(throttler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    private static EsThrottlerBean getInitializedBean() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esRecordsCapacity = RECORDS_CAPACITY;
        final EsThrottlerBean esThrottler = new EsThrottlerBean();
        esThrottler.configuration = configuration;
        esThrottler.initialize();
        return esThrottler;
    }
}
