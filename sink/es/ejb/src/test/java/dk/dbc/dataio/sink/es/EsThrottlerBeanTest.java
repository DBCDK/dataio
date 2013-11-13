package dk.dbc.dataio.sink.es;

import org.junit.Test;

/**
 * EsThrottlerBean unit tests.
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsThrottlerBeanTest {
    private static final int NEGATIVE_NUMBER = -42;

    @Test(expected = IllegalArgumentException.class)
    public void acquireRecordSlots_numOfSlotsArgIsNegative_throws() throws InterruptedException {
        getInitializedBean().acquireRecordSlots(NEGATIVE_NUMBER);
    }

    @Test
    public void acquireRecordSlots_requestedNumberOfRecordSlotsIsZero_returns() throws InterruptedException {
        getInitializedBean().acquireRecordSlots(0);
    }

    @Test
    public void acquireRecordSlots_requestedNumberOfRecordSlotsIsAvailable_returns() throws InterruptedException {
        getInitializedBean().acquireRecordSlots(EsThrottlerBean.RECORDS_CAPACITY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void releaseRecordSlots_numOfSlotsArgIsNegative_throws() throws InterruptedException {
        getInitializedBean().releaseRecordSlots(NEGATIVE_NUMBER);
    }

    @Test
    public void releaseRecordSlots_numOfSlotsArgIsZero_returns() throws InterruptedException {
        getInitializedBean().releaseRecordSlots(0);
    }

    private static EsThrottlerBean getInitializedBean() {
        final EsThrottlerBean esThrottler = new EsThrottlerBean();
        esThrottler.initialize();
        return esThrottler;
    }
}
