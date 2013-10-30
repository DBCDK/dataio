package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * PingResponse unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class PingResponseTest {
    private static final PingResponse.Status STATUS = PingResponse.Status.OK;
    private static final List<String> LOG = Arrays.asList("message");

    @Test(expected = NullPointerException.class)
    public void constructor_statusArgIsNull_throws() {
        new PingResponse(null, LOG);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_logArgIsNull_throws() {
        new PingResponse(STATUS, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final PingResponse instance = new PingResponse(STATUS, LOG);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_logArgIsEmpty_returnsNewInstance() {
        final PingResponse instance = new PingResponse(STATUS, new ArrayList<String>(0));
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfLogList() {
        final List<String> log = new ArrayList<>();
        log.add("msg1");
        final PingResponse instance = new PingResponse(STATUS, log);
        assertThat(instance.getLog().size(), is(1));
        log.add("msg2");
        final List<String> returnedLog = instance.getLog();
        assertThat(returnedLog.size(), is(1));
        returnedLog.add("msg3");
        assertThat(instance.getLog().size(), is(1));
    }

    public static PingResponse newPingResponse() {
        return new PingResponse(STATUS, LOG);
    }
}
