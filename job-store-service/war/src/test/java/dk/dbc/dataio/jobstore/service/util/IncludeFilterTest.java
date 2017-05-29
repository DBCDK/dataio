package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import java.util.BitSet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class IncludeFilterTest {

    @Test
    public void include() {
        BitSet bitSet = new BitSet();
        bitSet.set(2);
        IncludeFilter includeFilter = new IncludeFilter(bitSet);
        assertThat("first key is false", includeFilter.include(0), is(false));
        assertThat("third key is true", includeFilter.include(2), is(true));
    }

    @Test
    public void include_always() {
        IncludeFilter includeFilter = new IncludeFilterAlways();
        assertThat("any key is true", includeFilter.include(29), is(true));
    }
}
