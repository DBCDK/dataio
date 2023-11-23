package dk.dbc.dataio.commons.utils.lang;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class HashcodeTest {
    @Test
    public void algorithm() {
        assertThat("deterministic", Hashcode.of("abcdef"), is(Hashcode.of("abcdef")));
        assertThat("common prefix", Hashcode.of("abcdef:870970"), is(not(Hashcode.of("abcdef:870971"))));
    }
}
