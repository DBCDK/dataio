package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.commons.types.Pid;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PidFilterTest {

    @Test
    public void constructor_inputIsnull_ok() {
        assertThat(() -> new PidFilter(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void test_returns() {
        PidFilter pidFilter = new PidFilter(Collections.singleton(870970));
        assertThat(pidFilter.test(Pid.of("870971-basis:23142546")), is(false));
        assertThat(pidFilter.test(Pid.of("unit:1354373")), is(false));
        assertThat(pidFilter.test(Pid.of("870970-basis:23142546")), is(true));
    }
}
