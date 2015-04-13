package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncodingsUtilTest {
    @Test
    public void isEquivalent_firstArgIsNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent(null, "utf8"), is(false));
    }

    @Test
    public void isEquivalent_secondArgIsNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent("utf8", null), is(false));
    }

    @Test
    public void isEquivalent_bothArgsAreNull_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent(null, null), is(false));
    }

    @Test
    public void isEquivalent_argsAreEquivalent_returnsTrue() {
        assertThat(EncodingsUtil.isEquivalent("utf-8", "UTF8"), is(true));
    }

    @Test
    public void isEquivalent_argsAreNotEquivalent_returnsFalse() {
        assertThat(EncodingsUtil.isEquivalent("utf8", "latin1"), is(false));
    }
}